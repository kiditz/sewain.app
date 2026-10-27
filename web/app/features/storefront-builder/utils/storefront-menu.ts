import type { StorefrontMenu, StorefrontMenuItem, StorefrontPage } from "~/types/storefront";

export function defaultMenus(): StorefrontMenu[] {
  return [{
    id: "main-menu",
    name: "Main Menu",
    items: [
      { id: "home", pageId: "home" },
      { id: "products", pageId: "all-products" },
      { id: "collections", pageId: "collections" },
    ],
  }];
}

export function menuPages(pages: StorefrontPage[]): StorefrontPage[] {
  return pages.filter(page => page.kind !== "PRODUCT" && isMenuUrl(page.targetUrl));
}

export function menuItemPage(item: StorefrontMenuItem, pages: StorefrontPage[]) {
  return menuPages(pages).find(page => item.pageId ? page.id === item.pageId : page.targetUrl === item.url);
}

export function resolveMenuItems(items: StorefrontMenuItem[], pages: StorefrontPage[]) {
  return items.flatMap(item => {
    if (item.target) {
      const { type, title, slug } = item.target;
      if (!["PRODUCT", "COLLECTION"].includes(type) || !slug || !title) return [];
      return [{ ...item, label: item.label?.trim() || title, url: `/${type === "PRODUCT" ? "products" : "collections"}/${encodeURIComponent(slug)}` }];
    }
    const page = menuItemPage(item, pages);
    if (!page || page.status !== "PUBLISHED") return [];
    return [{ ...item, label: item.label?.trim() || page.title, url: page.targetUrl }];
  });
}

export function menuItemTitle(item: StorefrontMenuItem, pages: StorefrontPage[]) {
  return item.target?.title || menuItemPage(item, pages)?.title || "";
}

export function hasMenuDestination(item: StorefrontMenuItem, pages: StorefrontPage[]) {
  return item.target
    ? ["PRODUCT", "COLLECTION"].includes(item.target.type) && !!item.target.id && !!item.target.slug && !!item.target.title
    : !!menuItemPage(item, pages);
}

export function isMenuUrl(value: string): boolean {
  if (/^\/(?![\/\\])[^\\\s]*$/.test(value)) return true;
  try {
    const url = new URL(value);
    return ["https:", "http:"].includes(url.protocol);
  } catch {
    return false;
  }
}
