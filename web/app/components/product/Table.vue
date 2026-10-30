<script setup lang="ts">
import type { Product, ProductStatus } from "~/types/product";
import { EditOutlined, DeleteOutlined } from "@ant-design/icons-vue";
import imagePlaceholder from "~/assets/images/placeholder.svg";
import type { DataTableHeader } from "vuetify";
const { loadRegionalSettings, formatMoney, formatDate } = useSettings();
await loadRegionalSettings();

const { findProducts } = useProduct();
const { t } = useI18n();
const store = useProductStore();
const snackbar = useSnackbarStore();
const { apiErrorMessage } = useApiError();

const products = ref<Product[]>([]);
const totalItems = ref(0);
const loading = ref(false);

const page = ref(1);
const size = ref(10);

const emit = defineEmits<{
  edit: [product: Product];
  delete: [product: Product];
}>();

const sortBy = ref([
  {
    key: "name",
    order: "asc" as const,
  },
  {
    key: "status",
    order: "asc" as const,
  },
]);

const headers = computed<DataTableHeader[]>(() => [
  {
    title: t("product.fields.name"),
    key: "name",
    sortable: true,
  },
  {
    title: t("product.fields.attributes"),
    key: "attributes",
    sortable: false,
  },
  {
    title: t("product.fields.type"),
    key: "type",
    sortable: false,
  },
  {
    title: t("product.fields.variants"),
    key: "variants",
    sortable: false,
  },
  {
    title: t("product.fields.buyPrice"),
    key: "buyPrice",
    sortable: false,
    align: "end",
  },
  {
    title: t("product.fields.rental"),
    key: "rentalPlans",
    sortable: false,
  },
  {
    title: t("product.fields.status"),
    key: "status",
    sortable: true,
  },
  {
    title: t("common.createdAt"),
    key: "createdAt",
    sortable: true,
  },
  {
    title: t("common.updatedAt"),
    key: "updatedAt",
    sortable: true,
  },
  {
    title: t("common.actions"),
    key: "actions",
    sortable: false,
    align: "end",
  },
]);

const filters = reactive({
  keyword: "",
  "type.id": "",
  tagIds: [] as string[],
  status: null as ProductStatus | null,
});

let requestSequence = 0;
const loadProducts = async () => {
  const request = ++requestSequence;
  loading.value = true;

  try {
    const query: Record<string, unknown> = {
      page: page.value - 1,
      sort: sortBy.value,
      size: size.value,
      keyword: filters.keyword?.trim() || undefined,
      status: filters.status || undefined,
      "type.id": filters["type.id"],
      "tags.id": filters.tagIds.length ? filters.tagIds.join(",") : undefined,
    };

    const response = await findProducts(query);

    if (request !== requestSequence) return;
    products.value = response.items;
    totalItems.value = response.totalItems;
  } catch (error) {
    if (request !== requestSequence) return;
    products.value = [];
    totalItems.value = 0;
    snackbar.openSnackbar({ type: "error", message: apiErrorMessage(error) });
  } finally {
    if (request === requestSequence) loading.value = false;
  }
};

const formatCurrency = (amount: number) => formatMoney(amount);

const getVariantPriceRange = (product: Product) => {
  const prices = product.variants
    .map((variant) => variant.buyPrice)
    .filter((price): price is number => typeof price === "number");

  if (!prices.length) return "—";

  const min = Math.min(...prices);
  const max = Math.max(...prices);

  return min === max
    ? formatCurrency(min)
    : `${formatCurrency(min)} – ${formatCurrency(max)}`;
};

const getActiveVariantCount = (product: Product) =>
  product.variants.filter((variant) => variant.active).length;

const getRentalPlans = (product: Product) =>
  product.variants
    .filter((variant) => variant.active)
    .flatMap((variant) =>
      (variant.rentalPlans ?? []).filter((plan) => plan.active),
    );

const getRentalPlanCount = (product: Product) => getRentalPlans(product).length;

const getRentalPriceRange = (product: Product) => {
  const fees = getRentalPlans(product).map((plan) => plan.rentalFee);

  if (!fees.length) return "—";

  const min = Math.min(...fees);
  const max = Math.max(...fees);

  return min === max
    ? formatCurrency(min)
    : `${formatCurrency(min)} – ${formatCurrency(max)}`;
};

const getAttributes = (product: Product) => {
  const attributes = new Map<string, Set<string>>();

  for (const variant of product.variants) {
    for (const item of variant.values ?? []) {
      if (!item.attributeName || !item.value) continue;

      if (!attributes.has(item.attributeName)) {
        attributes.set(item.attributeName, new Set());
      }

      attributes.get(item.attributeName)!.add(item.value);
    }
  }

  return Array.from(attributes, ([name, values]) => ({
    name,
    values: Array.from(values),
  }));
};

watch(
  filters,
  () => {
    if (page.value !== 1) {
      page.value = 1;
    } else {
      loadProducts();
    }
  },
  { deep: true },
);

watch([page, size, sortBy], loadProducts, { deep: true });

watch(() => store.version, loadProducts);

await loadProducts();
</script>

