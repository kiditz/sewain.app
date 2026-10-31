import type { BlockType, SectionLayout, StorefrontPadding } from "~/types/storefront";

export const productControlTypes: BlockType[] = [
  "VARIANT_SELECTOR", "RENTAL_PLAN_SELECTOR", "DEPOSIT_INFO",
  "QUANTITY_SELECTOR", "PRICE_SUMMARY", "ADD_TO_CART",
];

export function productInformationDefaults() {
  return {
    mediaPosition: "left",
    layout: { direction: "horizontal", alignment: "left", position: "top", gap: 24, width: "page", height: "auto", customHeight: 0, alignTextBaseline: false } as SectionLayout,
    padding: { top: 32, right: 24, bottom: 32, left: 24 },
  };
}

export function productControlDefaults(type: BlockType) {
  const padding: StorefrontPadding = type === "ADD_TO_CART"
    ? { top: 13, right: 19, bottom: 13, left: 19 }
    : type === "PRICE_SUMMARY"
      ? { top: 16, right: 16, bottom: 16, left: 16 }
      : type === "DEPOSIT_INFO"
        ? { top: 14, right: 16, bottom: 14, left: 16 }
        : { top: 0, right: 0, bottom: 0, left: 0 };
  return { layout: { width: "full" as const }, padding };
}

export function objectSettings(value: unknown): Record<string, unknown> {
  return value && typeof value === "object" && !Array.isArray(value)
    ? value as Record<string, unknown> : {};
}

export function productInformationSettings(settings: Record<string, unknown>) {
  const defaults = productInformationDefaults();
  return {
    ...defaults, ...settings,
    layout: { ...defaults.layout, ...objectSettings(settings.layout) } as SectionLayout,
    padding: { ...defaults.padding, ...objectSettings(settings.padding) } as StorefrontPadding,
  };
}
