<script setup lang="ts">
import type { StorefrontBlock } from "~/types/storefront";
import { useCheckoutContext } from "~/features/storefront-checkout/composables/checkout-context";
import { useCheckoutBlockStyle } from "../../composables/useCheckoutBlockStyle";
import CheckoutPaymentSection from "~/features/storefront-checkout/components/CheckoutPaymentSection.vue";
const props = defineProps<{ block: StorefrontBlock; preview?: boolean }>();
const { t, paymentOptions, paymentMethod, initialPaymentChoice, fulfillmentType, balanceDueAt, cartRentalSubtotal, cartDepositSubtotal, errors, submitError, loadPaymentOptions } = useCheckoutContext();
const blockStyle = useCheckoutBlockStyle(() => props.block);
</script>
<template>
<div class="checkout-payment-block"><CheckoutPaymentSection
            v-if="paymentOptions"
            :style="blockStyle"
            :title="String(block.settings.title || '')"
            v-model:payment-method="paymentMethod"
            v-model:initial-payment-choice="initialPaymentChoice"
            :fulfillment-type="fulfillmentType"
            v-model:balance-due-at="balanceDueAt"
            :options="paymentOptions"
            :rental-subtotal="cartRentalSubtotal"
            :deposit-subtotal="cartDepositSubtotal"
            :errors="{ paymentMethod: errors.paymentMethod, balanceDueAt: errors.balanceDueAt }"
          />

          <div v-if="submitError" class="checkout-error" role="alert">
            <v-icon icon="mdi-alert-circle-outline" size="20" />
            <span>{{ submitError }}</span>
            <button v-if="!paymentOptions" type="button" class="checkout-retry-link" @click="loadPaymentOptions">
              {{ t("checkout.retry") }}
            </button>
          </div>

          <p class="checkout-terms">
            {{ t("checkout.terms") }}
          </p></div>
</template>
