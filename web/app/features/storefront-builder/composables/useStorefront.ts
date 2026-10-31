import { ensureFooterSection } from "../utils/footer-section";
import { normalizeCheckoutSection } from "../utils/checkout-layout";
import { normalizeCartSection } from "../utils/cart-layout";
import { ensureNavigationSection } from "../utils/navigation-section";
import { useStorefrontPages } from "./useStorefrontPages";
import { useStorefrontTheme } from "./useStorefrontTheme";
import { useStorefrontMenus } from "./useStorefrontMenus";
import { useStorefrontCatalog } from "./useStorefrontCatalog";
import { initialTheme } from "../utils/storefront-defaults";
import type { StorefrontMenu, StorefrontPage, StorefrontTheme } from "~/types/storefront";

interface StorefrontDocument {
  MenuItems?: StorefrontMenu[];
  headerMenuId?: string | null;
  pages?: StorefrontPage[];
  theme?: StorefrontTheme;
}

/** Compatibility facade: editors save through their own domain store. */
export function useStorefront() {
  const pageStore = useStorefrontPages();
  const themeStore = useStorefrontTheme();
  const menuStore = useStorefrontMenus();
  const catalog = useStorefrontCatalog();
  const api = useApi();
  async function load() {
    await Promise.all([pageStore.load(), themeStore.load(), menuStore.load()]);
    await menuStore.initializeDefaults();
  }
  async function loadPublished() {
    const response = await api<{ configuration: StorefrontDocument | null }>("/storefront/published");
    // Public data must never become the cached admin list (which includes drafts).
    pageStore.initialized.value = false;
    themeStore.initialized.value = false;
    menuStore.initialized.value = false;
    const document = response.configuration;
    pageStore.pages.value = document?.pages ?? [];
    pageStore.pages.value.forEach(page => { ensureNavigationSection(page); ensureFooterSection(page); page.sections.forEach(section => { normalizeCartSection(section); normalizeCheckoutSection(section); }); });
    themeStore.theme.value = document?.theme?.colors ? document.theme : structuredClone(initialTheme);
    menuStore.MenuItems.value = document?.MenuItems ?? [];
    menuStore.headerMenuId.value = document?.headerMenuId ?? null;
  }
  async function deletePage(id: string) {
    await pageStore.deletePage(id);
    for (const menu of menuStore.MenuItems.value) menu.items = menu.items.filter(item => item.pageId !== id);
  }
  return {
    ...pageStore, ...catalog,
    theme: themeStore.theme,
    MenuItems: menuStore.MenuItems,
    headerMenuId: menuStore.headerMenuId,
    headerMenu: menuStore.headerMenu,
    pageStore, themeStore, menuStore,
    load, loadPublished, deletePage,
  };
}