<template>
  <v-card rounded="lg" variant="outlined">
    <v-card-text>
      <v-row align="center">
        <v-col cols="12" md="4">
          <v-text-field
            v-model="filters.keyword"
            :label="t('product.filters.search')"
            :placeholder="t('product.filters.searchPlaceholder')"
            prepend-inner-icon="mdi-magnify"
            variant="outlined"
            density="compact"
            hide-details
            clearable
          />
        </v-col>
        <v-col cols="12" sm="6" md="2">
          <product-status v-model="filters.status" show-all />
        </v-col>
        <v-col cols="12" sm="6" md="2">
          <product-type-auto-complete v-model="filters['type.id']" />
        </v-col>
        <v-col cols="12" md="4">
          <product-tag-filter v-model="filters.tagIds" />
        </v-col>
      </v-row>
    </v-card-text>

    <v-divider />

    <v-data-table-server
      hover
      v-model:page="page"
      v-model:items-per-page="size"
      v-model:sort-by="sortBy"
      :headers="headers"
      :items="products"
      :items-length="totalItems"
      :loading="loading"
      :items-per-page-options="[10, 25, 50]"
      item-value="id"
    >
      <template #item.name="{ item }">
        <div class="d-flex align-center ga-3 py-2">
          <v-avatar size="44" rounded>
            <v-img
              :src="
                item.media
                  ? item.media
                  : item.variants[0]?.mediaList[0] || imagePlaceholder
              "
              cover
              @error="item.media = imagePlaceholder"
            />
          </v-avatar>

          <div class="min-width-0">
            <div class="font-weight-medium text-truncate">
              {{ item.name }}
            </div>
            <div class="text-body-small text-medium-emphasis">
              {{ item.slug }}
            </div>
          </div>
        </div>
      </template>
      <template #item.type="{ item }">
        <v-chip v-if="item.type" size="small" variant="tonal">
          {{ item.type.name }}
        </v-chip>
        <span v-else class="text-medium-emphasis">—</span>
      </template>
      <template #item.attributes="{ item }">
        <div
          v-if="getAttributes(item).length"
          class="d-flex flex-column ga-2 py-2"
        >
          <div
            v-for="attribute in getAttributes(item)"
            :key="attribute.name"
            class="d-flex justify-space-between align-center ga-2"
          >
            <span class="text-body-small text-medium-emphasis">
              {{ attribute.name }}
            </span>

            <div class="d-flex flex-wrap ga-1">
              <v-chip
                v-for="value in attribute.values"
                :key="value"
                size="x-small"
                variant="tonal"
              >
                {{ value }}
              </v-chip>
            </div>
          </div>
        </div>

        <span v-else class="text-medium-emphasis">—</span>
      </template>

      <template #item.variants="{ item }">
        <div class="d-flex flex-column">
          <span class="text-body-medium font-weight-medium">
            {{ t("product.variants.count", { count: item.variants.length }) }}
          </span>

          <span class="text-caption text-medium-emphasis">
            {{
              t("product.variants.active", {
                count: getActiveVariantCount(item),
              })
            }}
          </span>
        </div>
      </template>

      <template #item.buyPrice="{ item }">
        <span class="text-no-wrap">
          {{ getVariantPriceRange(item) }}
        </span>
      </template>

      <template #item.rentalPlans="{ item }">
        <div v-if="getRentalPlanCount(item)" class="py-2">
          <div class="text-no-wrap font-weight-medium">
            {{ getRentalPriceRange(item) }}
          </div>

          <div class="text-body-small text-medium-emphasis">
            {{
              t("product.rental.activePlans", {
                count: getRentalPlanCount(item),
              })
            }}
          </div>
        </div>

        <span v-else class="text-medium-emphasis">—</span>
      </template>

      <template #item.status="{ item }">
        <v-chip
          size="small"
          variant="tonal"
          :color="
            item.status === 'ACTIVE'
              ? 'success'
              : item.status === 'DRAFT'
                ? 'warning'
                : 'default'
          "
        >
          {{ t(`product.statusOptions.${item.status}`) }}
        </v-chip>
      </template>
      <template #item.createdAt="{ item }">
        <span class="text-body-small text-no-wrap">
          {{ formatDate(item.createdAt) }}
        </span>
      </template>

      <template #item.updatedAt="{ item }">
        <span class="text-body-small text-no-wrap">
          {{ formatDate(item.updatedAt) }}
        </span>
      </template>
      <template #item.actions="{ item }">
        <div class="d-flex justify-end ga-1">
          <v-btn
            icon
            size="small"
            variant="text"
            :to="`/merchant/products/${item.id}`"
            :aria-label="t('common.edit')"
          >
            <EditOutlined :style="{ fontSize: '16px' }" />
          </v-btn>

          <v-btn
            icon
            size="small"
            variant="text"
            color="error"
            :aria-label="t('common.remove')"
            @click="emit('delete', item)"
          >
            <DeleteOutlined :style="{ fontSize: '16px' }" />
          </v-btn>
        </div>
      </template>

      <template #no-data>
        <div class="py-8 text-center text-medium-emphasis">
          {{ t("product.empty") }}
        </div>
      </template>
    </v-data-table-server>
  </v-card>
</template>
