<script setup lang="ts">
import type { StorefrontBlock } from "~/types/storefront";
import { useCheckoutContext } from "~/features/storefront-checkout/composables/checkout-context";
import { useCheckoutBlockStyle } from "../../composables/useCheckoutBlockStyle";
const props = defineProps<{ block: StorefrontBlock; preview?: boolean }>();
const { t, fulfillmentType, deliveryAddress, deliveryAddressAttrs, errors } = useCheckoutContext();
const blockStyle = useCheckoutBlockStyle(() => props.block);
</script>
<template>
<section class="checkout-card" :style="blockStyle">
            <div class="checkout-section-heading">
              <div class="checkout-section-heading__icon">
                <v-icon icon="mdi-truck-outline" size="21" />
              </div>

              <div>
                <h2>{{ block.settings.title || t("checkout.fulfillmentTitle") }}</h2>
                <p>{{ t("checkout.fulfillmentDescription") }}</p>
              </div>
            </div>

            <div class="checkout-fields">
              <v-radio-group v-model="fulfillmentType" inline hide-details>
                <v-radio value="PICKUP" :label="t('checkout.pickup')" />
                <v-radio value="DELIVERY" :label="t('checkout.delivery')" />
              </v-radio-group>
              <v-textarea
                v-if="fulfillmentType === 'DELIVERY'"
                v-model="deliveryAddress"
                v-bind="deliveryAddressAttrs"
                :label="t('checkout.deliveryAddress')"
                :placeholder="t('checkout.deliveryAddressPlaceholder')"
                autocomplete="street-address"
                variant="outlined"
                rows="3"
                auto-grow
                :error-messages="errors.deliveryAddress"
                :required="fulfillmentType === 'DELIVERY'"
              />

            </div>
          </section>
</template>
