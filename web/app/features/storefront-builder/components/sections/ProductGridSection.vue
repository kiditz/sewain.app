<script setup lang="ts">
import type { Product } from "~/types/product";
import type { StorefrontBlock, StorefrontProduct } from "~/types/storefront";
import BlockRenderer from "~/components/storefront/BlockRenderer.vue";
import { useStorefront } from "../../composables/useStorefront";

const props = defineProps<{
  settings: Record<string, unknown>;
  blocks: StorefrontBlock[];
  preview?: boolean;
  selectedBlockId?: string | null;
  product?: StorefrontProduct;
}>();

const { t } = useI18n();
const catalog = useStorefront();
const { regional, loadRegionalSettings } = useSettings();
await loadRegionalSettings().catch(() => undefined);

const page = ref(1);
const selectedTypes = ref<string[]>([]);
const minimumPrice = ref<number | null>(null);
const maximumPrice = ref<number | null>(null);
const sortBy = ref("title-ascending");

const columns = computed(() =>
  Math.max(1, Math.min(6, Number(props.settings.columns) || 3)),
);
const pageSize = computed(() =>
  Math.max(1, Math.min(100, Number(props.settings.productsPerPage) || 12)),
);
const showFilters = computed(() => props.settings.showFilters !== false);
const showSorting = computed(() => props.settings.showSorting !== false);
const showProductCount = computed(
  () => props.settings.showProductCount !== false,
);

const productTypes = computed(() => catalog.productTypes.value
  .map(type => ({ value: type.id, title: type.name }))
  .sort((left, right) => left.title.localeCompare(right.title, regional.value.locale)));
if (showFilters.value) await catalog.loadProductTypes().catch(() => undefined);
watch(showFilters, visible => { if (visible) void catalog.loadProductTypes().catch(() => undefined); });

const currencyPrefix = computed(() =>
  regional.value.currencyPosition === "before"
    ? regional.value.currency
    : undefined,
);
const currencySuffix = computed(() =>
  regional.value.currencyPosition === "after"
    ? regional.value.currency
    : undefined,
);
const pageCount = computed(() =>
  Math.max(1, Math.ceil(totalItems.value / pageSize.value)),
);
const rangeStart = computed(() =>
  totalItems.value ? (page.value - 1) * pageSize.value + 1 : 0,
);
const rangeEnd = computed(() =>
  Math.min(page.value * pageSize.value, totalItems.value),
);
const hasActiveFilters = computed(
  () =>
    selectedTypes.value.length > 0 ||
    minimumPrice.value !== null ||
    maximumPrice.value !== null,
);
const gridStyle = computed(() => ({
  "--product-grid-columns": String(columns.value),
}));
const sortOptions = computed(() => [
  { title: t("productGrid.sort.titleAscending"), value: "title-ascending" },
  { title: t("productGrid.sort.titleDescending"), value: "title-descending" },
  { title: t("productGrid.sort.priceAscending"), value: "price-ascending" },
  { title: t("productGrid.sort.priceDescending"), value: "price-descending" },
  { title: t("productGrid.sort.dateAscending"), value: "date-ascending" },
  { title: t("productGrid.sort.dateDescending"), value: "date-descending" },
]);

function clearFilters() {
  selectedTypes.value = [];
  minimumPrice.value = null;
  maximumPrice.value = null;
  page.value = 1;
}

watch([selectedTypes, minimumPrice, maximumPrice, sortBy], () => { page.value = 1; }, { deep: true });
const productQuery = computed(() => {
  const [sortKey, sortOrder] = sortBy.value.split("-");
  return {
    page: page.value - 1, size: pageSize.value, status: "ACTIVE" as const, available: true,
    "type.id": selectedTypes.value.length ? selectedTypes.value.join(",") : undefined,
    minimumRentalPrice: minimumPrice.value ?? undefined,
    maximumRentalPrice: maximumPrice.value ?? undefined,
    sortBy: sortKey === "title" ? "name" : sortKey === "price" ? "rentalPrice" : "createdAt",
    direction: sortOrder === "descending" ? "desc" as const : "asc" as const,
  };
});
const { data: productsPage, status: requestStatus } = await useAsyncData(
  computed(() => catalog.queryKey("grid", productQuery.value)),
  () => catalog.fetchProducts(productQuery.value),
  { default: () => ({ items: [] as Product[], totalItems: 0 }) },
);
const visibleProducts = computed(() => productsPage.value.items);
const totalItems = computed(() => productsPage.value.totalItems);
const loading = computed(() => requestStatus.value === "pending");
</script>

