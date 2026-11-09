<script setup lang="ts">
import { usePublicCanonical } from "~/composables/usePublicCanonical";
import { useStorefront } from "~/features/storefront-builder/composables/useStorefront";

const storefront = useStorefront();
await callOnce(storefront.queryKey("published-configuration"), () => storefront.loadPublished(), { mode: "navigation" });
const { theme, getPageByKind } = storefront;
const page = computed(() => getPageByKind("PRODUCT_GRID"));

useSeoMeta({
  title: () => page.value?.title || "Products",
});
usePublicCanonical();
</script>

<template>
  <StorefrontLayout :theme="theme">
    <StorefrontRenderer v-if="page" :sections="page.sections" />
  </StorefrontLayout>
</template>
