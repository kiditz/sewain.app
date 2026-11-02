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
  <section class="product-description" :style="sectionStyle">
    <div
      class="product-description__inner"
      :class="`align-${normalized.layout.alignment}`"
    >
      <BlockRenderer
        :blocks="blocks"
        :layout="blockLayout"
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
  border-top: 1px solid #00000012;
}
.product-description__inner {
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
</style>
