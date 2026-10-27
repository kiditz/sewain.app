<script setup lang="ts">
import type { CollectionSummary } from "~/types/collection";
import type { Product } from "~/types/product";
import type { StorefrontBlock } from "~/types/storefront";
import { useStorefront } from "../../composables/useStorefront";

interface CollectionEntry {
  collection: CollectionSummary;
  /** Nama parent, ditampilkan sebagai eyebrow untuk sub-collection. */
  parentName: string | null;
  /** Hanya produk yang tersedia. */
  products: Product[];
}

const props = defineProps<{ block: StorefrontBlock }>();

/** Jumlah produk yang diambil per collection (produk tidak tersedia disaring). */
const FETCH_SIZE = 24;
/** Batas collection yang dimuat produknya dalam satu block. */
const MAX_COLLECTIONS = 30;

const catalog = useStorefront();
const { collections } = catalog;
await catalog.loadCollections().catch(() => undefined);

const clamp = (value: unknown, fallback: number, min: number, max: number) => {
  const number = Number(value);
  return Math.max(min, Math.min(max, Number.isFinite(number) && number > 0 ? number : fallback));
};

const settings = computed(() => ({
  collectionCount: clamp(props.block.settings.collectionCount, 12, 1, MAX_COLLECTIONS),
  productsPerCollection: clamp(props.block.settings.productsPerCollection, 8, 1, FETCH_SIZE),
  hideEmpty: props.block.settings.hideEmpty !== false,
  showDescription: props.block.settings.showDescription !== false,
  showViewAll: props.block.settings.showViewAll !== false,
}));

/**
 * Collection yang dipakai: sesuai pilihan (urut sesuai pilihan) atau semua.
 */
const candidates = computed(() => {
  const selectedIds = String(props.block.settings.collectionIds || "")
    .split(",")
    .filter(Boolean);

  const ordered = selectedIds.length
    ? selectedIds
        .map((id) => collections.value.find((collection) => collection.id === id))
        .filter((collection) => collection !== undefined)
    : collections.value;

  return ordered.slice(0, MAX_COLLECTIONS);
});

/**
 * Produk "tersedia" = status ACTIVE dan punya minimal satu varian aktif
 * (sama seperti yang dibutuhkan StorefrontProductCard).
 */
const isAvailable = (product: Product) =>
  product.status === "ACTIVE" &&
  (product.variants ?? []).some((variant) => variant.active);

const asyncKey = computed(() => catalog.queryKey("collection-products", {
  ids: candidates.value.map(collection => collection.id), ...settings.value,
}));
const { data: entries } = await useAsyncData<CollectionEntry[]>(asyncKey, async () => {
  const names = new Map(collections.value.map(collection => [collection.id, collection.name]));
  const result: CollectionEntry[] = [];
  // Fetch small batches and stop once enough nonempty collections are visible.
  for (let offset = 0; offset < candidates.value.length;) {
    const batch = candidates.value.slice(offset, offset + Math.min(4, settings.value.collectionCount - result.length));
    const loaded = await Promise.all(batch.map(async collection => {
      let products: Product[] = [];
      try {
        const response = await catalog.fetchCollectionProducts(collection.id, {
          page: 0, size: settings.value.productsPerCollection, available: true,
        });
        products = response.items.filter(isAvailable);
      } catch { /* A failed collection does not hide the rest of the section. */ }
      return { collection, parentName: collection.parentId ? names.get(collection.parentId) ?? null : null, products };
    }));
    offset += batch.length;
    result.push(...loaded.filter(entry => !settings.value.hideEmpty || entry.products.length));
    if (result.length >= settings.value.collectionCount) break;
  }
  return result;
}, { default: () => [] });

const visibleEntries = computed(() =>
  (entries.value ?? [])
    .filter((entry) => !settings.value.hideEmpty || entry.products.length > 0)
    .slice(0, settings.value.collectionCount),
);

const productsOf = (entry: CollectionEntry) =>
  entry.products.slice(0, settings.value.productsPerCollection);
</script>

<template>
  <div v-if="visibleEntries.length" class="block-collection-products">
    <section
      v-for="entry in visibleEntries"
      :id="entry.collection.slug"
      :key="entry.collection.id"
      class="collection-group"
    >
      <header class="collection-group__header">
        <div class="collection-group__title">
          <p v-if="entry.parentName" class="collection-group__parent">
            {{ entry.parentName }}
          </p>
          <h3>{{ entry.collection.name }}</h3>
          <p
            v-if="settings.showDescription && entry.collection.description"
            class="collection-group__description"
          >
            {{ entry.collection.description }}
          </p>
        </div>

        <NuxtLink
          v-if="settings.showViewAll"
          :to="`/collections/${entry.collection.slug}`"
          class="collection-group__link"
        >
          {{ $t("dashboard.viewAll") }}
          <v-icon icon="mdi-arrow-right" size="18" />
        </NuxtLink>
      </header>

      <div v-if="entry.products.length" class="collection-group__grid">
        <StorefrontProductCard
          v-for="product in productsOf(entry)"
          :key="product.id"
          :product="product"
        />
      </div>
      <p v-else class="empty-catalog">
        {{ $t("ui.no_products_available_in_this_collection_right_now") }}
      </p>
    </section>
  </div>
  <p v-else class="empty-catalog">
    {{ $t("ui.no_collections_with_available_products_to_display") }}
  </p>
</template>

<style scoped>
.block-collection-products {
  display: flex;
  flex-direction: column;
  gap: 56px;
  width: 100%;
  text-align: left;
}

.collection-group__header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.collection-group__title h3 {
  margin: 0;
  font-family: var(--store-heading-font, Georgia, serif);
  font-size: clamp(22px, 3vw, 32px);
  color: var(--store-text);
}

.collection-group__parent {
  margin: 0 0 4px;
  color: var(--store-primary);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.collection-group__description {
  max-width: 640px;
  margin: 8px 0 0;
  color: var(--store-text);
  opacity: 0.72;
}

.collection-group__link {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  gap: 4px;
  color: var(--store-primary);
  font-weight: 600;
  text-decoration: none;
}

.collection-group__link:hover {
  text-decoration: underline;
}

.collection-group__grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 24px 18px;
  width: 100%;
}

.empty-catalog {
  color: var(--store-text);
  opacity: 0.65;
}

@media (max-width: 900px) {
  .collection-group__grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 700px) {
  .collection-group__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .collection-group__header {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (max-width: 460px) {
  .collection-group__grid {
    grid-template-columns: 1fr;
  }
}
</style>
