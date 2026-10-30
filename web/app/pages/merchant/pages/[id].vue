<script setup lang="ts">
import { useStorefront } from "~/features/storefront-builder/composables/useStorefront";
definePageMeta({ layout: "merchant", middleware: ["merchant"] });
const route = useRoute();
const storefront = useStorefront();
await storefront.pageStore.load();
const page = storefront.pages.value.find(page => page.id === String(route.params.id) && page.kind === "CUSTOM");
if (!page) throw createError({ statusCode: 404, statusMessage: "Page not found" });
await navigateTo(`/merchant/storefront/pages/${page.slug}`, { replace: true });
</script>
<template><div /></template>
