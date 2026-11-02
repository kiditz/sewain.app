<script setup lang="ts">
import type { StorefrontBlock, SectionLayout } from '~/types/storefront';
import BlockRenderer from '~/components/storefront/BlockRenderer.vue';
import { useStorefront } from '../../composables/useStorefront';
import { contentSectionSettings } from '../../utils/content-section-layout';
import { getLayoutWidthStyle, getPaddingStyle } from '../../utils/block-settings';
const props = defineProps<{settings: Record<string, unknown>; blocks: StorefrontBlock[]; preview?: boolean; selectedBlockId?: string | null}>();
const {theme} = useStorefront();
const normalized = computed(() => contentSectionSettings(props.settings));
const style = computed(() => ({...getLayoutWidthStyle(normalized.value.layout, theme.value), ...getPaddingStyle(normalized.value.padding, theme.value, true)}));
const layout = computed<SectionLayout>(() => ({...normalized.value.layout, width: 'full'}));
</script>
<template>
  <section class="structured-section" :class="`structured-section--${settings.variant || 'default'}`" :style="style">
    <BlockRenderer :blocks="blocks" :layout="layout" :preview="preview" :selected-block-id="selectedBlockId" />
  </section>
</template>
<style scoped>
.structured-section { container: structured-section / inline-size; }
.structured-section--columns :deep(.builder-block), .structured-section--split :deep(.builder-block) { flex: 1 1 0; }
.structured-section--jumbo :deep(.block-heading) { font-size: clamp(48px, 10cqw, 150px); line-height: 1; }
.structured-section--pullquote { border-inline-start: 4px solid var(--store-primary); }
.structured-section--pullquote :deep(.block-heading) { font-family: var(--store-heading-font); font-style: italic; font-size: clamp(28px, 4cqw, 52px); }
.structured-section--large-logo :deep(.logo-block) { width: 100%; }
@container structured-section (max-width: 640px) {
  .structured-section--columns > :deep(.block-renderer), .structured-section--split > :deep(.block-renderer) { flex-direction: column !important; }
  .structured-section--columns :deep(.builder-block), .structured-section--split :deep(.builder-block) { width: 100%; flex-basis: auto; }
}
</style>
