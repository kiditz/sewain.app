<script setup lang="ts">
const { t } = useI18n();
definePageMeta({
  layout: "merchant",
  middleware: ["merchant"]
});
useHead({
  title: t("product.edit.title"),
});
const route = useRoute();
const inventoryId = route.params.id as string;
const { findInventory } = useInventory();

const inventory = await findInventory(inventoryId);
</script>
<template>
  <v-container fluid>
    <div class="d-flex align-center ga-2 mb-6">
      <v-btn
        icon="mdi-arrow-left"
        variant="text"
        to="/merchant/inventory"
        :title="t('common.back')"
      />

      <div>
        <span class="text-headline-small font-weight-bold">
          {{ t("inventory.edit.title") }}
        </span>
        <p class="text-body-medium text-medium-emphasis mt-1">
          {{ t("inventory.edit.subtitle") }}
        </p>
      </div>
    </div>
    <inventory-form :unit="inventory"/>
  </v-container>
</template>
