<script setup lang="ts">
import type { CollectionSummary, CollectionType } from "~/types/collection";

definePageMeta({
  layout: "merchant",
  middleware: "auth",
});

const { t } = useI18n();
useHead({ title: () => t("menu.collections") });
const { notifyCollectionError, watchCollectionErrors } = useCollectionError();

const { findCollections, deleteCollection: removeCollection } = useCollection();

const search = ref("");
const typeFilter = ref<"ALL" | CollectionType>("ALL");

const page = ref(1);
const itemsPerPage = ref(10);

const sortBy = ref<
  Array<{
    key: string;
    order?: "asc" | "desc";
  }>
>([
  {
    key: "name",
    order: "asc",
  },
]);

const deletingId = ref<string | null>(null);

const {
  data: collections,
  pending,
  error: loadError,
  refresh,
} = await useAsyncData<CollectionSummary[]>(
  "merchant-collections",
  () => findCollections(),
  {
    default: () => [],
  },
);

watchCollectionErrors([{ error: () => loadError.value, fallback: "collections.errors.loadFailed" }]);

const typeOptions = computed(() => [
  {
    title: t("collections.filter.all"),
    value: "ALL",
  },
  {
    title: t("collections.type.manual"),
    value: "MANUAL",
  },
  {
    title: t("collections.type.automated"),
    value: "AUTOMATED",
  },
]);

const headers = computed(() => [
  {
    title: t("collections.fields.title"),
    key: "name",
    sortable: true,
  },
  {
    title: t("collections.fields.type"),
    key: "type",
    sortable: true,
  },
  {
    title: t("collections.fields.products"),
    key: "productCount",
    sortable: true,
  },
  {
    title: "",
    key: "actions",
    sortable: false,
    align: "end" as const,
  },
]);

const filteredCollections = computed(() => {
  const keyword = search.value.trim().toLocaleLowerCase();

  return (collections.value ?? []).filter((collection) => {
    const matchesSearch =
      !keyword ||
      collection.name.toLocaleLowerCase().includes(keyword) ||
      collection.slug.toLocaleLowerCase().includes(keyword);

    const matchesType =
      typeFilter.value === "ALL" || collection.type === typeFilter.value;

    return matchesSearch && matchesType;
  });
});

const sortedCollections = computed(() => {
  const result = [...filteredCollections.value];

  // Ikuti sort yang dipilih pada v-data-table-server.
  for (const sort of [...sortBy.value].reverse()) {
    const { key, order } = sort;

    if (!order) continue;

    result.sort((a, b) => {
      const aValue = a[key as keyof CollectionSummary];
      const bValue = b[key as keyof CollectionSummary];

      let comparison: number;

      if (typeof aValue === "number" && typeof bValue === "number") {
        comparison = aValue - bValue;
      } else {
        comparison = String(aValue ?? "").localeCompare(
          String(bValue ?? ""),
          undefined,
          {
            numeric: true,
            sensitivity: "base",
          },
        );
      }

      return order === "desc" ? -comparison : comparison;
    });
  }

  return result;
});

const totalItems = computed(() => sortedCollections.value.length);

const pageItems = computed(() => {
  if (itemsPerPage.value <= 0) {
    return sortedCollections.value;
  }

  const start = (page.value - 1) * itemsPerPage.value;

  return sortedCollections.value.slice(start, start + itemsPerPage.value);
});

watch([search, typeFilter], () => {
  page.value = 1;
});

watch(totalItems, (total) => {
  const pageSize = itemsPerPage.value;

  if (pageSize <= 0) {
    page.value = 1;
    return;
  }

  const lastPage = Math.max(1, Math.ceil(total / pageSize));

  if (page.value > lastPage) {
    page.value = lastPage;
  }
});

