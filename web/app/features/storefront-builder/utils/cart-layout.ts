import type { SectionLayout, StorefrontSection, StorefrontBlock } from "~/types/storefront";

export function cartSectionDefaults() {
  return {
    summaryPosition: "right",
    layout: { direction: "horizontal", alignment: "left", position: "top", gap: 28, width: "page", height: "auto", customHeight: 0, alignTextBaseline: false } as SectionLayout,
    padding: { top: 32, right: 24, bottom: 32, left: 24 },
  };
}
export function cartBlockDefaults(type: StorefrontBlock["type"]) {
  return {
    layout: { direction: type === "CART_CHECKOUT" ? "horizontal" : "vertical", alignment: type === "CART_CHECKOUT" ? "center" : "left", position: "center", gap: type === "CART_CHECKOUT" ? 10 : 20, width: "full", height: "auto", customHeight: 0, alignTextBaseline: false } as SectionLayout,
    padding: type === "CART_SUMMARY" ? { top: 24, right: 24, bottom: 24, left: 24 } : type === "CART_CHECKOUT" ? { top: 12, right: 18, bottom: 12, left: 18 } : { top: 0, right: 0, bottom: 0, left: 0 },
  };
}
export function normalizeCartSection(section: StorefrontSection): boolean {
  if (section.type !== "CART") return false;
  const before = JSON.stringify(section);
  const defaults = cartSectionDefaults();
  section.settings = { ...defaults, ...section.settings,
    layout: { ...defaults.layout, ...section.settings.layout as object },
    padding: { ...defaults.padding, ...section.settings.padding as object },
  };
  const looseHeaderBlocks = section.blocks.filter(block => ["EYEBROW", "HEADING", "TEXT"].includes(block.type));
  if (looseHeaderBlocks.length) {
    let header = section.blocks.find(block => block.type === "HEADER");
    if (!header) {
      header = { id: crypto.randomUUID(), type: "HEADER", enabled: true, settings: cartBlockDefaults("HEADER"), blocks: [] };
      section.blocks.splice(section.blocks.indexOf(looseHeaderBlocks[0]!), 0, header);
    }
    header.blocks = [...(header.blocks ?? []), ...looseHeaderBlocks];
    section.blocks = section.blocks.filter(block => !looseHeaderBlocks.includes(block));
  }
  for (const block of section.blocks) {
    if (!["CART_ITEMS", "CART_SUMMARY", "CART_CHECKOUT"].includes(block.type)) continue;
    const defaults = cartBlockDefaults(block.type);
    block.settings = { ...defaults, ...block.settings,
      layout: { ...defaults.layout, ...block.settings.layout as object },
      padding: { ...defaults.padding, ...block.settings.padding as object },
    };
  }
  return before !== JSON.stringify(section);
}
