<script setup lang="ts">
import ResourceListLayout from "../ResourceListLayout.vue";
import {resourceListItemStyles} from "../../utils/resource-list-layout";
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
  product?: Product;
  blocks: StorefrontBlock[];
  preview?: boolean;
  selectedBlockId?: string | null;
}>();

const storefront = useStorefront();
const sectionStyle = computed(() => ({
  ...getLayoutStyle(props.settings.layout as SectionLayout, storefront.theme.value),
  ...getPaddingStyle(props.settings.padding as StorefrontPadding, storefront.theme.value, true),
}));
const collectionId = computed(() => {
  const source = props.settings.source;
  return typeof source === "string" && source !== "" ? source : null;
});
const count = computed(() => Math.max(1, Math.min(100, Number(props.settings.count) || 3)));
const selectedIds = computed(() => [...new Set(String(props.settings.productIds ?? "").split(",").filter(Boolean))]);
const productCardTemplate = computed(() => props.blocks.find(block => block.type === "PRODUCT_CARD"));
const contentBlocks = computed(() => props.blocks.filter(block => block.type !== "PRODUCT_CARD"));
const requestKey = computed(() => storefront.queryKey("featured", {
  recommendation: props.settings.recommendation, currentProduct: props.product?.id, productType: props.product?.type?.id, collection: collectionId.value, ids: selectedIds.value, count: count.value, enabled: productCardTemplate.value?.enabled ?? false,
}));
const { data: featuredProducts } = await useAsyncData<Product[]>(requestKey, async () => {
  if (!productCardTemplate.value?.enabled) return [];
  const fetchPage = (query: { page: number; size: number; id?: string }) => collectionId.value
    ? storefront.fetchCollectionProducts(collectionId.value, query) : storefront.fetchProducts(query);
  if (props.settings.recommendation && !selectedIds.value.length) {
    const query = { page: 0, size: Math.min(100, count.value + 1), available: true, ...(props.product?.type?.id ? { "type.id": props.product.type.id } : {}) };
    let items = (await storefront.fetchProducts(query)).items.filter(item => item.id !== props.product?.id);
    if (!items.length && props.product?.type?.id) items = (await storefront.fetchProducts({ page: 0, size: Math.min(100, count.value + 1), available: true })).items.filter(item => item.id !== props.product?.id);
    return items.slice(0, count.value);
  }
  if (!selectedIds.value.length) return (await fetchPage({ page: 0, size: count.value })).items;
  const ordered: Product[] = [];
  // Fetch configured IDs in bounded batches, preserving the merchant's order.
  for (let offset = 0; offset < selectedIds.value.length && ordered.length < count.value; offset += 100) {
    const ids = selectedIds.value.slice(offset, offset + 100);
    const result = await fetchPage({ page: 0, size: ids.length, id: ids.join(",") });
    const byId = new Map(result.items.map(product => [product.id, product]));
    ordered.push(...ids.flatMap(id => byId.has(id) ? [byId.get(id)!] : []));
  }
  return ordered.slice(0, count.value);
}, { default: () => [] });
const visibleProducts = computed(() => featuredProducts.value ?? []);
const displayType = computed(() => {
  const value = String(props.settings.displayType ?? "grid");
  return ["carousel", "grid", "editorial", "bento"].includes(value) ? value : "grid";
});
const itemStyles = computed(() => resourceListItemStyles(displayType.value, visibleProducts.value.length, 'products'));
</script>

<template>
  <section class="featured-products-section" :style="sectionStyle">
    <div class="products-inner" >
      <BlockRenderer
        :blocks="contentBlocks"
        :preview="preview"
        :selected-block-id="selectedBlockId"
      />
      <ResourceListLayout
        v-if="productCardTemplate && visibleProducts.length"
        class="product-list-grid"
        :layout="displayType"
        :columns="Number(settings.columns) || 3"
        :mobile-columns="Number(settings.mobileColumns) || 1"
        :gap="Number(settings.itemsGap ?? (displayType === 'editorial' ? 32 : 18))"
      >
        <div
          v-for="(item, index) in visibleProducts"
          :key="item.id"
          class="resource-layout__item product-list-card"
          :style="itemStyles[index]"
        >
          <BlockRenderer
            :blocks="productCardTemplate ? [productCardTemplate] : []"
            :preview="preview"
            :selected-block-id="selectedBlockId"
            :product-context="item"
          />
        </div>
      </ResourceListLayout>
      <p v-else-if="productCardTemplate?.enabled" class="empty-catalog">
        {{ $t("ui.no_active_products_to_display") }}
      </p>
    </div>
  </section>
</template>

<style scoped>
.featured-products-section { container: featured-products / inline-size; }
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
.product-list-grid { width:100%;min-width:0; }
.product-list-card {
  min-width: 0;
}
.empty-catalog {
  color: var(--store-text);
  opacity: 0.65;
}
</style>
