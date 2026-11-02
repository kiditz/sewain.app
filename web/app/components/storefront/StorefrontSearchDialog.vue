<script setup lang="ts">
import type { Product } from "~/types/product";
import { useStorefrontCatalog } from "~/features/storefront-builder/composables/useStorefrontCatalog";
import { useRecentlyViewedProducts } from "~/features/storefront-builder/composables/useRecentlyViewedProducts";
import { storefrontDrawerTargetKey } from "~/features/storefront-builder/utils/storefront-branding";

const open = defineModel<boolean>({ default: false });
const props = defineProps<{ preview?: boolean }>();
const { t } = useI18n();
const catalog = useStorefrontCatalog();
const history = useRecentlyViewedProducts();
const dialogTarget = inject(storefrontDrawerTargetKey, "body");
const search = ref<string | null>("");
const searchTerm = computed(() => (search.value || "").trim());
const results = ref<Product[]>([]);
const recentProducts = ref<Product[]>([]);
const loading = ref(false);
const recentLoading = ref(false);
const failed = ref(false);
const recentFailed = ref(false);
const page = ref(0);
const total = ref(0);
const input = ref<{ focus: () => void }>();
const scrollbar = ref<{ $refs: { el?: { update: () => void } } }>();
const retryPage = ref(0);
let sequence = 0;
let recentSequence = 0;
let timer: ReturnType<typeof setTimeout> | undefined;

const groups = computed(() => [
  ...(searchTerm.value
    ? [
        {
          id: "results",
          title: t("storefrontSearch.results"),
          products: results.value,
          loading: loading.value,
          failed: failed.value,
        },
      ]
    : []),
  {
    id: "recent",
    title: t("storefrontSearch.recentlyViewed"),
    products: recentProducts.value,
    loading: recentLoading.value,
    failed: recentFailed.value,
  },
]);

async function fetchResults(nextPage = 0) {
  const name = searchTerm.value;
  if (!open.value || !name) return;
  if (timer) clearTimeout(timer);
  const request = ++sequence;
  retryPage.value = nextPage;
  loading.value = true;
  failed.value = false;
  try {
    const response = await catalog.fetchProducts({
      name,
      available: true,
      page: nextPage,
      size: 12,
    });
    if (request !== sequence || !open.value) return;
    results.value = nextPage
      ? [
          ...new Map(
            [...results.value, ...response.items].map((product) => [
              product.id,
              product,
            ]),
          ).values(),
        ]
      : response.items;
    page.value = nextPage;
    total.value = response.totalItems;
  } catch {
    if (request === sequence && open.value) failed.value = true;
  } finally {
    if (request === sequence) loading.value = false;
  }
}

async function fetchRecent() {
  const request = ++recentSequence;
  history.load();
  recentProducts.value = [];
  recentFailed.value = false;
  if (!history.ids.value.length) {
    recentLoading.value = false;
    return;
  }
  const ids = [...history.ids.value];
  recentLoading.value = true;
  try {
    const response = await catalog.fetchProducts({
      id: ids.join(","),
      available: true,
      page: 0,
      size: ids.length,
    });
    if (request !== recentSequence || !open.value) return;
    const byId = new Map(
      response.items.map((product) => [product.id, product]),
    );
    recentProducts.value = ids.flatMap((id) =>
      byId.has(id) ? [byId.get(id)!] : [],
    );
  } catch {
    if (request === recentSequence && open.value) recentFailed.value = true;
  } finally {
    if (request === recentSequence) recentLoading.value = false;
  }
}

watch(searchTerm, () => {
  ++sequence;
  if (timer) clearTimeout(timer);
  results.value = [];
  total.value = 0;
  page.value = 0;
  failed.value = false;
  loading.value = !!searchTerm.value && open.value;
  if (loading.value)
    timer = setTimeout(() => {
      void fetchResults();
    }, 300);
});
watch(open, (visible) => {
  if (visible) {
    void fetchRecent();
    if (searchTerm.value) void fetchResults();
  } else {
    ++sequence;
    ++recentSequence;
    if (timer) clearTimeout(timer);
    loading.value = false;
    recentLoading.value = false;
  }
});
async function refreshScrollbar() {
  await nextTick();
  scrollbar.value?.$refs.el?.update();
}
async function focusSearch() {
  input.value?.focus();
  await refreshScrollbar();
}
watch([results, recentProducts, loading, recentLoading, failed, recentFailed], refreshScrollbar, { flush: "post" });
onBeforeUnmount(() => {
  ++sequence;
  ++recentSequence;
  if (timer) clearTimeout(timer);
});

function imageFor(product: Product) {
  return (
    product.media ||
    product.variants?.find((variant) => variant.mediaList?.length)
      ?.mediaList[0] ||
    ""
  );
}
function selectProduct(event: MouseEvent) {
  if (props.preview) event.preventDefault();
  else open.value = false;
}
</script>

