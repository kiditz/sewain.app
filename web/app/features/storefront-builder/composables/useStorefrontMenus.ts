import { useStorefrontState as useState } from "./useStorefrontState";
import type { StorefrontMenu } from "~/types/storefront";
import { defaultMenus } from "../utils/storefront-menu";
import { useStorefrontResource, useStorefrontResourceFactory } from "./useStorefrontResource";

export function useStorefrontMenus() {
  const menuItems = useState<StorefrontMenu[]>("storefront-menu-items", defaultMenus);
  const headerMenuId = useState<string | null>("storefront-header-menu", () => "main-menu");
  const initialized = useState("storefront-menus-initialized", () => false);
  const loading = useState("storefront-menus-loading", () => false);
  const saving = useState("storefront-menus-saving", () => false);
  const saveError = useState("storefront-menus-save-error", () => false);
  const hasUnsavedChanges = useState("storefront-menus-dirty", () => false);
  const saved = useState<Record<string, string>>("storefront-menus-saved", () => ({}));
  const savedNavigation = useState<string | undefined>("storefront-navigation-saved", () => undefined);
  const api = useApi();
  const createResource = useStorefrontResourceFactory("menus");
  const navigation = useStorefrontResource("navigation", "navigation", "/storefront/navigation");
  const menuResource = (id: string) => createResource(id, `/storefront/menus/${encodeURIComponent(id)}`);
  async function load() {
    if (initialized.value) return;
    loading.value = true;
    try {
      const [menus, nav] = await Promise.all([
        api<{ configuration: StorefrontMenu[] }>("/storefront/menus"),
        api<{ configuration: { headerMenuId?: string | null } }>("/storefront/navigation"),
      ]);
      menuItems.value = nav.configuration.headerMenuId === undefined && !menus.configuration.length ? defaultMenus() : menus.configuration;
      saved.value = Object.fromEntries(menus.configuration.map(menu => [menu.id, JSON.stringify(menu)]));
      headerMenuId.value = nav.configuration.headerMenuId === undefined ? "main-menu" : nav.configuration.headerMenuId;
      savedNavigation.value = nav.configuration.headerMenuId === undefined ? undefined : JSON.stringify({ headerMenuId: headerMenuId.value });
      initialized.value = true;
    } finally { loading.value = false; }
  }
  async function saveMenus() {
    if (saving.value) return;
    saving.value = true;
    hasUnsavedChanges.value = true;
    saveError.value = false;
    const menus = JSON.parse(JSON.stringify(menuItems.value)) as StorefrontMenu[];
    const nav = { headerMenuId: headerMenuId.value };
    try {
      // Save referenced menus first, navigation next, then remove old menus.
      for (const menu of menus) {
        const value = JSON.stringify(menu);
        if (saved.value[menu.id] === value) continue;
        await menuResource(menu.id).save(menu);
        saved.value[menu.id] = value;
      }
      const navigationValue = JSON.stringify(nav);
      if (savedNavigation.value !== navigationValue) {
        await navigation.save(nav);
        savedNavigation.value = navigationValue;
      }
      for (const id of Object.keys(saved.value)) {
        if (menus.some(menu => menu.id === id)) continue;
        await menuResource(id).remove();
        delete saved.value[id];
      }
      hasUnsavedChanges.value = false;
    } catch (error) {
      saveError.value = true;
      throw error;
    } finally { saving.value = false; }
  }
  return {
    MenuItems: menuItems, headerMenuId, initialized, loading, saving, saveError, hasUnsavedChanges, navigation,
    headerMenu: computed(() => menuItems.value.find(menu => menu.id === headerMenuId.value)),
    load, saveMenus,
    initializeDefaults: async () => { if (savedNavigation.value === undefined) await saveMenus(); },
  };
}
