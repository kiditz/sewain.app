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
  <section class="product-description">
    <div
      class="product-description__inner"
      :class="`align-${settings.alignment || 'left'}`"
    >
      <BlockRenderer
        :blocks="blocks"
        :preview="preview"
        :selected-block-id="selectedBlockId"
        :product="product"
        :product-description="true"
      />
    </div>
  </section>
</template>
<style scoped>
.product-description {
  padding: var(--store-spacing) 24px;
  border-top: 1px solid #00000012;
}
.product-description__inner {
  max-width: var(--store-width);
  margin: auto;
}
.align-center {
  text-align: center;
}
.align-right {
  text-align: right;
}
.product-description :deep(.block-heading) {
  font-size: clamp(28px, 4vw, 42px);
}
.product-description :deep(.block-text) {
  max-width: 900px;
}
</style>
