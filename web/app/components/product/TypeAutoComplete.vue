<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from "vue";
import type { ProductType } from "~/types/product-type";

const model = defineModel<string | null>({ default: null });

const props = withDefaults(
  defineProps<{
    label?: string;
    placeholder?: string;
    disabled?: boolean;
    hideDelete?: boolean;
    modelKey?: "id" | "name";
  }>(),
  { disabled: false, hideDelete: true, modelKey: "id" },
);
const { t } = useI18n();
const resolvedLabel = computed(() => props.label ?? t("product.fields.type"));
const resolvedPlaceholder = computed(() => props.placeholder ?? t("product.type.searchPlaceholder"));

const emit = defineEmits<{
  created: [productType: ProductType];
  deleted: [id: string];
}>();

const productTypeApi = useProductType();

const items = ref<ProductType[]>([]);
const search = ref("");
const loading = ref(false);
const creating = ref(false);
const deletingId = ref<string | null>(null);
const deleteDialog = ref(false);
const deleteTarget = ref<ProductType | null>(null);
const errorMessage = ref<string | null>(null);

let searchTimer: ReturnType<typeof setTimeout> | undefined;
let requestId = 0;
let mutationVersion = 0;
let disposed = false;

const normalize = (value: string) => value.trim().toLocaleLowerCase();

const getModelValue = (item: ProductType) =>
  props.modelKey === "name" ? item.name : item.id;

const canCreate = computed(() => {
  const keyword = normalize(search.value);

  if (!keyword || loading.value || creating.value || deletingId.value !== null || props.disabled) {
    return false;
  }

  return !items.value.some(
    (item) =>
      normalize(item.name) === keyword ||
      normalize(item.slug) === keyword,
  );
});

async function fetchItems(keyword = "") {
  const currentRequestId = ++requestId;

  loading.value = true;
  errorMessage.value = null;

  try {
    const result = await productTypeApi.find(
      keyword ? { search: keyword } : {},
    );

    if (disposed || currentRequestId !== requestId) return;

    // Pertahankan item yang sedang dipilih jika tidak ada dalam hasil pencarian.
    const selectedItem = items.value.find(
      (item) => getModelValue(item) === model.value,
    );

    items.value = [
      ...(selectedItem ? [selectedItem] : []),
      ...result.filter((item) => item.id !== selectedItem?.id),
    ];
  } catch {
    if (currentRequestId === requestId) {
      errorMessage.value = t("product.type.errors.loadFailed");
    }
  } finally {
    if (currentRequestId === requestId) {
      loading.value = false;
    }
  }
}

function scheduleSearch(keyword: string) {
  if (searchTimer) clearTimeout(searchTimer);

  searchTimer = setTimeout(() => {
    void fetchItems(keyword.trim());
  }, 250);
}

async function loadSelected(value: string | null) {
  if (!value) return;
  const version = mutationVersion;

  const existing = items.value.find(
    (item) => getModelValue(item) === value,
  );

  if (existing) {
    search.value = existing.name;
    return;
  }

  try {
    let item: ProductType | undefined;

    if (props.modelKey === "id") {
      item = await productTypeApi.findById(value);
    } else {
      const result = await productTypeApi.find({ search: value });

      item = result.find(
        (candidate) =>
          normalize(candidate.name) === normalize(value),
      );
    }

    if (!item || disposed || version !== mutationVersion || model.value !== value) return;

    if (!items.value.some((existingItem) => existingItem.id === item.id)) {
      items.value = [item, ...items.value];
    }

    search.value = item.name;
  } catch {
    if (!disposed && version === mutationVersion && model.value === value) {
      errorMessage.value = t("product.type.errors.loadSelectedFailed");
    }
  }
}

async function createItem() {
  const name = search.value.trim();

  if (!name || !canCreate.value) return;

  creating.value = true;
  errorMessage.value = null;

  try {
    const item = await productTypeApi.create({ name });

    items.value = [
      item,
      ...items.value.filter((existing) => existing.id !== item.id),
    ];

    model.value = getModelValue(item);
    search.value = item.name;

    emit("created", item);
  } catch {
    errorMessage.value = t("product.type.errors.createFailed");
  } finally {
    creating.value = false;
  }
}

function requestDelete(item: ProductType) {
  if (props.disabled || deletingId.value) return;
  deleteTarget.value = item;
  deleteDialog.value = true;
}

async function deleteItem() {
  const item = deleteTarget.value;
  if (!item || props.disabled || deletingId.value) return;
  ++requestId;
  ++mutationVersion;
  if (searchTimer) clearTimeout(searchTimer);
  loading.value = false;
  deletingId.value = item.id;
  errorMessage.value = null;

  try {
    await productTypeApi.remove(item.id);

    items.value = items.value.filter(
      (existing) => existing.id !== item.id,
    );

    if (model.value === getModelValue(item)) {
      model.value = null;
      search.value = "";
    }

    deleteDialog.value = false;
    deleteTarget.value = null;
    emit("deleted", item.id);
  } catch {
    errorMessage.value = t("product.type.errors.deleteFailed");
    deleteDialog.value = false;
  } finally {
    deletingId.value = null;
  }
}

watch(search, (value) => scheduleSearch(value));

watch(
  model,
  (value) => {
    void loadSelected(value);
  },
  { immediate: true },
);

watch(
  () => props.disabled,
  (disabled) => {
    if (disabled && searchTimer) {
      clearTimeout(searchTimer);
    }
  },
);

onBeforeUnmount(() => {
  disposed = true;
  ++requestId;
  ++mutationVersion;
  if (searchTimer) clearTimeout(searchTimer);
});

void fetchItems();
</script>

<template>
  <div class="product-type-autocomplete">
    <v-autocomplete
      v-model="model"
      v-model:search="search"
      :items="items"
      item-title="name"
      :item-value="props.modelKey"
      :label="resolvedLabel"
      :placeholder="resolvedPlaceholder"
      :loading="loading"
      :disabled="disabled || deletingId !== null"
      :error-messages="errorMessage ? [errorMessage] : []"
      no-filter
      clearable
      hide-details="auto"
      variant="outlined"
      density="compact"
    >
      <template v-if="!hideDelete" #item="{ props: itemProps, item }">
        <v-list-item
          v-bind="itemProps"
          :title="item.name"
          class="product-type-option"
        >
          <template #append>
            <v-btn
              icon="mdi-delete-outline"
              size="small"
              variant="text"
              color="error"
              :loading="deletingId === item.id"
              :disabled="disabled || deletingId !== null"
              :aria-label="t('product.type.deleteNamed', { name: item.name })"
              class="product-type-delete"
              @click.stop.prevent="requestDelete(item)"
            />
          </template>
        </v-list-item>
      </template>

      <template #no-data>
        <v-list-item>
          <div v-if="canCreate" class="d-flex justify-start mt-2">
            <v-btn
              prepend-icon="mdi-plus"
              variant="text"
              color="primary"
              size="small"
              :loading="creating"
              :disabled="disabled"
              @click="createItem"
            >
              {{ $t("ui.add") }}{{ search.trim() }}”
            </v-btn>
          </div>
        </v-list-item>
      </template>
    </v-autocomplete>
    <ConfirmDeleteDialog
      v-model="deleteDialog"
      :title="t('product.type.delete')"
      :message="t('product.type.deleteConfirm', { name: deleteTarget?.name })"
      @confirmed="deleteItem"
    />
  </div>
</template>
