<script setup lang="ts">
import { useStorefront } from "~/features/storefront-builder/composables/useStorefront";
import { useRecentlyViewedProducts } from "~/features/storefront-builder/composables/useRecentlyViewedProducts";

const route = useRoute();
const storefront = useStorefront();
await storefront.loadPublished();
const { theme, getPageByKind } = storefront;
const templatePage = computed(() => getPageByKind("PRODUCT"));

const slug = computed(() => String(route.params.slug));
const { findProductBySlug } = useProduct();

const { data: product } = await useAsyncData(
  () => `product-${slug.value}`,
  () => findProductBySlug(slug.value),
);

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
</script>

<template>
  <StorefrontLayout :theme="theme">
    <StorefrontRenderer v-if="templatePage" :sections="templatePage.sections" />
  </StorefrontLayout>
</template>
