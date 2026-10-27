<script setup lang="ts">
import type { Product } from "~/types/product";
import type {
  SectionLayout,
  StorefrontBlock,
  StorefrontPadding,
} from "~/types/storefront";
import BlockRenderer from "~/components/storefront/BlockRenderer.vue";
import { useStorefront } from "../../composables/useStorefront";
import { getLayoutStyle, getPaddingStyle } from "../../utils/block-settings";

const props = defineProps<{
  settings: Record<string, unknown>;
  blocks: StorefrontBlock[];
  preview?: boolean;
  selectedBlockId?: string | null;
}>();

const storefront = useStorefront();
const { findCollectionProducts } = useCollection();
const sectionStyle = computed(() => ({
  ...getLayoutStyle(props.settings.layout as SectionLayout, storefront.theme.value),
  ...getPaddingStyle(props.settings.padding as StorefrontPadding, storefront.theme.value, true),
}));
const collectionId = computed(() => {
  const source = props.settings.source;
  return typeof source === "string" && source !== "" ? source : null;
});
const collectionProductsKey = computed(
  () => `storefront-featured-products:${collectionId.value ?? "all"}`,
);
const { data: collectionProducts } = await useAsyncData<Product[] | null>(
  collectionProductsKey,
  async () => {
    if (!collectionId.value) return null;
    try {
      const response = await findCollectionProducts(collectionId.value, {
        page: 0,
        size: 100,
      });
      return response.items.filter((item) => item.status === "ACTIVE");
    } catch {
      return [];
    }
  },
  { default: () => null },
);

const productCardTemplate = computed(() =>
  props.blocks.find((block) => block.type === "PRODUCT_CARD"),
);
const contentBlocks = computed(() =>
  props.blocks.filter((block) => block.type !== "PRODUCT_CARD"),
);
const visibleProducts = computed<Product[]>(() => {
  const sourceProducts = collectionId.value
    ? (collectionProducts.value ?? [])
    : storefront.products.value;
  const activeProducts = sourceProducts.filter(
    (item) => item.status === "ACTIVE",
  );
  const selectedIds = String(props.settings.productIds ?? "")
    .split(",")
    .filter(Boolean);
  const orderedProducts = selectedIds.length
    ? selectedIds
        .map((id) => activeProducts.find((item) => item.id === id))
        .filter((item): item is Product => item !== undefined)
    : activeProducts;
  const count = Math.max(1, Math.min(100, Number(props.settings.count) || 3));
  return orderedProducts.slice(0, count);
});
const displayType = computed(() => {
  const value = String(props.settings.displayType ?? "grid");
  return ["carousel", "grid", "editorial"].includes(value) ? value : "grid";
});
const gridStyle = computed(() => ({
  "--product-list-columns": String(
    Math.max(1, Math.min(8, Number(props.settings.columns) || 3)),
  ),
  "--product-list-mobile-columns": String(
    Number(props.settings.mobileColumns) === 2 ? 2 : 1,
  ),
}));
</script>

<template>
  <section :style="sectionStyle">
    <div class="products-inner" >
      <BlockRenderer
        :blocks="contentBlocks"
        :preview="preview"
        :selected-block-id="selectedBlockId"
      />
      <div
        v-if="productCardTemplate && visibleProducts.length"
        class="product-list-grid"
        :class="`product-list-grid--${displayType}`"
        :style="gridStyle"
      >
        <div
          v-for="item in visibleProducts"
          :key="item.id"
          class="product-list-card"
        >
          <BlockRenderer
            :blocks="productCardTemplate ? [productCardTemplate] : []"
            :preview="preview"
            :selected-block-id="selectedBlockId"
            :product-context="item"
          />
        </div>
      </div>
      <p v-else-if="productCardTemplate" class="empty-catalog">
        {{ $t("ui.no_active_products_to_display") }}
      </p>
    </div>
  </section>
</template>

<style scoped>
.products-inner {
  width: 100%;
  min-width: 0;
}

.products-inner :deep(.block-heading) {
  font-size: clamp(30px, 4vw, 44px);
}
.products-inner :deep(.block-text) {
  margin-top: -8px;
}
.products-inner :deep(.product-list-grid) {
  margin-top: 10px;
}
.product-list-grid {
  --product-list-current-columns: var(--product-list-columns, 3);
  display: grid;
  grid-template-columns: repeat(
    var(--product-list-current-columns, 3),
    minmax(0, 1fr)
  );
  gap: 18px;
  width: 100%;
}
.product-list-grid--carousel {
  display: flex;
  overflow-x: auto;
  overscroll-behavior-inline: contain;
  scroll-snap-type: x mandatory;
  scrollbar-width: thin;
}
.product-list-grid--carousel .product-list-card {
  flex: 0 0 calc(100% / var(--product-list-current-columns, 3));
  scroll-snap-align: start;
}
.product-list-grid--editorial {
  grid-auto-rows: 110px;
  align-items: stretch;
}
.product-list-grid--editorial .product-list-card:nth-child(5n + 1) {
  grid-row: span 3;
}
.product-list-grid--editorial .product-list-card:nth-child(5n + 3) {
  grid-row: span 2;
}
.product-list-card {
  min-width: 0;
}
.empty-catalog {
  color: var(--store-text);
  opacity: 0.65;
}
@media (max-width: 600px) {
  .product-list-grid {
    --product-list-current-columns: var(--product-list-mobile-columns, 1);
    grid-template-columns: repeat(
      var(--product-list-current-columns),
      minmax(0, 1fr)
    );
  }
}
</style>
