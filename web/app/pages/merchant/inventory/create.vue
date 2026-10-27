<script setup lang="ts">
const { t } = useI18n();
definePageMeta({
  layout: "merchant",
  middleware: ["merchant"]
});
useHead({
  title: t("inventory.add.title"),
});
</script>
<template>
  <v-container fluid>
    <div class="d-flex align-center ga-2 mb-6">
      <v-btn icon="mdi-arrow-left" variant="text" to="/merchant/inventory" />

      <div>
        <span class="text-headline-small font-weight-bold">
          {{ t("inventory.add.title") }}
        </span>
        <p class="text-body-medium text-medium-emphasis mt-1">
          {{ t("inventory.add.subtitle") }}
        </p>
      </div>
    </div>
    <inventory-form />
  </v-container>
</template>
