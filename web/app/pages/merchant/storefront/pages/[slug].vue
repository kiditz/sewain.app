<script setup lang="ts">
import { useStorefront } from "~/features/storefront-builder/composables/useStorefront";
import PageBuilder from "~/features/storefront-builder/components/PageBuilder.vue";
definePageMeta({
  
  middleware: ["merchant"],
});
const route = useRoute();
const router = useRouter();
const store = useStorefront();
await store.load();
const page = computed(() =>
  store.getPageBySlugForEditing(String(route.params.slug)),
);
console.trace(page.value)
if (!page.value) await navigateTo("/merchant/storefront");
useHead({
  title: page.value?.title,
});
</script>
<template>
  <PageBuilder
    v-if="page"
    :page="page"
    @back="router.push('/merchant/storefront')"
    @saved="undefined"
  />
</template>
