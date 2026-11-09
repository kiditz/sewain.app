<script setup lang="ts">
import { usePublicCanonical } from "~/composables/usePublicCanonical";
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
if (import.meta.server && isMerchant && !homePage.value) {
  const event = useRequestEvent();
  if (event) setResponseStatus(event, 404, "Store unavailable");
}
const { t } = useI18n();
useSeoMeta({
  title: () => isMerchant && !homePage.value ? t("ui.store_unavailable") : undefined,
});
usePublicCanonical(() => !isMerchant || !!homePage.value);
</script>

<template>
  <StorefrontLayout v-if="isMerchant && homePage" :theme="theme">
    <StorefrontRenderer :sections="homePage.sections" />
  </StorefrontLayout>
  <LandingPage v-else-if="!isMerchant" />
  <main v-else class="store-unavailable">
    <p>404</p>
    <h1>{{ t("ui.store_unavailable") }}</h1>
    <p>{{ t("ui.store_unavailable_description") }}</p>
  </main>
</template>

<style scoped>
.store-unavailable {
  min-height: 60vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 40px 24px;
  text-align: center;
}
</style>
