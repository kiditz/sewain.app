<script setup lang="ts">
import BlockLabel from "./BlockLabel.vue";
import { storeToRefs } from "pinia";
import { getBlockSettings } from "../../utils/block-settings";
import type { StorefrontBlock } from "~/types/storefront";
import { useStorefrontProductStore } from "~/stores/storefront-product";

const props = defineProps<{
  block: StorefrontBlock;
}>();

const settings = computed(() =>
  getBlockSettings<{
    label?: string;
    min?: number;
    max?: number;
  }>(props.block),
);

const store = useStorefrontProductStore();
const { quantity, selectedVariant, availableQuantity } = storeToRefs(store);

const min = computed(() => Math.max(1, settings.value.min ?? 1));

const max = computed(() => availableQuantity.value);

function decrease() {
  if (quantity.value > min.value) {
    quantity.value--;
  }
}

function increase() {
  if (quantity.value < max.value) {
    quantity.value++;
  }
}

function updateQuantity(value: number) {
  if (!Number.isFinite(value)) return;

  quantity.value = Math.min(max.value, Math.max(min.value, Math.floor(value)));
}
</script>
<template>
  <div class="quantity-selector">
    <span class="quantity-label">
      <BlockLabel :value="settings.label || 'Quantity'" />
    </span>

    <div class="quantity-controls">
      <button
        type="button"
        class="quantity-button"
        :disabled="quantity <= min || !selectedVariant"
        :aria-label="$t('ui.decrease_quantity')"
        @click="decrease"
      >
        −
      </button>

      <input
        :value="quantity"
        type="number"
        :min="min"
        :max="max"
        :disabled="!selectedVariant"
        :aria-label="$t('orders.create.quantity')"
        @change="
          updateQuantity(Number(($event.target as HTMLInputElement).value))
        "
      />

      <button
        type="button"
        class="quantity-button"
        :disabled="quantity >= max || !selectedVariant"
        :aria-label="$t('ui.increase_quantity')"
        @click="increase"
      >
        +
      </button>
    </div>
  </div>
</template>

<style scoped>
.quantity-selector {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  color: var(--store-text);
}

.quantity-label {
  font-size: 13px;
  font-weight: 500;
}

.quantity-controls {
  display: flex;
  align-items: center;
  border: 1px solid var(--store-secondary);
  border-radius: var(--store-radius);
  overflow: hidden;
}

.quantity-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: 0;
  background: var(--store-surface);
  color: var(--store-text);
  font-size: 18px;
  cursor: pointer;
}

.quantity-button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.quantity-controls input {
  width: 44px;
  height: 36px;
  padding: 0;
  border: 0;
  outline: none;
  background: var(--store-surface);
  color: var(--store-text);
  text-align: center;
  font: inherit;
  appearance: textfield;
}

.quantity-controls input::-webkit-inner-spin-button,
.quantity-controls input::-webkit-outer-spin-button {
  margin: 0;
  appearance: none;
}
</style>
