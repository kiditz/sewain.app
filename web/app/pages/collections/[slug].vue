<script setup lang="ts">
import { useStorefront } from "~/features/storefront-builder/composables/useStorefront";
import type { Collection } from "~/types/collection";
import type { PageResponse } from "~/types/pagination";
import type { Product } from "~/types/product";

const route = useRoute();
const slug = String(route.params.slug);
const storefront = useStorefront();
await storefront.loadPublished();
const { theme, getPageByKind } = storefront;
const { findCollectionBySlug, findCollectionProducts } = useCollection();
const allProductsPage = computed(() =>
  slug === "all" ? getPageByKind("PRODUCT_GRID") : undefined,
);

const { data: collection } = await useAsyncData<Collection | null>(
  `storefront-collection-${slug}`,
  () => (slug === "all" ? Promise.resolve(null) : findCollectionBySlug(slug)),
);
if (slug !== "all" && !collection.value) {
  throw createError({ statusCode: 404, statusMessage: "Collection not found" });
}

const { data: productsPage } = await useAsyncData<PageResponse<Product> | null>(
  `storefront-collection-products-${slug}`,
  () =>
    collection.value
      ? findCollectionProducts(collection.value.id, { page: 0, size: 100 })
      : Promise.resolve(null),
);
const title = computed(
  () => collection.value?.name ?? allProductsPage.value?.title ?? "All products",
);
const visibleProducts = computed(() =>
  (productsPage.value?.items ?? []).filter(
    (product) => product.status === "ACTIVE",
  ),
);
useSeoMeta({ title: () => title.value });
</script>

<template>
  <StorefrontLayout :theme="theme">
    <StorefrontRenderer
      v-if="slug === 'all' && allProductsPage"
      :sections="allProductsPage.sections"
    />
    <section v-else class="collection-page">
      <div class="collection-heading">
        <p class="text-overline">{{ $t("ui.collection") }}</p>
        <h1>{{ title }}</h1>
        <p v-if="collection?.description" class="collection-description">
          {{ collection.description }}
        </p>
      </div>
      <div v-if="visibleProducts.length" class="product-grid">
        <StorefrontProductCard
          v-for="product in visibleProducts"
          :key="product.id"
          :product="product"
        />
      </div>
      <p v-else class="empty-collection">
        {{ $t("ui.there_are_no_products_in_this_collection_yet") }}
      </p>
    </section>
  </StorefrontLayout>
</template>

<style scoped>
.collection-page {
  max-width: var(--store-width);
  margin: 0 auto;
  padding: var(--store-spacing) 24px;
}
.collection-heading {
  margin-bottom: 32px;
}
.collection-heading h1 {
  margin: 0;
  font-family: var(--store-heading-font, Georgia, serif);
  font-size: clamp(32px, 5vw, 52px);
}
.collection-heading .text-overline {
  color: var(--store-primary);
  letter-spacing: 0.12em;
}
.collection-description,
.empty-collection {
  max-width: 680px;
  opacity: 0.72;
}
.product-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 24px 18px;
}
@media (max-width: 700px) {
  .product-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 460px) {
  .product-grid {
    grid-template-columns: 1fr;
  }
}
</style>
