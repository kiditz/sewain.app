<script setup lang="ts">
import {sanitizeRichText} from "~/utils/content-security";
import { useStorefront } from "../../composables/useStorefront";
import { getBlockSettings, getPaddingStyle, getBorderRadiusStyle } from "../../utils/block-settings";
import type { StorefrontBlock, StorefrontPadding, StorefrontBorderRadius } from "~/types/storefront";
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
const appearance = computed(() => props.block.settings.style as { color?: string; backgroundColor?: string } | undefined);
const textStyle = computed(() => {
  return {
    ...paddingStyle.value,
    ...getBorderRadiusStyle({ topLeft: 0, topRight: 0, bottomRight: 0, bottomLeft: 0, ...props.block.settings.borderRadius as StorefrontBorderRadius }, storefront.theme.value),
    color: String(appearance.value?.color || "var(--store-primary)"),
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
    :class="{ 'block-eyebrow--fill': width === 'fill' }"
    class="block-eyebrow ql-editor"
    v-html="sanitizeRichText(settings.text)"
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
