<script setup lang="ts">
import type { Product } from "~/types/product";

const { t } = useI18n();
definePageMeta({
  layout: "merchant",
  middleware: ["merchant"]
});
useHead({ title: () => t("menu.products") });

const productToDelete = ref<Product | null>(null);
const showDeleteDialog = ref(false);
const { deleteProduct } = useProduct();
const store = useProductStore();
const snackbar = useSnackbarStore();
const { apiErrorMessage } = useApiError();
const openDeleteDialog = (product: Product) => {
  productToDelete.value = product;
  showDeleteDialog.value = true;
};
const confirmDelete = async () => {
  if (!productToDelete.value) {
    return;
  }

  try {
    await deleteProduct(productToDelete.value.id);

    showDeleteDialog.value = false;
    productToDelete.value = null;
    store.refresh();
    snackbar.openSnackbar({
      type: "success",
      message: "product.delete.success",
    });
  } catch (error: any) {
    snackbar.openSnackbar({
      type: "error",
      message: apiErrorMessage(error),
    });
  }
};
</script>
<template>
  <v-container fluid class="pa-4 pa-md-4">
    <ConfirmDeleteDialog
      v-model="showDeleteDialog"
      :title="$t('product.delete.confirm.title')"
      :message="$t('product.delete.confirm.message')"
      @confirmed="confirmDelete"
    />
    <div class="d-flex flex-wrap align-center justify-space-between ga-4 mb-6">
      <div>
        <span class="text-headline-small font-weight-bold">
          {{ t("product.title") }}
        </span>
        <p class="text-body-medium text-medium-emphasis mt-1">
          {{ t("product.subtitle") }}
        </p>
      </div>

      <div class="d-flex flex-wrap ga-2">
      <v-btn variant="outlined" prepend-icon="mdi-upload" to="/merchant/products/import">Import CSV</v-btn>
      <v-btn
        color="primary"
        prepend-icon="mdi-plus"
        to="/merchant/products/create"
      >
        {{ t("product.add.title") }}
      </v-btn>
      </div>
    </div>
    <product-table @delete="openDeleteDialog" />
  </v-container>
</template>
