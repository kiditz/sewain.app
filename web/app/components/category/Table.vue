<script setup lang="ts">
import Sortable from "sortablejs";
import type { ComponentPublicInstance } from "vue";
import {
  DeleteOutlined,
  EditOutlined,
  HolderOutlined,
} from "@ant-design/icons-vue";

import type { Category } from "~/types/category";
import type { PageQuery } from "~/types/pagination";

const { searchCategories, reorderCategories } = useCategory();
const { t } = useI18n();

const categoryStore = useCategoryStore();

const tableRef = ref<ComponentPublicInstance | null>(null);

const categories = ref<Category[]>([]);
const totalItems = ref(0);
const loading = ref(false);
const reordering = ref(false);

const page = ref(1);
const itemsPerPage = ref(10);
const search = ref("");

const emit = defineEmits<{
  edit: [category: Category];
  delete: [category: Category];
}>();
const getParentName = (parentId: string | null) => {
  if (!parentId) {
    return null;
  }

  return categories.value.find((category) => category.id === parentId)?.name;
};
const headers = [
  {
    title: t("common.name"),
    key: "name",
    sortable: false,
  },
  {
    title: t("common.slug"),
    key: "slug",
    sortable: false,
  },
  {
    title: t("common.parent"),
    key: "parentName",
    sortable: false,
  },
  {
    title: t("common.actions"),
    key: "actions",
    sortable: false,
    align: "end" as const,
  },
];

const canReorder = computed(() => {
  return !search.value && !reordering.value;
});

const loadCategories = async () => {
  loading.value = true;

  try {
    const query: PageQuery = {
      page: page.value,
      itemsPerPage: itemsPerPage.value,
      search: search.value || undefined,
      sortBy: "sortOrder",
      sortDirection: "asc",
    };

    const response = await searchCategories(query);

    categories.value = response.items;
    totalItems.value = response.totalItems;
  } finally {
    loading.value = false;
  }
};

let sortable: Sortable | null = null;

const destroySortable = () => {
  sortable?.destroy();
  sortable = null;
};

const initSortable = async () => {
  destroySortable();

  if (!canReorder.value || !tableRef.value) {
    return;
  }

  await nextTick();

  const table = tableRef.value.$el as HTMLElement;
  const tbody = table.querySelector("tbody");

  if (!tbody) {
    return;
  }

  sortable = Sortable.create(tbody, {
    animation: 150,
    draggable: "tr",
    handle: ".drag-handle",
    onEnd: handleReorder,
  });
};

const handleReorder = async (event: Sortable.SortableEvent) => {
  console.log("handleReorder");

  const { oldIndex, newIndex } = event;

  if (oldIndex == null || newIndex == null || oldIndex === newIndex) {
    return;
  }

  const reordered = [...categories.value];

  const [moved] = reordered.splice(oldIndex, 1);

  if (!moved) {
    return;
  }

  reordered.splice(newIndex, 0, moved);

  categories.value = reordered;

  // hanya reorder sibling dengan parent yang sama
  const siblingIds = reordered
    .filter((category) => category.parentId === moved.parentId)
    .map(({ id }) => id);

  reordering.value = true;
  sortable?.option("disabled", true);

  try {
    await reorderCategories(siblingIds);
    await loadCategories();
    await categoryStore.refresh();
  } catch {
    await loadCategories();
  } finally {
    reordering.value = false;
    sortable?.option("disabled", false);
  }
};
watch(
  () => categoryStore.version,
  async () => {
    await loadCategories();
    await initSortable();
  },
);

watch([page, itemsPerPage, search], async () => {
  await loadCategories();
  await initSortable();
});

onMounted(async () => {
  await loadCategories();
  await initSortable();
});

onBeforeUnmount(destroySortable);
</script>

<template>
  <v-card variant="outlined" rounded="lg" class="bg-surface rounded-s-0 border-s-0">
    <v-card-item class="pa-0">
      <template #append>
        <v-text-field
          v-model="search"
          :label="$t('product.filters.search')"
          :placeholder="$t('ui.search_category')"
          variant="outlined"
          prepend-inner-icon="mdi-magnify"
          clearable
          hide-details
          class="mb-4"
          width="300"
        />
      </template>
    </v-card-item>

    <v-divider />

    <v-card-text>
      <v-data-table-server
        ref="tableRef"
        v-model:page="page"
        v-model:items-per-page="itemsPerPage"
        :headers="headers"
        :items="categories"
        :items-length="totalItems"
        :loading="loading || reordering"
        :items-per-page-options="[10, 25, 50]"
        item-value="id"
        disable-sort
      >
        <template #item.parentName="{ item }">
          <span v-if="getParentName(item.parentId)">
            {{ getParentName(item.parentId) }}
          </span>

          <span v-else class="text-medium-emphasis"> — </span>
        </template>

        <template #item.actions="{ item }">
          <div class="d-flex justify-end align-center ga-1">
            <v-btn
              icon
              size="small"
              variant="text"
              class="drag-handle"
              :disabled="!canReorder"
            >
              <HolderOutlined :style="{ fontSize: '16px' }" />
            </v-btn>

            <v-btn
              icon
              size="small"
              variant="text"
              :disabled="reordering"
              @click="emit('edit', item)"
            >
              <EditOutlined :style="{ fontSize: '16px' }" />
            </v-btn>

            <v-btn
              icon
              size="small"
              variant="text"
              color="error"
              :disabled="reordering"
              @click="emit('delete', item)"
            >
              <DeleteOutlined :style="{ fontSize: '16px' }" />
            </v-btn>
          </div>
        </template>

        <template #no-data>
          <div class="py-8 text-center text-medium-emphasis">
            {{ $t("ui.no_categories_found") }}
          </div>
        </template>
      </v-data-table-server>
    </v-card-text>
  </v-card>
</template>

<style scoped>
.drag-handle {
  cursor: grab;
}

.drag-handle:active {
  cursor: grabbing;
}
</style>
