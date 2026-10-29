import type { StorefrontBlock, SectionLayout, StorefrontPadding } from "~/types/storefront";
import { useStorefront } from "./useStorefront";
import { getLayoutStyle, getPaddingStyle } from "../utils/block-settings";
import { checkoutBlockDefaults } from "../utils/checkout-layout";
export function useCheckoutBlockStyle(block: () => StorefrontBlock) {
  const { theme } = useStorefront();
  return computed(() => {
    const value = block();
    const defaults = checkoutBlockDefaults(value.type);
    return { ...getLayoutStyle({ ...defaults.layout, ...value.settings.layout as Partial<SectionLayout> }, theme.value),
      ...getPaddingStyle({ ...defaults.padding, ...value.settings.padding as StorefrontPadding }, theme.value), boxSizing: "border-box" as const, minWidth: 0, flexWrap: "wrap" as const };
  });
}
