<script setup lang="ts">
import type { StorefrontBlock } from "~/types/storefront";
import { useCheckoutContext } from "~/features/storefront-checkout/composables/checkout-context";
import { useCheckoutBlockStyle } from "../../composables/useCheckoutBlockStyle";
const props = defineProps<{ block: StorefrontBlock; preview?: boolean }>();
const { t, customerName, customerNameAttrs, email, emailAttrs, phone, phoneAttrs, errors } = useCheckoutContext();
const blockStyle = useCheckoutBlockStyle(() => props.block);
</script>
<template>
<section class="checkout-card" :style="blockStyle">
            <div class="checkout-section-heading">
              <div class="checkout-section-heading__icon">
                <v-icon icon="mdi-account-outline" size="21" />
              </div>

              <div>
                <h2>{{ block.settings.title || t("checkout.contactTitle") }}</h2>
                <p>{{ t("checkout.contactDescription") }}</p>
              </div>
            </div>
            <div class="checkout-fields">
              <v-text-field
                v-model="customerName"
                v-bind="customerNameAttrs"
                :label="t('checkout.fullName')"
                :placeholder="t('checkout.fullNamePlaceholder')"
                autocomplete="name"
                variant="outlined"
                :error-messages="errors.customerName"
                required
              />

              <v-text-field
                v-model="email"
                v-bind="emailAttrs"
                :label="t('checkout.email')"
                :placeholder="$t('ui.you_example_com')"
                type="email"
                autocomplete="email"
                variant="outlined"
                :error-messages="errors.email"
                required
              />

              <v-text-field
                v-model="phone"
                v-bind="phoneAttrs"
                :label="t('checkout.phone')"
                :placeholder="t('checkout.phonePlaceholder')"
                type="tel"
                autocomplete="tel"
                variant="outlined"
                :error-messages="errors.phone"
                required
              />
            </div>
          </section>
</template>
