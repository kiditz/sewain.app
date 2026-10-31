import type { StorefrontBlock, StorefrontPage, StorefrontSection, SectionLayout } from "~/types/storefront";
export function footerDefaults() {
  return { padding: { top: 36, right: 24, bottom: 36, left: 24 },
    layout: { direction: "horizontal", alignment: "space-between", position: "center", gap: 24, width: "page", height: "auto", customHeight: 0, alignTextBaseline: false } as SectionLayout };
}
export function socialLinkDefaults() {
  return { network: "instagram", url: "", label: "", openInNewTab: true, size: 24, padding: { top: 8, right: 8, bottom: 8, left: 8 } };
}
export function createFooterSection(): StorefrontSection {
  const padding = () => ({ top: 0, right: 0, bottom: 0, left: 0 });
  const text = (type: "HEADING" | "TEXT", content = ""): StorefrontBlock => ({
    id: crypto.randomUUID(), type, enabled: true,
    settings: { text: content, padding: padding(), style: { color: "", backgroundColor: "#00000000" }, layout: { width: "match", maxWidth: "normal", alignment: "left" } },
  });
  return { id: crypto.randomUUID(), type: "FOOTER", enabled: true, settings: footerDefaults(), blocks: [
    { id: crypto.randomUUID(), type: "HEADER", enabled: true,
      settings: { padding: padding(), layout: { ...footerDefaults().layout, direction: "vertical", alignment: "left", gap: 8, width: "full" } },
      blocks: [text("HEADING"), text("TEXT")] },
    { id: crypto.randomUUID(), type: "SOCIAL_LINK", enabled: true, settings: socialLinkDefaults() },
    text("TEXT", "© {year}"),
  ] };
}
export function ensureFooterSection(page: StorefrontPage): boolean {
  if (page.sections.some(section => section.type === "FOOTER")) return false;
  page.sections.push(createFooterSection());
  return true;
}
