<script setup lang="ts">
import { getBlockSettings, getPaddingStyle } from "../../utils/block-settings";

import {
  allPaddingZero,
  type StorefrontBlock,
  type StorefrontPadding,
  type StorefrontProduct,
} from "~/types/storefront";
import type { Product } from "~/types/product";
import { useStorefront } from "../../composables/useStorefront";

const props = defineProps<{
  block: StorefrontBlock;
  product?: StorefrontProduct;
  productContext?: Product;
  preview?: boolean;
  selectedBlockId?: string | null;
  productDescription?: boolean;
}>();
const storefront = useStorefront();
const { t } = useI18n();
const store = useStorefrontProductStore();
const { selectedVariant } = storeToRefs(store);

const settings = computed(() =>
  getBlockSettings<{
    showPeriod: boolean;
  }>(props.block),
);
const rentalPlan = computed(() => {
  const plans = props.productContext?.variants
    .filter((variant) => variant.active)
    .flatMap((variant) => variant.rentalPlans)
    .filter((plan) => plan.active && Number.isFinite(plan.rentalFee))
    .sort((left, right) => left.rentalFee - right.rentalFee);
  return (
    plans?.find((plan) => plan.durationUnit === "DAY") ??
    plans?.[0] ??
    selectedVariant.value?.rentalPlans.find(
      (plan) => plan.durationUnit === "DAY",
    )
  );
});
const rentalFee = computed(() => rentalPlan.value?.rentalFee ?? 0);
const periodLabel = computed(() => {
  const plan = rentalPlan.value;
  if (!plan) return t("ui.hari");
  const unit = t(`rentalplan.durationUnitOptions.${plan.durationUnit}`);
  return `/ ${plan.durationValue} ${unit}`;
});
const padding = computed(
  () => (props.block.settings.padding ?? allPaddingZero) as StorefrontPadding,
);
const layout = computed(() => {
  const value = props.block.settings.layout;
  return value !== null && typeof value === "object" && !Array.isArray(value)
    ? (value as Record<string, unknown>)
    : {};
});
const width = computed(() => layout.value.width ?? "match");
const textStyle = computed(() => {
  const paddingStyle = getPaddingStyle(padding.value, storefront.theme.value);
  const narrowPadding = layout.value.maxWidth === "narrow" ? 24 : 0;

  return {
    ...paddingStyle,
    textAlign:
      (layout.value.alignment as "left" | "center" | "right" | undefined) ??
      "left",
    paddingLeft: `${(padding.value?.left ?? storefront.theme.value.layout.sectionSpacing) + narrowPadding}px`,
    paddingRight: `${(padding.value?.right ?? storefront.theme.value.layout.sectionSpacing) + narrowPadding}px`,
  };
});

</script>

<template>
  <div
    class="product-price"
    :class="{ 'product-price--fill': width === 'fill' }"
    :style="textStyle"
  >
    <strong>
      {{
        money(rentalFee)
      }}
    </strong>
    <span v-if="settings.showPeriod !== false">{{ periodLabel }}</span>
  </div>
</template>

<style scoped>
.product-price {
  font-size: 13px;
  box-sizing: border-box;
  width: fit-content;
}
.product-price--fill {
  width: 100%;
}
.product-price strong {
  font-size: 14px;
}
.product-price span {
  margin-inline-start: 5px;
  font-size: 13px;
  opacity: 0.7;
}
</style>
