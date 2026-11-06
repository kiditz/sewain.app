<script setup lang="ts">
import { useStorefront } from "~/features/storefront-builder/composables/useStorefront";
const route = useRoute();
const storefront = useStorefront();
await callOnce(storefront.queryKey("published-configuration"), () => storefront.loadPublished(), { mode: "navigation" });
const { getPageBySlug, theme } = storefront;
const page = computed(() => getPageBySlug(String(route.params.slug)));
useSeoMeta({ title: () => page.value?.title ?? "Page not found" });
useHead({
  title: page.value?.title,
});
</script>
<template>
  <StorefrontLayout :theme="theme">
    <StorefrontRenderer v-if="page" :sections="page.sections" />
    <div v-else class="not-found">
      <h1>{{ $t("ui.page_not_found") }}</h1>
      <p>{{ $t("ui.this_page_may_be_unpublished_or_no_longer_exists") }}</p>
      <NuxtLink to="/">{{ $t("ui.back_to_home") }}</NuxtLink>
    </div>
  </StorefrontLayout>
</template>
<style scoped>
.not-found {
  min-height: 55vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 12px;
  padding: 40px;
  text-align: center;
}
.not-found h1 {
  font-family: Georgia, serif;
  font-size: 42px;
}
.not-found a {
  color: var(--store-primary);
}
</style>
