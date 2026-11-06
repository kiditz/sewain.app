<script setup lang="ts">
import {sanitizeRichText} from "~/utils/content-security";
import { useStorefront } from "../../composables/useStorefront";
import { getBlockSettings, getPaddingStyle, getBorderRadiusStyle } from "../../utils/block-settings";
import type { StorefrontBlock, StorefrontPadding, StorefrontBorderRadius } from "~/types/storefront";
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
const appearance = computed(() => props.block.settings.style as { color?: string; backgroundColor?: string } | undefined);
const textStyle = computed(() => {
  return {
    ...paddingStyle.value,
    ...getBorderRadiusStyle({ topLeft: 0, topRight: 0, bottomRight: 0, bottomLeft: 0, ...props.block.settings.borderRadius as StorefrontBorderRadius }, storefront.theme.value),
    color: String(appearance.value?.color || "var(--store-text)"),
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
    :style="textStyle"
    :class="{ 'block-heading--fill': width === 'fill' }"
    class="ql-editor block-heading"
    v-html="sanitizeRichText(settings.text)"
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
