<script setup lang="ts">
import { useStorefront } from "../../composables/useStorefront";
import { getBlockSettings, getPaddingStyle } from "../../utils/block-settings";
import type { StorefrontBlock, StorefrontPadding } from "~/types/storefront";
const storefront = useStorefront();

const props = defineProps<{ block: StorefrontBlock }>();

const settings = computed(() =>
  getBlockSettings<{ text?: string }>(props.block),
);
const padding = computed(() => props.block.settings.padding as StorefrontPadding);
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
    :style="textStyle"
    :class="{ 'block-eyebrow--fill': width === 'fill' }"
    class="block-eyebrow ql-editor"
    v-html="settings.text"
  ></div>
</template>
<style scoped>
.block-eyebrow {
  box-sizing: border-box;
  font-family: var(--store-heading-font, Georgia, serif);
  font-size: 0.75em;
  font-weight: 700;
  letter-spacing: 0.16em;
  color: var(--store-primary);
  width: fit-content;
}

.block-eyebrow--fill {
  width: 100%;
}
</style>
