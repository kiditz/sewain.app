<script setup lang="ts">
import { useStorefront } from "~/features/storefront-builder/composables/useStorefront";

/**
 * Halaman /collections menggunakan page dengan kind COLLECTIONS di builder.
 */
const storefront = useStorefront();
await callOnce(storefront.queryKey("published-configuration"), () => storefront.loadPublished(), { mode: "navigation" });
const { theme, getPageByKind } = storefront;
const page = computed(() => getPageByKind("COLLECTIONS"));

useSeoMeta({
  title: () => page.value?.title || "Collections",
});
</script>

<template>
  <StorefrontLayout :theme="theme">
    <StorefrontRenderer v-if="page" :sections="page.sections" />
  </StorefrontLayout>
</template>
