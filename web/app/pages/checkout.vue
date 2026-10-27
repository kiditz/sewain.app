<script setup lang="ts">
import StorefrontLayout from "~/components/storefront/StorefrontLayout.vue";
import StorefrontRenderer from "~/components/storefront/StorefrontRenderer.vue";
import { useStorefront } from "~/features/storefront-builder/composables/useStorefront";


const storefront = useStorefront();
await storefront.loadPublished().catch(() => undefined);
const { theme, getPageByKind } = storefront;
const page = computed(() => getPageByKind("CHECKOUT"));

useSeoMeta({
  title: () => page.value?.title || "Checkout",
});
</script>

<template>
  <StorefrontLayout :theme="theme">
    <StorefrontRenderer v-if="page" :sections="page.sections" />
  </StorefrontLayout>
</template>