const confirmDelete = async (collection: CollectionSummary) => {
  if (!confirm(t("collections.deleteConfirm"))) return;

  deletingId.value = collection.id;

  try {
    await removeCollection(collection.id);
    await refresh();
  } catch (error) {
    notifyCollectionError(error, "collections.errors.deleteFailed");
  } finally {
    deletingId.value = null;
  }
};
</script>

<template>
  <v-container fluid class="pa-4 pa-md-6">
    <div class="d-flex flex-wrap align-center justify-space-between ga-4 mb-6">
      <div>
        <div class="text-headline-small font-weight-bold">
          {{ t("collections.title") }}
        </div>

        <p class="text-body-medium text-medium-emphasis mt-1">
          {{ t("collections.subtitle") }}
        </p>
      </div>

      <v-btn
        color="primary"
        prepend-icon="mdi-plus"
        to="/merchant/collections/create"
      >
        {{ t("collections.actions.create") }}
      </v-btn>
    </div>

    <v-card rounded="lg" flat>
      <v-card-text>
        <v-row align="center">
          <v-col cols="12" md="7">
            <v-text-field
              density="compact"
              v-model="search"
              :placeholder="t('collections.search')"
              prepend-inner-icon="mdi-magnify"
              variant="outlined"
              hide-details
              clearable
            />
          </v-col>

          <v-col cols="12" md="5">
            <v-select
              density="compact"
              v-model="typeFilter"
              :items="typeOptions"
              :label="t('collections.fields.type')"
              variant="outlined"
              hide-details
            />
          </v-col>
        </v-row>
      </v-card-text>
      <v-divider />
      <v-data-table-server
        v-model:page="page"
        v-model:items-per-page="itemsPerPage"
        v-model:sort-by="sortBy"
        :headers="headers"
        :items="pageItems"
        :items-length="totalItems"
        :loading="pending || deletingId !== null"
        :items-per-page-options="[10, 25, 50, 100]"
        item-value="id"
      >
        <template #item.name="{ item }">
          <div class="d-flex align-center ga-3 py-2">
            <v-avatar size="44" rounded="lg" color="surface-variant">
              <v-img
                v-if="item.imageUrl"
                :src="item.imageUrl"
                :alt="item.name"
                cover
              />

              <v-icon icon="mdi-folder-outline" v-else> </v-icon>
            </v-avatar>

            <div class="min-width-0">
              <div class="font-weight-medium">
                {{ item.name }}
              </div>

              <div class="text-body-small text-medium-emphasis">
                {{ item.slug }}
              </div>

              <div
                v-if="item.description"
                class="text-body-small text-medium-emphasis"
              >
                {{ item.description }}
              </div>
            </div>
          </div>
        </template>

        <template #item.type="{ item }">
          <v-chip
            size="small"
            :color="item.type === 'AUTOMATED' ? 'primary' : undefined"
            variant="tonal"
          >
            {{ t(`collections.type.${item.type.toLowerCase()}`) }}
          </v-chip>
        </template>

        <template #item.productCount="{ item }">
          <span v-if="item.type === 'MANUAL'">
            {{ item.productCount }}
          </span>

          <span v-else class="text-medium-emphasis">
            {{ t("collections.automaticProducts") }}
          </span>
        </template>

        <template #item.actions="{ item }">
          <div class="d-flex justify-end ga-1">
            <v-btn
              icon="mdi-pencil-outline"
              variant="text"
              size="small"
              :aria-label="t('collections.actions.edit')"
              :to="{
                path: '/merchant/collections/create',
                query: { id: item.id },
              }"
            />

            <v-btn
              icon="mdi-delete-outline"
              variant="text"
              size="small"
              color="error"
              :aria-label="t('collections.actions.delete')"
              :loading="deletingId === item.id"
              :disabled="deletingId !== null"
              @click="confirmDelete(item)"
            />
          </div>
        </template>

        <template #no-data>
          <div class="py-10 text-center text-medium-emphasis">
            {{ t("collections.empty") }}
          </div>
        </template>
      </v-data-table-server>
    </v-card>
  </v-container>
</template>
