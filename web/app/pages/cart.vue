<script setup lang="ts">
import { useStorefront } from "~/features/storefront-builder/composables/useStorefront";

/**
 * Halaman /cart dirender dari page dengan kind CART di storefront builder
 * (section CART dengan block CART_ITEMS / CART_SUMMARY / CART_CHECKOUT),
 * Halaman ini hanya bertugas memuat konfigurasi storefront dan data cart;
 * seluruh tampilan & interaksinya ada di section/block.
 */
const cartStore = useCartStore();
const storefront = useStorefront();
await storefront.loadPublished().catch(() => undefined);
const { theme, getPageByKind } = storefront;
const page = computed(() => getPageByKind("CART"));

await useAsyncData("storefront-cart-data", async () => {
  await cartStore.getCart();
  return true;
});

useSeoMeta({
  title: () => page.value?.title || "Shopping Cart",
});
</script>

<template>
  <StorefrontLayout :theme="theme">
    <StorefrontRenderer v-if="page" :sections="page.sections" />
  </StorefrontLayout>
</template>
