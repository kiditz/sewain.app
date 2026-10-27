<script setup lang="ts">
import { useStorefront } from "~/features/storefront-builder/composables/useStorefront";

const route = useRoute();
const storefront = useStorefront();
await storefront.loadPublished();
const { theme, getPageByKind } = storefront;
const templatePage = computed(() => getPageByKind("PRODUCT"));

var slug = String(route.params.slug);
const { findProductBySlug } = useProduct();

const { data: product } = await useAsyncData(
  () => `product-${slug}`,
  () => findProductBySlug(slug),
);

if (!product.value) {
  throw createError({ statusCode: 404, statusMessage: "Product not found" });
}
const store = useStorefrontProductStore();
store.setProduct(product.value);
useSeoMeta({
  title: () => product?.value?.name ?? "Product",
  description: () => product?.value?.description ?? "",
});
</script>

<template>
  <StorefrontLayout :theme="theme">
    <StorefrontRenderer v-if="templatePage" :sections="templatePage.sections" />
  </StorefrontLayout>
</template>
