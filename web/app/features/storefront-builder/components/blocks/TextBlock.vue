<script setup lang="ts">
import { useStorefront } from "../../composables/useStorefront";
import { getBlockSettings, getPaddingStyle } from "../../utils/block-settings";
import type { StorefrontBlock, StorefrontPadding } from "~/types/storefront";
const storefront = useStorefront();
const store = useStorefrontProductStore();
const { product } = storeToRefs(store);
const props = defineProps<{
  block: StorefrontBlock;
  productDescription?: boolean;
}>();

const settings = computed(() =>
  getBlockSettings<{ text?: string }>(props.block),
);

const text = computed(() => {
  if (props.productDescription && product?.value?.description) {
    return product?.value.description;
  }

  return settings.value.text ?? "";
});

const padding = computed(
  () => props.block.settings.padding as StorefrontPadding,
);
const paddingStyle = computed(() =>
  getPaddingStyle(padding.value, storefront.theme.value),
);
const layout = computed(() => {
  const value = props.block.settings.layout;
  return value !== null && typeof value === "object" && !Array.isArray(value)
    ? (value as Record<string, unknown>)
    : {};
});
const width = computed(() => layout.value.width ?? "match");
const textStyle = computed(() => {
  const narrowPadding = layout.value.maxWidth === "narrow" ? 24 : 0;

  return {
    ...paddingStyle.value,
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
    v-if="productDescription"
    class="block-text ql-editor"
    :class="{ 'block-text--fill': width === 'fill' }"
    :style="textStyle"
    v-html="product?.description"
  />
  <div
    v-else
    class="block-text ql-editor"
    :class="{ 'block-text--fill': width === 'fill' }"
    :style="textStyle"
    v-html="text"
  />
</template>
<style scoped>
.block-text {
  box-sizing: border-box;
  font-size: 1em;
  line-height: 1.8;
  margin: 0;
  white-space: pre-line;
  opacity: 0.78;
  width: fit-content;
}

.block-text--fill {
  width: 100%;
}
</style>
