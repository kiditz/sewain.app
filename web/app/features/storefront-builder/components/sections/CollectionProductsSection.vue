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
  <section class="collection-products">
    <div class="collection-products-inner">
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
.collection-products {
  max-width: var(--store-width);
  padding: var(--store-spacing) 24px;
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
