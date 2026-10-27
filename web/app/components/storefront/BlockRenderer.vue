<script setup lang="ts">
import { VueDraggable } from "vue-draggable-plus";
import type {
  StorefrontBlock,
  SectionLayout,
  StorefrontBorderRadius,
} from "~/types/storefront";
import type { Product } from "~/types/product";
import { blockRegistry } from "~/features/storefront-builder/utils/block-registry";
import { blockComponentRegistry } from "~/features/storefront-builder/utils/block-component-registry";
import {
  getBorderRadiusStyle,
  getLayoutStyle,
} from "~/features/storefront-builder/utils/block-settings";
import { builderDndKey } from "~/features/storefront-builder/utils/builder-dnd";
import { useStorefront } from "~/features/storefront-builder/composables/useStorefront";

const props = defineProps<{
  blocks: StorefrontBlock[];
  layout?: SectionLayout;
  preview?: boolean;
  selectedBlockId?: string | null;
  productDescription?: boolean;
  productContext?: Product;
}>();

const storefront = useStorefront();

const visibleBlocks = computed(() =>
  props.blocks.filter((block) => block.enabled),
);

const layout = computed(() => props.layout as SectionLayout);

const layoutStyle = computed(() => ({
  ...getLayoutStyle(layout.value, storefront.theme.value),
  maxWidth: undefined,
}));

function blockStyle(block: StorefrontBlock) {
  if (["HEADER", "MENU"].includes(block.type)) {
    return {
      width: "100%",
      flex: "0 1 100%",
    };
  }

  if (block.type !== "PRODUCT_CARD" && block.type !== "IMAGE") return undefined;

  const borderRadiusStyle = getBorderRadiusStyle(
    block.settings.borderRadius as StorefrontBorderRadius,
    storefront.theme.value,
  );

  return { width: "100%", ...borderRadiusStyle };
}

function childLayout(block: StorefrontBlock) {
  return block.type === "HEADER"
    ? (block.settings.layout as SectionLayout | undefined)
    : layout.value;
}

// Drag & drop block langsung di preview. Hanya aktif jika dirender di dalam
// PageBuilder (ada provider-nya) dan dalam mode preview. Block hanya bisa
// dipindah di dalam list-nya sendiri (satu section / satu area).
const builder = inject(builderDndKey, null);
const canDrag = computed(() => !!props.preview && !!builder);

const dragBlocks = computed<StorefrontBlock[]>({
  get: () => visibleBlocks.value,
  set: (next) => builder?.reorderBlocks(next.map((block) => block.id)),
});

function isFillTextBlock(block: StorefrontBlock) {
  const layout = block.settings.layout;
  return (
    ["TEXT", "HEADING", "EYEBROW", "PRODUCT_TITLE", "PRODUCT_PRICE", 'MENU'].includes(block.type) &&
    layout !== null &&
    typeof layout === "object" &&
    !Array.isArray(layout) &&
    (layout as Record<string, unknown>).width === "fill"
  );
}
</script>

<template>
  <VueDraggable
    v-model="dragBlocks"
    class="block-renderer"
    :style="layoutStyle"
    :disabled="!canDrag"
    handle=".builder-block-handle"
    ghost-class="builder-ghost"
    :animation="180"
  >
    <div
      v-for="block in dragBlocks"
      :key="block.id"
      class="builder-block"
      :class="{
        'is-selected': preview && block.id === selectedBlockId,
        'is-preview': preview,
        'is-fill': isFillTextBlock(block),
        'is-product-card': block.type === 'PRODUCT_CARD',
        'is-image-block': block.type === 'IMAGE',
      }"
      :style="blockStyle(block)"
      :data-block-id="block.id"
      :data-block-type="block.type"
      :data-block-label="blockRegistry[block.type].label"
    >
      <div
        v-if="canDrag"
        class="builder-handle builder-block-handle"
        :title="$t('ui.drag_to_reorder_block')"
      >
        <v-icon size="14">mdi-drag</v-icon>
        {{ blockRegistry[block.type].label }}
      </div>

      <component
        :is="blockComponentRegistry[block.type]"
        :block="block"
        :preview="preview"
        :selected-block-id="selectedBlockId"
        :product-description="productDescription"
        :product-context="productContext"
      >
        <BlockRenderer
          v-if="block.blocks?.length"
          :blocks="block.blocks"
          :layout="childLayout(block)"
          :preview="preview"
          :selected-block-id="selectedBlockId"
          :product-description="productDescription"
          :product-context="productContext"
        />
      </component>
    </div>
  </VueDraggable>
