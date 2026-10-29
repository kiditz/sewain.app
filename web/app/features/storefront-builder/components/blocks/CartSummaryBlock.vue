<script setup lang="ts">
import { getBlockSettings, getLayoutStyle, getPaddingStyle } from "../../utils/block-settings";
import { useCartView } from "../../composables/useCartView";
import type { SectionLayout, StorefrontPadding, StorefrontBlock } from "~/types/storefront";

import { useStorefront } from "../../composables/useStorefront";
import { cartBlockDefaults } from "../../utils/cart-layout";
const props = defineProps<{
  block: StorefrontBlock;
  preview?: boolean;
}>();

const { theme } = useStorefront();
const blockStyle = computed(() => {
  const defaults = cartBlockDefaults(props.block.type);
  return {
    ...getLayoutStyle({ ...defaults.layout, ...props.block.settings.layout as Partial<SectionLayout> }, theme.value),
    ...getPaddingStyle({ ...defaults.padding, ...props.block.settings.padding as StorefrontPadding }, theme.value),
  };
});

const settings = computed(() =>
  getBlockSettings<{
    title?: string;
    rentalLabel?: string;
    depositLabel?: string;
    showDeposit?: boolean;
    totalLabel?: string;
    totalHint?: string;
    noteText?: string;
  }>(props.block),
);

const { isEmpty, loading, error, rentalSubtotal, depositSubtotal, totalAmount } =
  useCartView(() => props.preview);

// Disembunyikan saat cart kosong / gagal dimuat (kecuali di preview builder).
const visible = computed(() => !loading.value && !error.value && !isEmpty.value);
</script>

<template>
  <aside v-if="visible" class="cart-summary" :class="{ 'cart-summary--horizontal': block.settings.layout && (block.settings.layout as SectionLayout).direction === 'horizontal' }" :style="blockStyle">
    <h2 class="cart-summary__title">
      {{ settings.title || "Order summary" }}
    </h2>

    <div class="cart-summary__items">
      <div class="cart-summary__row">
        <span>{{ settings.rentalLabel || "Rental subtotal" }}</span>
        <strong>{{ money(rentalSubtotal) }}</strong>
      </div>

      <div v-if="settings.showDeposit !== false" class="cart-summary__row">
        <span>{{ settings.depositLabel || "Refundable deposit" }}</span>
        <strong>{{ money(depositSubtotal) }}</strong>
      </div>
    </div>

    <div class="cart-summary__divider" />

    <div class="cart-summary__total">
      <div>
        <strong>{{ settings.totalLabel || "Total" }}</strong>
        <p v-if="settings.totalHint">{{ settings.totalHint }}</p>
      </div>

      <strong class="cart-summary__amount">
        {{ money(totalAmount) }}
      </strong>
    </div>

    <div v-if="settings.noteText" class="cart-summary__note">
      <v-icon icon="mdi-information-outline" size="18" />
      <p>{{ settings.noteText }}</p>
    </div>
  </aside>
</template>

<style scoped>
.cart-summary {
  width: 100%;
  min-width: 0;
  box-sizing: border-box;
  flex-wrap: wrap;
  border: 1px solid color-mix(in srgb, var(--store-text) 14%, transparent);
  border-radius: var(--store-radius);
  background: var(--store-surface);
  color: var(--store-text);
}

.cart-summary > * { min-width: 0; max-width: 100%; }
.cart-summary--horizontal > :not(.cart-summary__divider) { flex: 1 1 180px; width: auto; }
.cart-summary--horizontal > .cart-summary__divider { flex-basis: 100%; }

.cart-summary__title {
  margin: 0;
  font-family: var(--store-heading-font), Georgia, serif;
  font-size: 19px;
  font-weight: 700;
}

.cart-summary__items {
  width: 100%;
  display: grid;
  gap: 18px;
}

.cart-summary__row {
  flex-wrap: wrap;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  font-size: 13px;
}

.cart-summary__row > span {
  opacity: 0.7;
}

.cart-summary__row > strong {
  font-weight: 600;
  text-align: right;
}

.cart-summary__divider {
  height: 1px;
  margin: 0;
  width: 100%;
  background: color-mix(in srgb, var(--store-text) 15%, transparent);
}

.cart-summary__total {
  width: 100%;
  flex-wrap: wrap;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.cart-summary__total > div > strong {
  font-size: 16px;
}

.cart-summary__total p {
  margin: 6px 0 0;
  font-size: 12px;
  opacity: 0.65;
}

.cart-summary__amount {
  color: var(--store-primary);
  font-size: 19px;
  text-align: right;
  overflow-wrap: anywhere;
}

.cart-summary__note {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin: 0;
  font-size: 12px;
  line-height: 1.6;
  opacity: 0.7;
}

.cart-summary__note p {
  margin: 0;
}

.cart-summary__note :deep(.v-icon) {
  flex-shrink: 0;
}

</style>
