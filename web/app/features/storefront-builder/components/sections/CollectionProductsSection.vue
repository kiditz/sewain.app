<script setup lang="ts">
import { useStorefront } from "../../composables/useStorefront";
import { contentSectionSettings } from "../../utils/content-section-layout";
import { getLayoutWidthStyle, getPaddingStyle } from "../../utils/block-settings";
import type { StorefrontBlock, StorefrontProduct } from "~/types/storefront";
import BlockRenderer from "~/components/storefront/BlockRenderer.vue";
const props = defineProps<{
  settings: Record<string, unknown>;
  blocks: StorefrontBlock[];
  preview?: boolean;
  selectedBlockId?: string | null;
  product?: StorefrontProduct;
}>();
const { theme } = useStorefront();
const normalized = computed(() => contentSectionSettings(props.settings, "left"));
const sectionStyle = computed(() => ({ ...getLayoutWidthStyle(normalized.value.layout, theme.value), ...getPaddingStyle(normalized.value.padding, theme.value, true) }));
const blockLayout = computed(() => ({ ...normalized.value.layout, width: "full" as const }));
</script>
<template>
  <section class="collection-products" :style="sectionStyle">
    <div class="collection-products-inner">
      <BlockRenderer
        :blocks="blocks"
        :layout="blockLayout"
        :preview="preview"
        :selected-block-id="selectedBlockId"
        :product="product"
      />
    </div>
  </section>
</template>
<style scoped>
.collection-products {
  margin: auto;
}
.collection-products-inner :deep(.block-heading) {
  font-size: clamp(30px, 4vw, 44px);
}
.collection-products-inner :deep(.block-text) {
  margin-top: -8px;
}
.collection-products-inner :deep(.block-collection-products) {
  margin-top: 16px;
}
</style>