</template>

<style>
/* Handle drag block (base .builder-handle ada di StorefrontRenderer). */
.builder-block-handle {
  top: -22px;
  left: -2px;
  border-radius: 3px 3px 0 0;
}

.builder-block.is-selected > .builder-block-handle {
  display: inline-flex;
}

.builder-block.is-preview:hover > .builder-block-handle {
  display: inline-flex;
  opacity: 0.85;
}

.builder-block.is-selected > .builder-block-handle {
  opacity: 1;
}
</style>

<style scoped>
.builder-block {
  display: flex;
  flex-direction: column;
  align-items: inherit;
  /* width: 100%;
  min-width: 0; */
}
.builder-block.is-fill {
  width: 100%;
}
.builder-block.is-preview {
  position: relative;
  cursor: pointer;
}
.builder-block.is-selected {
  outline: 2px solid #2563eb;
  outline-offset: 2px;
  position: relative;
  z-index: 2;
  border-radius: 4px;
}
.builder-block.is-selected.is-product-card {
  border-radius: inherit;
}
.builder-block.is-selected.is-image-block {
  border-radius: inherit;
}
.block-renderer {
  display: flex;
  min-width: 0;
  width: 100%;
}

.button-secondary {
  background: var(--store-secondary);
  color: var(--store-text);
}
.button-outline {
  background: transparent;
  border: 1px solid currentColor;
  color: var(--store-primary);
}

.block-product-grid,
.block-collection-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  width: 100%;
}
.block-product-card {
  display: flex;
  flex-direction: column;
  gap: 7px;
  min-width: 0;
}
.block-product-art {
  height: clamp(130px, 20vw, 250px);
  border-radius: var(--store-radius);
  display: grid;
  place-items: center;
  position: relative;
  overflow: hidden;
}
.block-product-art span {
  position: absolute;
  top: 12px;
  left: 12px;
  font-size: 10px;
  letter-spacing: 0.12em;
  font-weight: 700;
  opacity: 0.55;
}
.block-product-art div {
  width: 65px;
  height: 85px;
  border-radius: 15px 15px 22px 22px;
  background: #ffffff80;
  transform: rotate(-8deg);
}
.block-product-card small {
  color: var(--store-primary);
  font-weight: 700;
}
.block-product-card strong {
  font-size: 14px;
}
.block-product-price {
  font-size: 13px;
  opacity: 0.7;
}
.block-collection-card {
  min-height: 170px;
  padding: 18px;
  border-radius: var(--store-radius);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  color: var(--store-text);
  text-decoration: none;
}
.block-collection-card strong {
  font-family: var(--store-heading-font, Georgia, serif);
  font-size: 20px;
}
.block-collection-card small,
.collection-number {
  opacity: 0.65;
}
.collection-number {
  font-size: 12px;
}
.product-title {
  font-family: var(--store-heading-font, Georgia, serif);
  font-size: 14px;
  line-height: 1.1;
  padding: 4px;
  margin: 0;
  color: var(--store-text);
}

.product-control,
.quantity-control {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
}
.product-control select {
  width: 100%;
  min-height: 46px;
  border: 1px solid #d1d5db;
  border-radius: var(--store-radius);
  padding: 0 12px;
  background: var(--store-surface);
  color: var(--store-text);
  font: inherit;
}
.deposit-info {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  background: var(--store-secondary);
  border-radius: var(--store-radius);
  font-size: 13px;
}
.cart-button {
  align-self: stretch;
  justify-content: center;
  border: 0;
  cursor: pointer;
  font: inherit;
  font-weight: 700;
}
.cart-button.button-primary {
  background: var(--store-primary);
  color: white;
}
.cart-button.button-secondary {
  background: var(--store-secondary);
  color: var(--store-text);
}
.cart-button.button-outline {
  background: transparent;
  border: 1px solid var(--store-primary);
  color: var(--store-primary);
}
@media (max-width: 600px) {
  .block-product-grid,
  .block-collection-grid {
    grid-template-columns: 1fr;
  }
  .block-product-art {
    height: 200px;
  }
  .block-heading {
    font-size: 30px;
  }
}
</style>
