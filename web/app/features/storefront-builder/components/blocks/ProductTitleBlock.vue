<script setup lang="ts">
import { useStorefront } from "../../composables/useStorefront";
import { getPaddingStyle } from "../../utils/block-settings";
import {
  allPaddingZero,
  type StorefrontBlock,
  type StorefrontPadding,
} from "~/types/storefront";
import type { Product } from "~/types/product";
const storefront = useStorefront();
const store = useStorefrontProductStore();
const { product } = storeToRefs(store);
const props = defineProps<{
  block: StorefrontBlock;
  productContext?: Product;
  preview?: boolean;
  selectedBlockId?: string | null;
  productDescription?: boolean;
}>();
const title = computed(
  () => props.productContext?.name || product.value?.name || "Everyday Camera",
);
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
    class="product-title-block"
    :class="{ 'product-title-block--fill': width === 'fill' }"
    :style="textStyle"
  >
    <strong v-if="productContext && preview">{{ title }}</strong>
    <NuxtLink
      v-else-if="productContext"
      :to="`/products/${productContext.slug}`"
      class="font-weight-bold product-title-link"
    >
      {{ title }}
    </NuxtLink>
    <strong v-else>{{ title }}</strong>
  </div>
</template>

<style scoped>
.product-title-block {
  box-sizing: border-box;
  width: fit-content;
}
.product-title-block--fill {
  width: 100%;
}
.product-title-link {
  color: inherit;
  text-decoration: none;
}
</style>
