<script setup lang="ts">
import { useStorefront } from "../../composables/useStorefront";
import { getBlockSettings, getPaddingStyle, getBorderRadiusStyle } from "../../utils/block-settings";
import type { StorefrontBlock, StorefrontPadding, StorefrontBorderRadius } from "~/types/storefront";
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
const appearance = computed(() => props.block.settings.style as { color?: string; backgroundColor?: string } | undefined);
const textStyle = computed(() => {
  return {
    ...paddingStyle.value,
    ...getBorderRadiusStyle({ topLeft: 0, topRight: 0, bottomRight: 0, bottomLeft: 0, ...props.block.settings.borderRadius as StorefrontBorderRadius }, storefront.theme.value),
    color: String(appearance.value?.color || "color-mix(in srgb, var(--store-text) 78%, transparent)"),
    backgroundColor: String(appearance.value?.backgroundColor || "transparent"),
    maxWidth: layout.value.maxWidth === "narrow" ? "min(100%, 65ch)" : undefined,
    textAlign:
      (layout.value.alignment as "left" | "center" | "right" | undefined) ??
      "left",
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
  width: fit-content;
}

.block-text--fill {
  width: 100%;
}
</style>