<template>
  <section class="product-grid-section">
    <div class="product-grid-section__inner">
      <BlockRenderer
        :blocks="blocks"
        :preview="preview"
        :selected-block-id="selectedBlockId"
        :product="product"
      />

      <div class="product-grid-toolbar">
        <div
          v-if="showProductCount"
          class="product-grid-toolbar__count"
          aria-live="polite"
        >
          {{
            t("productGrid.showing", {
              start: rangeStart,
              end: rangeEnd,
              count: totalItems,
            })
          }}
        </div>
        <v-select
          v-if="showSorting"
          v-model="sortBy"
          class="product-grid-toolbar__sort"
          :label="t('productGrid.sort.label')"
          :items="sortOptions"
          variant="outlined"
          density="compact"
          hide-details
        />
      </div>

      <div v-if="showFilters" class="product-grid-filters">
        <v-select
          v-model="selectedTypes"
          class="product-grid-filters__types"
          :label="t('productGrid.filters.productType')"
          :items="productTypes"
          :placeholder="t('productGrid.filters.allTypes')"
          multiple
          chips
          closable-chips
          clearable
          variant="outlined"
          density="compact"
          hide-details
        />
        <v-text-field
          v-model.number="minimumPrice"
          class="product-grid-filters__price"
          :label="t('productGrid.filters.minimumPrice')"
          type="number"
          min="0"
          :prefix="currencyPrefix"
          :suffix="currencySuffix"
          variant="outlined"
          density="compact"
          hide-details
        />
        <v-text-field
          v-model.number="maximumPrice"
          class="product-grid-filters__price"
          :label="t('productGrid.filters.maximumPrice')"
          type="number"
          min="0"
          :prefix="currencyPrefix"
          :suffix="currencySuffix"
          variant="outlined"
          density="compact"
          hide-details
        />
        <v-btn
          v-if="hasActiveFilters"
          variant="text"
          color="primary"
          class="product-grid-filters__clear"
          @click="clearFilters"
        >
          {{ t("productGrid.filters.clear") }}
        </v-btn>
      </div>

      <v-progress-linear v-if="loading" indeterminate color="primary" />
      <div
        v-if="visibleProducts.length"
        class="product-grid"
        :style="gridStyle"
      >
        <StorefrontProductCard
          v-for="item in visibleProducts"
          :key="item.id"
          :product="item"
        />
      </div>
      <p v-else-if="loading" class="product-grid-empty" />
      <p v-else-if="hasActiveFilters" class="product-grid-empty">
        {{ t("productGrid.noFilterResults") }}
      </p>
      <p v-else class="product-grid-empty">
        {{ t("productGrid.noProducts") }}
      </p>

      <v-pagination
        v-if="pageCount > 1"
        v-model="page"
        class="product-grid-pagination"
        :length="pageCount"
        :total-visible="5"
        rounded="circle"
        density="comfortable"
      />
    </div>
  </section>
</template>

<style scoped>
.product-grid-section {
  max-width: var(--store-width);
  padding: var(--store-spacing) 24px;
  margin: auto;
}
.product-grid-section__inner :deep(.block-heading) {
  font-size: clamp(30px, 4vw, 44px);
}
.product-grid-section__inner :deep(.block-text) {
  margin-top: -8px;
}
.product-grid-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin: 24px 0 16px;
}
.product-grid-toolbar__count {
  color: var(--store-text);
  opacity: 0.72;
}
.product-grid-toolbar__sort {
  flex: 0 1 250px;
}
.product-grid-filters {
  display: grid;
  grid-template-columns: minmax(220px, 2fr) repeat(2, minmax(140px, 1fr)) auto;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}
.product-grid {
  display: grid;
  grid-template-columns: repeat(var(--product-grid-columns, 3), minmax(0, 1fr));
  gap: 24px 18px;
}
.product-grid-empty {
  padding: 48px 0;
  color: var(--store-text);
  text-align: center;
  opacity: 0.7;
}
.product-grid-pagination {
  margin-top: 28px;
}
@media (max-width: 900px) {
  .product-grid {
    --product-grid-columns: 2 !important;
  }
  .product-grid-filters {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 560px) {
  .product-grid-section {
    padding-right: 16px;
    padding-left: 16px;
  }
  .product-grid-toolbar {
    align-items: stretch;
    flex-direction: column;
  }
  .product-grid-toolbar__sort {
    flex-basis: auto;
    width: 100%;
  }
  .product-grid-filters {
    grid-template-columns: 1fr;
  }
}
</style>
