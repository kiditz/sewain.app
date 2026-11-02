<script setup lang="ts">
import type {
  Product,
  ProductVariant,
  ProductVariantSelection,
} from "~/types/product";
import imagePlaceholder from "~/assets/images/placeholder.svg";

/**
 * Dialog untuk memilih satu atau beberapa varian produk.
 *
 * - Produk bisa dicari (nama / slug) dan setiap varian-nya ditampilkan.
 * - Yang bisa dipilih hanya varian, produk tidak.
 * - Varian nonaktif ditampilkan tetapi tidak bisa dipilih.
 */
const props = withDefaults(
  defineProps<{
    modelValue: boolean;
    multiple?: boolean;
    /** Varian yang sedang terpilih (ditandai saat dialog dibuka). */
    selectedVariantId?: string | null;
  }>(),
  {
    multiple: false,
    selectedVariantId: null,
  },
);

const emit = defineEmits<{
  "update:modelValue": [value: boolean];
  selected: [selection: ProductVariantSelection];
  selectedMultiple: [selections: ProductVariantSelection[]];
}>();

const { t } = useI18n();
const { findProducts } = useProduct();

const PAGE_SIZE = 6;

const keyword = ref<string | null>("");
const page = ref(1);
const totalItems = ref(0);
const products = ref<Product[]>([]);
const loading = ref(false);
const failed = ref(false);

const selections = ref<ProductVariantSelection[]>([]);

const activeVariantId = computed(
  () => selections.value[0]?.variant.id ?? props.selectedVariantId,
);
const selectedIds = computed(() => new Set(selections.value.map(item => item.variant.id)));
function isSelected(id: string) {
  return props.multiple ? selectedIds.value.has(id) : activeVariantId.value === id;
}

const pageCount = computed(() =>
  Math.max(1, Math.ceil(totalItems.value / PAGE_SIZE)),
);

let requestId = 0;
let searchTimer: ReturnType<typeof setTimeout> | undefined;

const loadProducts = async () => {
  const currentRequestId = ++requestId;

  loading.value = true;
  failed.value = false;

  try {
    const query: Record<string, unknown> = {
      page: page.value - 1,
      size: PAGE_SIZE,
      keyword: (keyword.value || "").trim() || undefined,
    };

    const response = await findProducts(query);

    // Abaikan response lama jika user sudah mengetik keyword baru.
    if (currentRequestId !== requestId) return;

    products.value = response.items;
    totalItems.value = response.totalItems;
  } catch {
    if (currentRequestId !== requestId) return;

    products.value = [];
    totalItems.value = 0;
    failed.value = true;
  } finally {
    if (currentRequestId === requestId) loading.value = false;
  }
};

watch(keyword, () => {
  if (searchTimer) clearTimeout(searchTimer);

  searchTimer = setTimeout(() => {
    page.value = 1;
    void loadProducts();
  }, 300);
});

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return;

    selections.value = [];
    keyword.value = "";
    page.value = 1;

    if (searchTimer) clearTimeout(searchTimer);

    void loadProducts();
  },
);

onBeforeUnmount(() => {
  if (searchTimer) clearTimeout(searchTimer);
});

const pick = (product: Product, variant: ProductVariant) => {
  if (!variant.active) return;

  if (!props.multiple) {
    selections.value = [{ product, variant }];
  } else if (selectedIds.value.has(variant.id)) {
    selections.value = selections.value.filter(item => item.variant.id !== variant.id);
  } else {
    selections.value = [...selections.value, { product, variant }];
  }
};

const close = () => {
  emit("update:modelValue", false);
};

const confirm = () => {
  if (!selections.value.length) return;

  if (props.multiple) emit("selectedMultiple", [...selections.value]);
  else emit("selected", selections.value[0]!);
  close();
};
</script>

