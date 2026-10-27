<script setup lang="ts">
import { getBlockSettings } from "../../utils/block-settings";
import { useCartView } from "../../composables/useCartView";
import type { StorefrontBlock } from "~/types/storefront";

const props = defineProps<{
  block: StorefrontBlock;
  preview?: boolean;
}>();

const settings = computed(() =>
  getBlockSettings<{
    label?: string;
    url?: string;
    style?: string;
  }>(props.block),
);

const { isEmpty, loading, error } = useCartView(() => props.preview);

const visible = computed(() => !loading.value && !error.value && !isEmpty.value);
</script>

<template>
  <NuxtLink
    v-if="visible"
    :to="settings.url || '/checkout'"
    class="cart-checkout"
    :class="`cart-checkout--${settings.style || 'primary'}`"
  >
    {{ settings.label || "Proceed to checkout" }}
    <v-icon icon="mdi-arrow-right" size="18" />
  </NuxtLink>
</template>

<style scoped>
.cart-checkout {
  display: inline-flex;
  width: 100%;
  min-height: 48px;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 12px 18px;
  border: 1px solid var(--store-primary);
  border-radius: var(--store-radius);
  background: var(--store-primary);
  color: white;
  font-size: 13px;
  font-weight: 600;
  text-align: center;
  text-decoration: none;
  transition: opacity 160ms ease;
}

.cart-checkout:hover {
  opacity: 0.86;
}

.cart-checkout--secondary {
  border-color: var(--store-secondary);
  background: var(--store-secondary);
  color: var(--store-text);
}

.cart-checkout--outline {
  background: transparent;
  color: var(--store-primary);
}

@media (prefers-reduced-motion: reduce) {
  .cart-checkout {
    transition: none;
  }
}
</style>
