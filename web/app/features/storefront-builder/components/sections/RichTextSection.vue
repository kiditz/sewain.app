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
  <section class="rich-text">
    <div
      class="rich-text__inner"
      :class="`align-${settings.alignment || 'center'}`"
    >
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
.rich-text {
  padding: var(--store-spacing) 24px;
}
.rich-text__inner {
  max-width: 760px;
  margin: auto;
}
.align-center {
  text-align: center;
}
.align-right {
  text-align: right;
}
.align-center :deep(.block-button) {
  align-self: center;
}
.align-right :deep(.block-button) {
  align-self: flex-end;
}
.rich-text :deep(.block-heading) {
  font-size: clamp(30px, 4vw, 46px);
}
.rich-text :deep(.block-text) {
  line-height: 1.85;
}
</style>
