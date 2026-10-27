<script setup lang="ts">
import type { ProductStatus } from "~/types/product";
const props = withDefaults(
  defineProps<{
    showAll?: boolean;
  }>(),
  { showAll: false },
);
const model = defineModel<ProductStatus | null>({
  default: null,
});

const { t } = useI18n();

const items = computed<Array<{ value: ProductStatus | null; title: string }>>(
  () => {
    const statuses: Array<{ value: ProductStatus | null; title: string }> = [
      {
        value: "ACTIVE",
        title: t("product.statusOptions.ACTIVE"),
      },
      {
        value: "DRAFT",
        title: t("product.statusOptions.DRAFT"),
      },
      {
        value: "UNLISTED",
        title: t("product.statusOptions.UNLISTED"),
      },
    ];

    return props.showAll
      ? [{ value: null, title: t("common.all") }, ...statuses]
      : statuses;
  },
);
</script>

<template>
  <v-select
    v-model="model"

    :items="items"
    item-title="title"
    item-value="value"
    variant="outlined"
    density="compact"
    hide-details
  />
</template>
