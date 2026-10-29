<script setup lang="ts">
import BlockLabel from "./BlockLabel.vue";
import type { StorefrontBlock } from "~/types/storefront";
import { useCheckoutContext } from "~/features/storefront-checkout/composables/checkout-context";
import { useCheckoutBlockStyle } from "../../composables/useCheckoutBlockStyle";
const props = defineProps<{ block: StorefrontBlock; preview?: boolean }>();
const { t, submitting, cartLoading, hasCartItems, paymentReady } = useCheckoutContext();
const blockStyle = useCheckoutBlockStyle(() => props.block);
</script>
<template>
<button
            type="submit"
            class="checkout-button checkout-submit"
            :style="blockStyle"
            :disabled="preview || submitting || cartLoading || !hasCartItems || !paymentReady"
          >
            <v-progress-circular
              v-if="submitting"
              indeterminate
              size="18"
              width="2"
            />

            <template v-else>
              <BlockLabel :value="String(block.settings.label || t('checkout.placeOrder'))" />
              <v-icon icon="mdi-arrow-right" size="18" />
            </template>
          </button>
</template>
