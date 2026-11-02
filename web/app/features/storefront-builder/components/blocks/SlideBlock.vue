<script setup lang="ts">
import type { StorefrontBlock } from "~/types/storefront";
import { useStorefront } from "../../composables/useStorefront";
import { getPaddingStyle } from "../../utils/block-settings";
import { slideSettings } from "../../utils/slideshow";

const props = defineProps<{ block: StorefrontBlock }>();
const { theme } = useStorefront();
const settings = computed(() => slideSettings(props.block.settings));
const contentStyle = computed(() => ({
  ...getPaddingStyle(settings.value.padding, theme.value),
  maxWidth: settings.value.layout.width === "page" ? `${theme.value.layout.contentMaxWidth}px` : undefined,
  "--slideshow-content-height": `max(0px, calc(var(--slideshow-slide-height) - ${Number(settings.value.padding.top ?? 0) + Number(settings.value.padding.bottom ?? 0)}px))`,
}));
const overlayStyle = computed(() => ({
  backgroundColor: settings.value.appearance.overlayColor,
  opacity: Math.min(100, Math.max(0, Number(settings.value.appearance.overlayOpacity) || 0)) / 100,
}));
</script>

<template>
  <div class="slideshow-slide" :style="{ backgroundColor: settings.appearance.backgroundColor }">
    <div v-if="settings.image.src" class="slideshow-slide__background">
      <v-img :src="settings.image.src" :alt="settings.image.alt" cover height="100%" width="100%" />
    </div>
    <div class="slideshow-slide__overlay" :style="overlayStyle" aria-hidden="true" />
    <div class="slideshow-slide__content" :style="contentStyle"><slot /></div>
  </div>
</template>

<style scoped>
.slideshow-slide { position: relative; isolation: isolate; width: 100%; min-width: 0; min-height: var(--slideshow-slide-height); color: var(--store-text); background: var(--store-surface); }
.slideshow-slide__background, .slideshow-slide__overlay { position: absolute; inset: 0; pointer-events: none; }
.slideshow-slide__background { z-index: -2; }
.slideshow-slide__overlay { z-index: -1; }
.slideshow-slide__content { width: 100%; min-width: 0; margin: 0 auto; box-sizing: border-box; }
.slideshow-slide__content > :deep(.block-renderer) { min-height: var(--slideshow-content-height); }
.slideshow-slide__content :deep(.builder-block) { min-width: 0; max-width: 100%; }
@container storefront-slideshow (max-width: 640px) {
  .slideshow-slide__content > :deep(.block-renderer) { flex-wrap: wrap; }
}
</style>
