import type { StorefrontMenuItem, StorefrontPage } from "~/types/storefront";
import { menuItemPage } from "./storefront-menu";
export type BlockLinkDestination = Pick<StorefrontMenuItem, "pageId" | "target">;
export interface SavedBlockLink extends BlockLinkDestination { url: string }

export function isBlockLinkField(key: string, value: unknown): boolean {
  if (value != null && typeof value !== "string") return false;
  if (/^(src|alt|altText)$/i.test(key) || /(?:image|media|logo|favicon|poster|thumbnail)(?:url|src|source)?$/i.test(key)) return false;
  return /(?:url|href|link|linkTo|destination|redirectTo|route|path)$/i.test(key);
}

export function blockLinkUrl(destination: BlockLinkDestination, pages: StorefrontPage[]): string {
  if (destination.target) {
    const { type, slug } = destination.target;
    if (!slug || !["PRODUCT", "COLLECTION"].includes(type)) return "";
    return `/${type === "PRODUCT" ? "products" : "collections"}/${encodeURIComponent(slug)}`;
  }
  return menuItemPage({ id: "block-link", ...destination }, pages)?.targetUrl ?? "";
}

export function savedBlockLinks(value: unknown): Record<string, SavedBlockLink> {
  return value && typeof value === "object" && !Array.isArray(value) ? value as Record<string, SavedBlockLink> : {};
}
