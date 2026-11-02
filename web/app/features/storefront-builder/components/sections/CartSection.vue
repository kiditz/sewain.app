<script setup lang="ts">
import type { BlockType, StorefrontBlock, SectionLayout, StorefrontPadding } from "~/types/storefront";
import BlockRenderer from "~/components/storefront/BlockRenderer.vue";
import { useCartView } from "../../composables/useCartView";
import { useStorefront } from "../../composables/useStorefront";
import { getLayoutStyle, getPaddingStyle } from "../../utils/block-settings";
import { cartSectionDefaults } from "../../utils/cart-layout";

/**
 * Section halaman cart. Block dibagi ke tiga area berdasarkan tipenya:
 *  - header : HEADER (EYEBROW, HEADING, TEXT)
 *  - main   : CART_ITEMS
 *  - side   : block lain (CART_SUMMARY, CART_CHECKOUT, BUTTON, ...)
 * Urutan di dalam tiap area mengikuti urutan block di builder.
 */
const props = defineProps<{
  settings: Record<string, unknown>;
  blocks: StorefrontBlock[];
  preview?: boolean;
  selectedBlockId?: string | null;
}>();

const { theme } = useStorefront();
const defaults = cartSectionDefaults();
const layout = computed(() => ({ ...defaults.layout, ...props.settings.layout as Partial<SectionLayout> }));
const sectionStyle = computed(() => ({
  ...getLayoutStyle(layout.value, theme.value),
  flexDirection: "column" as const,
  alignItems: "stretch",
  justifyContent: "flex-start",
  ...getPaddingStyle({ ...defaults.padding, ...props.settings.padding as StorefrontPadding }, theme.value),
}));
const contentStyle = computed(() => ({
  ...getLayoutStyle({ ...layout.value, width: "full", height: "auto" }, theme.value),
}));
const blockLayout = computed<SectionLayout>(() => ({
  ...layout.value, direction: "vertical", width: "full", height: "auto",
}));

const HEADER_BLOCKS: BlockType[] = ["HEADER"];
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
  <section class="cart-section" :style="sectionStyle">
    <div v-if="hasHeader" class="cart-section__header">
      <BlockRenderer
        :blocks="headerBlocks"
        :layout="blockLayout"
        :preview="preview"
        :selected-block-id="selectedBlockId"
      />
    </div>

    <div
      class="cart-section__layout"
      :style="contentStyle"
      :class="{
        'cart-section__layout--single': singleColumn || layout.direction === 'vertical',
        'cart-section__layout--summary-left': summaryOnLeft,
      }"
    >
      <div class="cart-section__main">
        <BlockRenderer
          :blocks="mainBlocks"
          :layout="blockLayout"
          :preview="preview"
          :selected-block-id="selectedBlockId"
        />
      </div>

      <div v-if="hasSide" v-show="!singleColumn" class="cart-section__side">
        <BlockRenderer
          :blocks="sideBlocks"
          :layout="blockLayout"
          :preview="preview"
          :selected-block-id="selectedBlockId"
        />
      </div>
    </div>
  </section>
</template>

<style scoped>
.cart-section { container: cart-section / inline-size; box-sizing: border-box; width: 100%; min-width: 0; color: var(--store-text); }
.cart-section__header { min-width: 0; }
.cart-section__header :deep(.block-heading) { font-size: clamp(30px, 4vw, 42px); }
.cart-section__layout { min-width: 0; }
.cart-section__main { flex: 1 1 0; min-width: 0; }
.cart-section__side { flex: 0 1 350px; min-width: 0; }
.cart-section__layout--summary-left .cart-section__main { order: 2; }
.cart-section__layout--summary-left .cart-section__side { order: 1; }
.cart-section__layout--single { flex-direction: column !important; }
.cart-section__layout--single > div { flex: 0 1 auto; width: 100%; }
.cart-section :deep(.builder-block) { min-width: 0; max-width: 100%; }
.cart-section :deep([data-block-type="CART_ITEMS"]),
.cart-section :deep([data-block-type="CART_SUMMARY"]),
.cart-section :deep([data-block-type="CART_CHECKOUT"]) { width: 100%; }
@container cart-section (max-width: 900px) {
  .cart-section__layout { flex-direction: column !important; align-items: stretch !important; }
  .cart-section__main, .cart-section__side { flex: 0 1 auto; width: 100%; order: 0 !important; }
}
</style>
