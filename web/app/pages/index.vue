<script setup lang="ts">
import { useStorefront } from "~/features/storefront-builder/composables/useStorefront";

definePageMeta({ layout: "default" });

const { isMerchant } = useSiteContext();
const storefront = useStorefront();
const { theme } = storefront;
if (isMerchant) {
  await callOnce(storefront.queryKey("published-configuration"), () => storefront.loadPublished(), { mode: "navigation" });
}
const homePage = computed(() => {
  const page = storefront.getPageByKind("HOME");
  return page?.status === "PUBLISHED" ? page : null;
});
</script>

<template>
  <StorefrontLayout v-if="isMerchant && homePage" :theme="theme">
    <StorefrontRenderer :sections="homePage.sections" />
  </StorefrontLayout>
  <LandingPage v-else-if="!isMerchant" />
</template>
