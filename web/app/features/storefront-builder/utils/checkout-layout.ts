import type { BlockType, SectionLayout, StorefrontSection, StorefrontBlock } from "~/types/storefront";
export const checkoutBlockTypes: BlockType[] = ["CHECKOUT_CONTACT", "CHECKOUT_FULFILLMENT", "CHECKOUT_NOTES", "CHECKOUT_PAYMENT", "CHECKOUT_SUMMARY", "CHECKOUT_SUBMIT"];
export function checkoutSectionDefaults() {
  return { layout: { direction: "horizontal", alignment: "left", position: "top", gap: 28, width: "page", height: "auto", customHeight: 0, alignTextBaseline: false } as SectionLayout,
    padding: { top: 24, right: 24, bottom: 24, left: 24 } };
}
export function checkoutBlockDefaults(type: BlockType) {
  const button = type === "CHECKOUT_SUBMIT";
  return { ...(button ? { label: "" } : type === "HEADER" ? {} : { title: "" }),
    layout: { direction: button ? "horizontal" : "vertical", alignment: button ? "center" : "left", position: "top", gap: button ? 10 : 16, width: "full", height: "auto", customHeight: 0, alignTextBaseline: false } as SectionLayout,
    padding: type === "HEADER" ? { top: 0, right: 0, bottom: 0, left: 0 } : button ? { top: 12, right: 18, bottom: 12, left: 18 } : { top: 24, right: 24, bottom: 24, left: 24 } };
}
export function normalizeCheckoutSection(section: StorefrontSection): boolean {
  if (section.type !== "CHECKOUT") return false;
  const before = JSON.stringify(section);
  const legacy = !Object.hasOwn(section.settings, "layout");
  const defaults = checkoutSectionDefaults();
  section.settings = { ...defaults, ...section.settings, layout: { ...defaults.layout, ...section.settings.layout as object }, padding: { ...defaults.padding, ...section.settings.padding as object } };
  const loose = section.blocks.filter(block => ["EYEBROW", "HEADING", "TEXT"].includes(block.type));
  if (loose.length) {
    let header = section.blocks.find(block => block.type === "HEADER");
    if (!header) {
      header = { id: crypto.randomUUID(), type: "HEADER", enabled: true, settings: checkoutBlockDefaults("HEADER"), blocks: [] };
      section.blocks.splice(section.blocks.indexOf(loose[0]!), 0, header);
    }
    header.blocks = [...header.blocks ?? [], ...loose];
    section.blocks = section.blocks.filter(block => !loose.includes(block));
  }
  if (legacy && !section.blocks.some(block => checkoutBlockTypes.includes(block.type))) {
    section.blocks.push(...checkoutBlockTypes.map(type => ({ id: crypto.randomUUID(), type, enabled: true, settings: checkoutBlockDefaults(type) })));
  }
  for (const block of section.blocks) {
    if (!checkoutBlockTypes.includes(block.type)) continue;
    const defaults = checkoutBlockDefaults(block.type);
    block.settings = { ...defaults, ...block.settings, layout: { ...defaults.layout, ...block.settings.layout as object }, padding: { ...defaults.padding, ...block.settings.padding as object } };
  }
  return before !== JSON.stringify(section);
}
