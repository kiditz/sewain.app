<script setup lang="ts">
import { useStorefront } from "~/features/storefront-builder/composables/useStorefront";

const storefront = useStorefront();
await storefront.loadPublished();
const { theme, getPageByKind } = storefront;
const page = computed(() => getPageByKind("PRODUCT_GRID"));

useSeoMeta({
  title: () => page.value?.title || "Products",
});
</script>

<template>
  <StorefrontLayout :theme="theme">
    <StorefrontRenderer v-if="page" :sections="page.sections" />
  </StorefrontLayout>
</template>
