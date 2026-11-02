<script setup lang="ts">
import { useStorefront } from "~/features/storefront-builder/composables/useStorefront";
import type { StorefrontPage } from "~/types/storefront";
definePageMeta({ layout: "merchant", middleware: ["merchant"] });
const storefront = useStorefront();
const { t } = useI18n();
const snackbar = useSnackbarStore();
const { apiErrorMessage } = useApiError();
const search = ref("");
const confirmDelete = ref(false);
const pendingDelete = ref<StorefrontPage | null>(null);
const deletingId = ref<string | null>(null);
const loadError = ref(false);
async function load() {
  loadError.value = false;
  try { await storefront.pageStore.load(); }
  catch { loadError.value = true; }
}
await load();
const pages = computed(() => storefront.pages.value.filter(page => page.kind === "CUSTOM"));
const headers = computed(() => [
  { title: t("ui.page_title"), key: "title" },
  { title: "Slug", key: "slug" },
  { title: "Status", key: "status" },
  { title: "", key: "actions", sortable: false, align: "end" as const },
]);
function requestDelete(page: StorefrontPage) {
  pendingDelete.value = page;
  confirmDelete.value = true;
}
async function remove() {
  const page = pendingDelete.value;
  if (!page || page.kind !== "CUSTOM" || deletingId.value) return;
  deletingId.value = page.id;
  try {
    await storefront.deletePage(page.id);
    confirmDelete.value = false;
    pendingDelete.value = null;
    snackbar.openSnackbar({ message: "Halaman dihapus.", type: "success" });
  } catch (error) {
    snackbar.openSnackbar({ message: t(apiErrorMessage(error)), type: "error" });
  } finally { deletingId.value = null; }
}
</script>
<template>
  <v-container class="py-8" style="max-width: 1400px">
    <header class="d-flex align-center justify-space-between flex-wrap ga-4 mb-6">
      <div><h1 class="text-headline-medium font-weight-bold">{{ $t('ui.pages') }}</h1><p class="text-body-medium text-medium-emphasis mt-2">{{ $t('ui.create_and_manage_the_pages_customers_see_in_your_store') }}</p></div>
      <v-btn color="primary" prepend-icon="mdi-plus" to="/merchant/pages/create">{{ $t('ui.create_page') }}</v-btn>
    </header>
    <v-card variant="outlined" rounded="lg">
      <v-card-text><v-text-field v-model="search" :label="$t('ui.search_pages')" prepend-inner-icon="mdi-magnify" variant="outlined" density="compact" hide-details clearable /></v-card-text>
      <v-divider />
      <div v-if="loadError" class="pa-5"><p class="mb-3">Halaman gagal dimuat.</p><v-btn variant="outlined" @click="load">Coba lagi</v-btn></div>
      <v-data-table v-else :headers="headers" :items="pages" :search="search || ''" :loading="storefront.pageStore.loading.value" :items-per-page="10" :no-data-text="$t('ui.no_pages_found')">
        <template #item.title="{ item }"><NuxtLink :to="`/merchant/storefront/pages/${item.slug}`" class="text-primary text-decoration-none">{{ item.title }}</NuxtLink></template>
        <template #item.status="{ item }"><v-chip size="small" :color="item.status === 'PUBLISHED' ? 'success' : 'default'">{{ item.status === 'PUBLISHED' ? $t('themeLibrary.pageReady') : $t('themeLibrary.draft') }}</v-chip></template>
        <template #item.actions="{ item }">
          <v-btn icon="mdi-pencil-outline" variant="text" size="small" :aria-label="`${$t('common.edit')} ${item.title}`" :to="`/merchant/storefront/pages/${item.slug}`" />
          <v-btn icon="mdi-trash-can-outline" variant="text" size="small" color="error" :aria-label="`${$t('ui.delete_page')} ${item.title}`" :loading="deletingId === item.id" :disabled="deletingId !== null" @click="requestDelete(item)" />
        </template>
      </v-data-table>
    </v-card>
    <ConfirmDeleteDialog v-model="confirmDelete" :title="$t('ui.delete_page')" :message="`Hapus halaman ${pendingDelete?.title || ''}? Link halaman ini juga akan dihapus dari menu toko.`" @confirmed="remove" />
  </v-container>
</template>
