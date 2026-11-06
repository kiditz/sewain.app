<script setup lang="ts">
import { useStorefront } from "~/features/storefront-builder/composables/useStorefront";
import type { StorefrontPage } from "~/types/storefront";

definePageMeta({ layout: "default" });

const { isMerchant, merchantSlug } = useSiteContext();
const storefront = useStorefront();
const { theme } = storefront;
const { data: homePage } = await useAsyncData<StorefrontPage | null>(
  `storefront-home-${merchantSlug ?? "landing"}`,
  async () => {
    if (!isMerchant) return null;

    try {
      await storefront.loadPublished();
    } catch {
      return null;
    }

    const page = storefront.getPageByKind("HOME");
    return page?.status === "PUBLISHED" ? page : null;
  },
  { default: () => null },
);
</script>

<template>
  <StorefrontLayout v-if="isMerchant && homePage" :theme="theme">
    <StorefrontRenderer :sections="homePage.sections" />
  </StorefrontLayout>
  <LandingPage v-else-if="!isMerchant" />
</template>
