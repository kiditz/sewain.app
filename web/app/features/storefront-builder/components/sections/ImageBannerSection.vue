<script setup lang="ts">
import type { StorefrontBlock, StorefrontProduct } from "~/types/storefront";
import BlockRenderer from "~/components/storefront/BlockRenderer.vue";
defineProps<{
  settings: Record<string, string | number | boolean>;
  blocks: StorefrontBlock[];
  preview?: boolean;
  selectedBlockId?: string | null;
  product?: StorefrontProduct;
}>();
</script>
<template>
  <section
    class="image-banner"
    :style="{ background: String(settings.background || '#e8e4dc') }"
  >
    <div class="image-banner__inner">
      <BlockRenderer
        :blocks="blocks"
        :preview="preview"
        :selected-block-id="selectedBlockId"
        :product="product"
      />
    </div>
  </section>
</template>
<style scoped>
.image-banner {
  padding: 44px 24px;
  border-radius: var(--store-radius);
  margin: 0 auto;
  max-width: calc(var(--store-width) + 48px);
}
.image-banner__inner {
  max-width: var(--store-width);
  margin: auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}
.image-banner__inner :deep(.builder-block) {
  width: auto;
  flex: 1;
}
.image-banner__inner :deep(.block-renderer) {
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  gap: 24px;
}
.image-banner__inner :deep(.block-heading) {
  font-size: clamp(28px, 4vw, 42px);
}
.image-banner__inner :deep(.block-button) {
  white-space: nowrap;
  flex-shrink: 0;
}
@media (max-width: 640px) {
  .image-banner__inner :deep(.builder-block) {
    width: auto;
    flex: 1;
  }
  .image-banner__inner :deep(.block-renderer) {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
