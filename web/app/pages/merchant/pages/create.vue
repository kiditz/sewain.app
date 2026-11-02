<script setup lang="ts">
import { useStorefront } from "~/features/storefront-builder/composables/useStorefront";
import type { StorefrontPage } from "~/types/storefront";
definePageMeta({ layout: "merchant", middleware: ["merchant"] });
const storefront = useStorefront();
await storefront.pageStore.load();
const snackbar = useSnackbarStore();
const { t } = useI18n();
const { apiErrorMessage } = useApiError();
const title = ref("");
const slug = ref("");
const saving = ref(false);
const submitted = ref(false);
const created = ref<StorefrontPage | null>(null);
const normalizedSlug = computed(() => storefront.slugify(slug.value || title.value));
const titleError = computed(() => submitted.value && !title.value.trim() ? "Judul halaman wajib diisi." : "");
const slugError = computed(() => {
  if (!submitted.value) return "";
  if (!normalizedSlug.value) return "Slug halaman wajib diisi.";
  return storefront.pages.value.some(page => page.slug === normalizedSlug.value && page.id !== created.value?.id)
    ? "Slug sudah digunakan." : "";
});
async function create() {
  if (saving.value) return;
  submitted.value = true;
  if (titleError.value || slugError.value) return;
  saving.value = true;
  try {
    const page = created.value ?? storefront.createPage(title.value, normalizedSlug.value);
    created.value = page;
    page.title = title.value.trim();
    page.slug = normalizedSlug.value;
    page.targetUrl = storefront.targetUrlFor(page);
    await storefront.pageStore.savePage(page);
    snackbar.openSnackbar({ message: "Halaman dibuat sebagai draft.", type: "success" });
    await navigateTo(`/merchant/storefront/pages/${page.slug}`);
  } catch (error) {
    snackbar.openSnackbar({ message: t(apiErrorMessage(error)), type: "error" });
  } finally { saving.value = false; }
}
</script>
<template>
  <v-container class="py-8" style="max-width: 820px">
    <header class="d-flex align-center ga-3 mb-6"><v-btn icon="mdi-arrow-left" variant="text" to="/merchant/pages" :disabled="saving" aria-label="Kembali ke Pages" /><h1 class="text-headline-medium font-weight-bold">{{ $t('ui.create_page') }}</h1></header>
    <v-form @submit.prevent="create">
      <v-card variant="outlined" rounded="lg">
        <v-card-text class="d-flex flex-column ga-4">
          <v-text-field v-model="title" :label="$t('ui.page_title')" variant="outlined" :disabled="saving" :error-messages="titleError" autofocus />
          <v-text-field v-model="slug" label="Slug" :placeholder="normalizedSlug" variant="outlined" :disabled="saving" :error-messages="slugError" :hint="`/pages/${normalizedSlug || 'nama-halaman'}`" persistent-hint />
          <p class="text-body-small text-medium-emphasis">Halaman baru disimpan sebagai draft. Atur kontennya di editor, lalu publish theme melalui library storefront.</p>
        </v-card-text>
        <v-card-actions class="pa-4"><v-spacer /><v-btn variant="text" to="/merchant/pages" :disabled="saving">{{ $t('common.cancel') }}</v-btn><v-btn type="submit" color="primary" variant="flat" :loading="saving">{{ $t('ui.create_page') }}</v-btn></v-card-actions>
      </v-card>
    </v-form>
  </v-container>
</template>
