import type { StorefrontMenu, StorefrontPage, StorefrontSection, SectionLayout } from "~/types/storefront";

export function headerDefaults() {
  return {
    menuId: null as string | null,
    padding: { top: 20, right: 24, bottom: 20, left: 24 },
    layout: {
      direction: "horizontal", alignment: "space-between", position: "center",
      gap: 24, width: "page", height: "auto", customHeight: 0, alignTextBaseline: false,
    } as SectionLayout,
  };
}

export function createHeaderSection(): StorefrontSection {
  return { id: crypto.randomUUID(), type: "HEADER", enabled: true, settings: headerDefaults(), blocks: [] };
}

/** Upgrade old pages without duplicating or re-enabling their header. */
export function ensureHeaderSection(page: StorefrontPage): boolean {
  if (page.sections.some(section => section.type === "HEADER")) return false;
  page.sections.unshift(createHeaderSection());
  return true;
}

export function resolveHeaderMenu(menus: StorefrontMenu[], selectedId: unknown, defaultId: string | null) {
  if (selectedId === "__none__") return undefined;
  const id = typeof selectedId === "string" && selectedId ? selectedId : defaultId;
  return menus.find(menu => menu.id === id);
}