<template>
  <v-dialog
    v-model="open"
    max-width="900"
    :attach="preview ? dialogTarget : undefined"
    :contained="preview"
    :aria-label="t('storefrontSearch.title')"
    @after-enter="focusSearch"
  >
    <v-card class="storefront-search" rounded="lg">
      <div class="storefront-search__heading">
        <h2 class="text-title-large">{{ t("storefrontSearch.title") }}</h2>
        <v-btn
          icon="mdi-close"
          variant="text"
          :aria-label="t('storefrontSearch.close')"
          @click="open = false"
        />
      </div>
      <div class="px-5 pb-4">
        <v-text-field
          ref="input"
          v-model="search"
          :label="t('storefrontSearch.placeholder')"
          prepend-inner-icon="mdi-magnify"
          variant="outlined"
          clearable
          hide-details
          :loading="loading"
          @keydown.enter.prevent="fetchResults()"
        />
      </div>
      <v-divider />
      <nuxt-scrollbar
        ref="scrollbar"
        class="storefront-search__scroll"
        :options="{ suppressScrollX: true, wheelPropagation: false }"
      >
        <section
          v-for="group in groups"
          :key="group.id"
          class="storefront-search__group"
        >
          <h3 class="text-title-medium px-5 pt-4 pb-2">{{ group.title }}</h3>
          <v-progress-linear
            v-if="group.loading"
            indeterminate
            color="primary"
            :aria-label="group.title"
          />
          <div v-if="group.failed" class="px-5 py-3" role="status">
            <p>{{ t("storefrontSearch.loadFailed") }}</p>
            <v-btn
              variant="text"
              @click="
                group.id === 'recent' ? fetchRecent() : fetchResults(retryPage)
              "
              >{{ t("storefrontSearch.retry") }}</v-btn
            >
          </div>
          <p
            v-else-if="!group.loading && !group.products.length"
            class="px-5 py-3 text-body-medium"
            role="status"
          >
            {{
              t(
                group.id === "recent"
                  ? "storefrontSearch.noRecent"
                  : "storefrontSearch.noResults",
              )
            }}
          </p>
          <v-row v-if="group.products.length" class="px-3 pb-3" dense>
            <v-col
              v-for="product in group.products"
              :key="product.id"
              cols="6"
              sm="4"
              md="3"
            >
              <v-card
                :to="preview ? undefined : `/products/${product.slug}`"
                variant="outlined"

                class="storefront-search__product"
                @click="selectProduct"
              >
                <v-img
                  v-if="imageFor(product)"
                  :src="imageFor(product)"
                  :alt="product.name"
                  aspect-ratio="1"
                  cover
                  class="storefront-search__product-image"
                >
                  <template #placeholder>
                    <div class="d-flex align-center justify-center fill-height">
                      <v-progress-circular
                        indeterminate
                        size="20"
                        width="2"
                        color="primary"
                      />
                    </div>
                  </template>
                </v-img>
                <div v-else class="storefront-search__product-image storefront-search__no-image" aria-hidden="true" />

                <div class="pa-2">
                  <div class="text-body-medium storefront-search__product-name">
                    {{ product.name }}
                  </div>
                </div>
              </v-card>
            </v-col>
          </v-row>
          <div
            v-if="group.id === 'results' && results.length < total && !failed"
            class="px-5 pb-3"
          >
            <v-btn
              variant="text"
              :loading="loading"
              :disabled="loading"
              @click="fetchResults(page + 1)"
              >{{ t("ui.load_more") }}</v-btn
            >
          </div>
        </section>
      </nuxt-scrollbar>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.storefront-search {
  display: flex;
  flex-direction: column;
  max-height: min(85dvh, 800px);
  overflow: hidden;
  background: var(--store-surface, rgb(var(--v-theme-surface)));
  color: var(--store-text, rgb(var(--v-theme-on-surface)));
}


.storefront-search__heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 12px 8px 20px;
}
.storefront-search__scroll {
  position: relative;
  flex: 0 1 auto;
  min-height: 0;
  height: min(60dvh, 560px);
}
.storefront-search__group {
  padding-bottom: 12px;
}
.storefront-search__image {
  flex: none;
  border-radius: 6px;
  background: rgba(var(--v-theme-on-surface), 0.04);
}
.storefront-search__product {
  display: block;
  height: 100%;
  text-decoration: none;
  color: inherit;
  border-radius: 8px;
  overflow: hidden;
  transition: background-color 0.2s ease;
}

.storefront-search__product:hover {
  background: rgba(var(--v-theme-on-surface), 0.05);
}

.storefront-search__product-image {
  width: 100%;
  border-radius: 8px;
  background: var(--store-image-background, rgb(var(--v-theme-surface)));
}

.storefront-search__product-name {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  overflow: hidden;
  line-height: 1.4;
}
.storefront-search__no-image { aspect-ratio: 1; }
</style>
