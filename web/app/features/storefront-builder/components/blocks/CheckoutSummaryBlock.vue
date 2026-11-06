<script setup lang="ts">
const { formatDate } = useSettings();
import type { StorefrontBlock } from "~/types/storefront";
import { useCheckoutContext } from "~/features/storefront-checkout/composables/checkout-context";
import { useCheckoutBlockStyle } from "../../composables/useCheckoutBlockStyle";
const props = defineProps<{ block: StorefrontBlock; preview?: boolean }>();
const { t, cartItemCount, cartItems, cartRentalSubtotal, cartDepositSubtotal, cartTotalAmount } = useCheckoutContext();
const blockStyle = useCheckoutBlockStyle(() => props.block);
</script>
<template>
<aside class="checkout-summary" :style="blockStyle">
          <h2 class="checkout-summary__title">{{ block.settings.title || t("checkout.summaryTitle") }}</h2>

          <div class="checkout-summary__count">
            {{ t("checkout.cartItemsCount", { count: cartItemCount }) }}
          </div>

          <div class="checkout-summary__products">
            <article
              v-for="item in cartItems"
              :key="item.id"
              class="checkout-product"
            >
              <div class="checkout-product__image">
                <v-img
                  :src="item.mediaList?.at(0) || undefined"
                  :alt="item.productName"
                  aspect-ratio="0.8"
                  cover
                >
                  <template #error>
                    <div class="checkout-image-placeholder">
                      <v-icon icon="mdi-image-outline" size="24" />
                    </div>
                  </template>
                </v-img>
              </div>

              <div class="checkout-product__body">
                <NuxtLink
                  :to="`/products/${item.productSlug}`"
                  class="checkout-product__name"
                >
                  {{ item.productName }}
                </NuxtLink>

                <p class="checkout-product__variant">
                  {{ item.variantName || "Standard variant" }}
                  · {{ t("checkout.quantity", { count: item.quantity }) }}
                </p>

                <div class="checkout-product__period">
                  <v-icon icon="mdi-calendar-range" size="15" />
                  <span>
                    {{ formatDate(item.rentalStartAt) }}
                    – {{ formatDate(item.rentalEndAt) }}
                  </span>
                </div>

                <strong class="checkout-product__price">
                  {{ money(item.totalAmount) }}
                </strong>
              </div>
            </article>
          </div>

          <div class="checkout-summary__divider" />

          <div class="checkout-summary__rows">
            <div class="checkout-summary__row">
              <span>{{ t("checkout.rentalSubtotal") }}</span>
              <strong>{{ money(cartRentalSubtotal) }}</strong>
            </div>

            <div class="checkout-summary__row">
              <span>{{ t("checkout.deposit") }}</span>
              <strong>{{ money(cartDepositSubtotal) }}</strong>
            </div>
          </div>

          <div class="checkout-summary__divider" />

          <div class="checkout-summary__total">
            <div>
              <strong>{{ t("checkout.total") }}</strong>
              <p>{{ t("checkout.totalDescription") }}</p>
            </div>

            <strong class="checkout-summary__amount">
              {{ money(cartTotalAmount) }}
            </strong>
          </div>



          <div class="checkout-summary__note">
            <v-icon icon="mdi-shield-check-outline" size="19" />
            <p>
              {{ t("checkout.validationNote") }}
            </p>
          </div>
        </aside>
</template>
