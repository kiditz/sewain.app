<script setup lang="ts">
import type { Attribute } from "~/types/attribute";
import type { PageQuery } from "~/types/pagination";
import { EditOutlined, DeleteOutlined } from "@ant-design/icons-vue";

const { searchAttributes } = useAttribute();

const attributes = ref<Attribute[]>([]);
const totalItems = ref(0);
const loading = ref(false);

const page = ref(1);
const itemsPerPage = ref(10);
const search = ref("");

const emit = defineEmits<{
  edit: [attribute: Attribute];
  delete: [attribute: Attribute];
}>();

const sortBy = ref([
  {
    key: "name",
    order: "asc" as const,
  },
]);

const headers = [
  {
    title: "Name",
    key: "name",
    sortable: true,
  },
  {
    title: "Description",
    key: "description",
    sortable: false,
  },
  {
    title: "Values",
    key: "values",
    sortable: false,
  },
  {
    title: "Actions",
    key: "actions",
    sortable: false,
    align: "end" as const,
  },
];

const loadAttributes = async () => {
  loading.value = true;

  try {
    const sort = sortBy.value[0];

    const query: PageQuery = {
      page: page.value,
      size: itemsPerPage.value,
      sortBy: sort?.key,
      direction: sort?.order,
    };

    const response = await searchAttributes(query);

    attributes.value = response.items;
    totalItems.value = response.totalItems;
  } finally {
    loading.value = false;
  }
};

const attributeStore = useAttributeStore();

watch(
  () => attributeStore.version,
  () => {
    loadAttributes();
  },
);

watch(
  [page, itemsPerPage, search, sortBy],
  () => {
    loadAttributes();
  },
  {
    deep: true,
  },
);

await loadAttributes();
</script>

<template>
  <v-card
    variant="outlined"
    class="bg-surface rounded-0"
  >
    <v-card-item>
      <template #append>
        <v-text-field
          v-model="search"
          :label="$t('product.filters.search')"
          :placeholder="$t('ui.search_attribute')"
          variant="outlined"
          prepend-inner-icon="mdi-magnify"
          clearable
          hide-details
          width="300"
        />
      </template>
    </v-card-item>

    <v-divider />

    <v-card-text>
      <v-data-table-server
        v-model:page="page"
        v-model:items-per-page="itemsPerPage"
        v-model:sort-by="sortBy"
        :headers="headers"
        :items="attributes"
        :items-length="totalItems"
        :loading="loading"
        :items-per-page-options="[10, 25, 50]"
        item-value="id"
      >
        <template #item.description="{ item }">
          <span
            v-if="item.description"
            class="text-medium-emphasis"
          >
            {{ item.description }}
          </span>

          <span
            v-else
            class="text-medium-emphasis"
          >
            —
          </span>
        </template>

        <template #item.values="{ item }">
          <div class="d-flex flex-wrap ga-1 py-2">
            <v-chip
              v-for="value in item.values"
              :key="value.id"
              size="small"
              variant="tonal"
            >
              {{ value.value }}
            </v-chip>

            <span
              v-if="!item.values.length"
              class="text-medium-emphasis"
            >
              —
            </span>
          </div>
        </template>

        <template #item.actions="{ item }">
          <div class="d-flex justify-end ga-1">
            <v-btn
              icon
              size="small"
              variant="text"
              @click="emit('edit', item)"
            >
              <EditOutlined :style="{ fontSize: '16px' }" />
            </v-btn>

            <v-btn
              icon
              size="small"
              variant="text"
              color="error"
              @click="emit('delete', item)"
            >
              <DeleteOutlined :style="{ fontSize: '16px' }" />
            </v-btn>
          </div>
        </template>

        <template #no-data>
          <div class="py-8 text-center text-medium-emphasis">
            {{ $t("ui.no_attributes_found") }}
          </div>
        </template>
      </v-data-table-server>
    </v-card-text>
  </v-card>
</template>