<template>
  <v-dialog
    :model-value="modelValue"
    max-width="720"
    height="min(85dvh, 760px)"
    scrollable
    @update:model-value="emit('update:modelValue', $event)"
  >
    <v-card rounded="lg" class="variant-dialog-card">
      <v-card-title class="d-flex align-center justify-space-between pa-5">
        <div class="min-width-0">
          <div class="text-title-medium">
            {{ t("product.variantDialog.title") }}
          </div>
          <div class="text-body-small text-medium-emphasis text-wrap mt-1">
            {{ t(multiple ? "product.variantDialog.multipleSubtitle" : "product.variantDialog.subtitle") }}
          </div>
        </div>

        <v-btn
          icon="mdi-close"
          variant="text"
          :aria-label="t('common.cancel')"
          @click="close"
        />
      </v-card-title>

      <v-divider />

      <div class="pa-4 pb-2">
        <v-text-field
          v-model="keyword"
          :placeholder="t('product.variantDialog.searchPlaceholder')"
          prepend-inner-icon="mdi-magnify"
          variant="outlined"
          density="compact"
          hide-details
          clearable
          autofocus
        />
      </div>

      <v-card-text class="variant-dialog-body pa-4">
        <div v-if="loading" class="d-flex justify-center py-12">
          <v-progress-circular indeterminate />
        </div>

        <div v-else-if="failed" class="py-10 text-center text-medium-emphasis">
          <v-icon icon="mdi-alert-circle-outline" size="36" class="mb-2" />
          <div>{{ t("product.variantDialog.error") }}</div>
          <v-btn class="mt-3" variant="tonal" @click="loadProducts">
            {{ t("product.variantDialog.retry") }}
          </v-btn>
        </div>

        <div
          v-else-if="products.length === 0"
          class="py-10 text-center text-medium-emphasis"
        >
          <v-icon icon="mdi-package-variant" size="36" class="mb-2" />
          <div>{{ t("product.variantDialog.empty") }}</div>
        </div>

        <div v-else class="d-flex flex-column ga-3">
          <v-card
            v-for="product in products"
            :key="product.id"
            variant="outlined"
            rounded="lg"
          >
            <!-- Product (tidak bisa dipilih) -->
            <div class="d-flex align-center ga-3 pa-3 product-header">
              <v-avatar size="40" rounded>
                <v-img
                  :src="
                    (product.media
                      ? product.media
                      : product.variants[0]?.mediaList[0]) || imagePlaceholder
                  "
                  cover
                />
              </v-avatar>

              <div class="min-width-0 flex-grow-1">
                <div class="font-weight-medium text-truncate">
                  {{ product.name }}
                </div>
                <div class="text-body-small text-medium-emphasis text-truncate">
                  {{ product.slug }}
                </div>
              </div>

              <v-chip size="x-small" variant="tonal">
                {{
                  t("product.variants.count", {
                    count: product.variants?.length ?? 0,
                  })
                }}
              </v-chip>
            </div>

            <v-divider />

            <!-- Varian (bisa dipilih) -->
            <v-list
              v-if="product.variants?.length"
              density="comfortable"
              class="pa-2"
              rounded="lg"
              :role="multiple ? 'group' : 'radiogroup'"
              :aria-label="product.name"
            >
              <v-list-item
                v-for="variant in product.variants"
                :key="variant.id"
                :active="isSelected(variant.id)"
                :disabled="!variant.active"
                color="primary"
                class="pa-2"
                rounded="lg"
                :role="multiple ? 'checkbox' : 'radio'"
                :aria-checked="isSelected(variant.id)"
                :aria-label="`${product.name} · ${variant.name} (${variant.sku})`"
                @click="pick(product, variant)"
              >
                <template #prepend>
                  <v-icon
                    :icon="
                      multiple
                        ? isSelected(variant.id) ? 'mdi-checkbox-marked' : 'mdi-checkbox-blank-outline'
                        : isSelected(variant.id) ? 'mdi-radiobox-marked' : 'mdi-radiobox-blank'
                    "
                  />
                </template>

                <v-list-item-title class="font-weight-medium">
                  {{ variant.name }}
                </v-list-item-title>

                <v-list-item-subtitle>
                  {{ variant.sku }}
                </v-list-item-subtitle>

                <template #append>
                  <div class="d-flex flex-wrap justify-end ga-1">
                    <v-chip
                      v-for="item in variant.values ?? []"
                      :key="item.attributeValueId"
                      size="x-small"
                      variant="tonal"
                    >
                      {{ item.value }}
                    </v-chip>

                    <v-chip size="x-small" variant="outlined">
                      {{
                        t("product.form.unitsCount", {
                          count: variant.inventoryUnits?.length ?? 0,
                        })
                      }}
                    </v-chip>

                    <v-chip
                      v-if="!variant.active"
                      size="x-small"
                      color="warning"
                      variant="tonal"
                    >
                      {{ t("product.form.inactiveVariant") }}
                    </v-chip>
                  </div>
                </template>
              </v-list-item>
            </v-list>

            <div
              v-else
              class="pa-4 text-body-medium text-medium-emphasis text-center"
            >
              {{ t("product.variantDialog.noVariants") }}
            </div>
          </v-card>
        </div>
      </v-card-text>

      <div v-if="!loading && pageCount > 1" class="d-flex justify-center pb-2">
        <v-pagination
          v-model="page"
          :length="pageCount"
          :total-visible="5"
          density="comfortable"
          @update:model-value="loadProducts"
        />
      </div>

      <v-divider />

      <v-card-actions class="pa-4">
        <div
          v-if="selections.length"
          class="text-body-small text-medium-emphasis text-truncate"
        >
          {{ multiple
            ? t('product.variantDialog.selectedCount', { count: selections.length })
            : `${selections[0]!.product.name} · ${selections[0]!.variant.name}` }}
        </div>

        <v-spacer />

        <v-btn variant="text" @click="close">
          {{ t("common.cancel") }}
        </v-btn>

        <v-btn color="primary" :disabled="!selections.length" @click="confirm">
          {{ t("common.select") }}
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.variant-dialog-card {
  display: flex;
  flex-direction: column;
  max-height: 100%;
  min-height: 0;
  overflow: hidden;
}

.variant-dialog-card > :not(.variant-dialog-body) {
  flex-shrink: 0;
}

.variant-dialog-body {
  flex: 1 1 0;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
}

.product-header {
  background: rgba(var(--v-theme-on-surface), 0.02);
}

.min-width-0 {
  min-width: 0;
}
</style>
