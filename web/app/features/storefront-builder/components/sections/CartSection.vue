<script setup lang="ts">
import type { BlockType, StorefrontBlock } from "~/types/storefront";
import BlockRenderer from "~/components/storefront/BlockRenderer.vue";
import { useCartView } from "../../composables/useCartView";

/**
 * Section halaman cart. Block dibagi ke tiga area berdasarkan tipenya:
 *  - header : EYEBROW, HEADING, TEXT
 *  - main   : CART_ITEMS
 *  - side   : block lain (CART_SUMMARY, CART_CHECKOUT, BUTTON, ...)
 * Urutan di dalam tiap area mengikuti urutan block di builder.
 */
const props = defineProps<{
  settings: Record<string, string | number | boolean>;
  blocks: StorefrontBlock[];
  preview?: boolean;
  selectedBlockId?: string | null;
}>();

const HEADER_BLOCKS: BlockType[] = ["EYEBROW", "HEADING", "TEXT"];
const MAIN_BLOCKS: BlockType[] = ["CART_ITEMS"];

const headerBlocks = computed(() =>
  props.blocks.filter((block) => HEADER_BLOCKS.includes(block.type)),
);
const mainBlocks = computed(() =>
  props.blocks.filter((block) => MAIN_BLOCKS.includes(block.type)),
);
const sideBlocks = computed(() =>
  props.blocks.filter(
    (block) =>
      !HEADER_BLOCKS.includes(block.type) && !MAIN_BLOCKS.includes(block.type),
  ),
);

const { isEmpty, loading, error } = useCartView(() => props.preview);

const hasHeader = computed(() => headerBlocks.value.some((b) => b.enabled));
const hasSide = computed(() => sideBlocks.value.some((b) => b.enabled));

// Satu kolom jika tidak ada konten samping, atau cart kosong / gagal dimuat.
const singleColumn = computed(
  () => !hasSide.value || loading.value || !!error.value || isEmpty.value,
);

const summaryOnLeft = computed(() => props.settings.summaryPosition === "left");
</script>

<template>
  <section class="cart-section">
    <div v-if="hasHeader" class="cart-section__header">
      <BlockRenderer
        :blocks="headerBlocks"
        :preview="preview"
        :selected-block-id="selectedBlockId"
      />
    </div>

    <div
      class="cart-section__layout"
      :class="{
        'cart-section__layout--single': singleColumn,
        'cart-section__layout--summary-left': summaryOnLeft,
      }"
    >
      <div class="cart-section__main">
        <BlockRenderer
          :blocks="mainBlocks"
          :preview="preview"
          :selected-block-id="selectedBlockId"
        />
      </div>

      <div v-if="hasSide" v-show="!singleColumn" class="cart-section__side">
        <BlockRenderer
          :blocks="sideBlocks"
          :preview="preview"
          :selected-block-id="selectedBlockId"
        />
      </div>
    </div>
  </section>
</template>

<style scoped>
.cart-section {
  width: 100%;
  max-width: var(--store-width, 1200px);
  margin: 0 auto;
  padding: var(--store-spacing) 24px;
  color: var(--store-text);
}

.cart-section__header {
  margin-bottom: 36px;
}

.cart-section__header :deep(.block-heading) {
  font-size: clamp(30px, 4vw, 42px);
}

.cart-section__layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(280px, 350px);
  align-items: start;
  gap: 28px;
}

.cart-section__layout--summary-left {
  grid-template-columns: minmax(280px, 350px) minmax(0, 1fr);
}

.cart-section__layout--summary-left .cart-section__main {
  order: 2;
}

.cart-section__layout--summary-left .cart-section__side {
  order: 1;
}

.cart-section__layout--single {
  grid-template-columns: minmax(0, 1fr);
}

.cart-section__main,
.cart-section__side {
  min-width: 0;
}

.cart-section__side {
  position: sticky;
  top: 24px;
}

.cart-section__side :deep(.block-renderer) {
  flex-direction: column;
  gap: 18px;
}

@media (max-width: 960px) {
  .cart-section__layout,
  .cart-section__layout--summary-left {
    grid-template-columns: minmax(0, 1fr);
  }

  .cart-section__layout--summary-left .cart-section__main,
  .cart-section__layout--summary-left .cart-section__side {
    order: 0;
  }

  .cart-section__side {
    position: static;
  }
}
</style>
