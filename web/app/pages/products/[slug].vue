<script setup lang="ts">
import { usePublicCanonical } from "~/composables/usePublicCanonical";
import { useStorefront } from "~/features/storefront-builder/composables/useStorefront";
import { useRecentlyViewedProducts } from "~/features/storefront-builder/composables/useRecentlyViewedProducts";

const route = useRoute();
const storefront = useStorefront();
await callOnce(storefront.queryKey("published-configuration"), () => storefront.loadPublished(), { mode: "navigation" });
const { theme, getPageByKind } = storefront;
const templatePage = computed(() => getPageByKind("PRODUCT"));

const slug = computed(() => String(route.params.slug));
const { findProductBySlug } = useProduct();

const { data: product, error: productError } = await useAsyncData(
  () => storefront.queryKey("product-detail", { slug: slug.value }),
  () => findProductBySlug(slug.value),
);

if (productError.value) {
  throw storefrontRequestError(productError.value, "Product not found");
}
if (!product.value) {
  throw createError({ statusCode: 404, statusMessage: "Product not found" });
}
const store = useStorefrontProductStore();
const viewed = useRecentlyViewedProducts();
let mounted = false;
watch(product, (value) => {
  if (!value) return;
  store.setProduct(value);
  if (mounted && value.status === "ACTIVE") viewed.remember(value.id);
}, { immediate: true });
onMounted(() => {
  mounted = true;
  if (product.value?.status === "ACTIVE") viewed.remember(product.value.id);
});
useSeoMeta({
  title: () => product?.value?.name ?? "Product",
  description: () => product?.value?.description ?? "",
});
usePublicCanonical(() => !!product.value);
</script>

<template>
  <StorefrontLayout :theme="theme">
    <StorefrontRenderer v-if="templatePage" :sections="templatePage.sections" />
  </StorefrontLayout>
</template>
