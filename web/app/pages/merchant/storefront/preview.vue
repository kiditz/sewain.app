<script setup lang="ts">
import { useStorefront } from '~/features/storefront-builder/composables/useStorefront';
import StorefrontLayout from '~/components/storefront/StorefrontLayout.vue';
import StorefrontRenderer from '~/components/storefront/StorefrontRenderer.vue';
definePageMeta({
  key: route => route.fullPath, middleware: ['merchant'] });
const storefront = useStorefront();
storefront.pageStore.initialized.value = false;
storefront.themeStore.initialized.value = false;
storefront.menuStore.initialized.value = false;
await storefront.load();
const route = useRoute();
const pageId = ref(storefront.getPageByKind('HOME')?.id ?? storefront.pages.value[0]?.id);
const page = computed(() => storefront.pages.value.find(page => page.id === pageId.value));
</script>
<template>
  <div class="theme-preview">
    <div class="theme-preview__toolbar">
      <v-btn to="/merchant/storefront" variant="text" prepend-icon="mdi-arrow-left">{{ $t('themeLibrary.back') }}</v-btn>
      <span>{{ $t('themeLibrary.previewHint') }}</span>
      <v-select v-model="pageId" :items="storefront.pages.value" item-title="title" item-value="id" density="compact" hide-details variant="outlined" :aria-label="$t('themeLibrary.page')" style="max-width: 240px" />
      <v-btn v-if="page" :to="{ path: `/merchant/storefront/pages/${page.slug}`, query: route.query }" variant="tonal">{{ $t('themeLibrary.edit') }}</v-btn>
    </div>
    <div class="theme-preview__canvas">
      <StorefrontLayout v-if="page" :theme="storefront.theme.value" preview>
        <StorefrontRenderer :sections="page.sections" preview />
      </StorefrontLayout>
    </div>
  </div>
</template>
<style scoped>
.theme-preview { height: 100dvh; display: flex; flex-direction: column; }
.theme-preview__toolbar { display: flex; gap: 16px; align-items: center; flex-wrap: wrap; padding: 12px; border-bottom: 1px solid rgba(var(--v-theme-on-surface), .12); }
.theme-preview__toolbar > span { flex: 1; font-size: 13px; }
.theme-preview__canvas { flex: 1; min-height: 0; overflow: auto; }
</style>
