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
const productId = route.params.id as string;
const { findProduct } = useProduct();

const product = await findProduct(productId);
</script>
<template>
  <v-container fluid>
    <div class="d-flex align-center ga-2 mb-6">
      <v-btn
        icon="mdi-arrow-left"
        variant="text"
        to="/merchant/products"
        :aria-label="t('product.add.back')"
      />

      <div>
        <span class="text-headline-small font-weight-bold">
          {{ t("product.edit.title") }}
        </span>
        <p class="text-body-medium text-medium-emphasis mt-1">
          {{ t("product.edit.subtitle") }}
        </p>
      </div>
    </div>
    <product-form :product="product"/>
  </v-container>
</template>
