<script setup lang="ts">
import BlockLabel from "./BlockLabel.vue";
import { getBlockSettings } from "../../utils/block-settings";
import type { StorefrontBlock } from "~/types/storefront";
import type { CartItemCommand } from "~/types/cart";
import moment from "moment";

const props = defineProps<{
  block: StorefrontBlock;
  preview?: boolean;
}>();

const settings = computed(() =>
  getBlockSettings<{
    style?: string;
    label?: string;
  }>(props.block),
);

const store = useStorefrontProductStore();
const cartStore = useCartStore();
const {loading} = storeToRefs(cartStore)
const { canAddToCart } = storeToRefs(store);

const handleAddToCart = async () => {
  if (props.preview || !store.canAddToCart) return;
  const cart: CartItemCommand = {
    productId: store.product!.id,
    depositSubtotal: store.depositSubtotal,
    quantity: store.quantity,
    rentalStartAt: moment(store.startDate, "YYYY-MM-DD", "id")
      .utcOffset("+07:00")
      .startOf("day")
      .format("YYYY-MM-DDTHH:mm:ssZ"),
    rentalEndAt: moment(store.endDate, "YYYY-MM-DD", "id")
      .utcOffset("+07:00")
      .startOf("day")
      .format("YYYY-MM-DDTHH:mm:ssZ"),
    rentalPlans: store.rentalPlanCombination,

    rentalSubtotal: store.rentalSubtotal,
    totalAmount: store.total,
    variantId: store.selectedVariant!.id,
  };

  await cartStore.addItem(cart);
};
</script>

<template>
  <v-btn
    :disabled="preview || !canAddToCart"
    @click="handleAddToCart"
    :loading="loading"
    type="button"
    class="block-button cart-button"
    :class="`button-${settings.style || 'primary'}`"
  >
    <BlockLabel :value="settings.label || 'Sewa Sekarang'" />
    <span aria-hidden="true">↗</span>
  </v-btn>
</template>

<style scoped>
.block-button.v-btn { height: auto; min-height: 36px; white-space: normal; }
.block-button {
  align-self: flex-start;
  display: inline-flex;
  gap: 12px;
  align-items: center;
  padding: 13px 19px;
  min-height: auto;
  height: auto;
  border-radius: var(--store-radius);
  background: var(--store-primary);
  color: var(--store-on-primary, #fff);
  text-decoration: none;
  font-weight: 600;
}

.cart-button {
  align-self: stretch;
  justify-content: center;
  border: 0;
  cursor: pointer;
  font: inherit;
  font-weight: 700;
  min-height: 48px;
  height: auto;
  text-transform: none;
  letter-spacing: normal;
}
</style>
