<script setup lang="ts">
import { useStorefront } from "../../composables/useStorefront";
import { getBlockSettings, getPaddingStyle } from "../../utils/block-settings";
import type { StorefrontBlock, StorefrontPadding } from "~/types/storefront";
const storefront = useStorefront();

const props = defineProps<{ block: StorefrontBlock }>();

const settings = computed(() =>
  getBlockSettings<{ text?: string; level?: string }>(props.block),
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
    :class="{ 'block-heading--fill': width === 'fill' }"
    class="ql-editor block-heading"
    v-html="settings.text"
  ></div>
</template>
<style scoped>
.block-heading {
  box-sizing: border-box;
  font-family: var(--store-heading-font, Georgia, serif);
  font-size: clamp(28px, 4vw, 44px);
  line-height: 1.1;
  margin: 0;
  color: var(--store-text);
  overflow: hidden;
  width: fit-content;
}

.block-heading--fill {
  width: 100%;
}
</style>
