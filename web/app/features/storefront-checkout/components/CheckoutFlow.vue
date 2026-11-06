<script setup lang="ts">
const { formatDate } = useSettings();
import CheckoutPaymentSection from "~/features/storefront-checkout/components/CheckoutPaymentSection.vue";
import { useCheckoutState } from "../composables/useCheckoutState";
import { checkoutContextKey } from "../composables/checkout-context";
import type { CSSProperties } from "vue";
const props = withDefaults(defineProps<{ showHeader?: boolean; preview?: boolean; embedded?: boolean; layoutStyle?: CSSProperties }>(), { showHeader: true });
const state = useCheckoutState(props.preview);
provide(checkoutContextKey, state);
const { cartStore, t, items, cartLoading, cartError, cartItems, cartItemCount, cartRentalSubtotal, cartDepositSubtotal, cartTotalAmount, customerName, customerNameAttrs, email, emailAttrs, phone, phoneAttrs, deliveryAddress, deliveryAddressAttrs, notes, notesAttrs, fulfillmentType, paymentMethod, initialPaymentChoice, balanceDueAt, errors, submitting, submitError, createdOrder, paymentOptions, loadPaymentOptions, hasCartItems, paymentReady, submitCheckout, load } = state;
await load();
</script>

<template>
    <div class="checkout-page" :class="{ 'checkout-page--embedded': embedded }">
      <header v-if="showHeader" class="checkout-header">
        <div>
          <p class="checkout-eyebrow">{{ t("checkout.eyebrow") }}</p>
          <h1 class="checkout-title">{{ t("checkout.title") }}</h1>
          <p class="checkout-description">
            {{ t("checkout.description") }}
          </p>
        </div>

        <NuxtLink to="/cart" class="checkout-back">
          <v-icon icon="mdi-arrow-left" size="18" />
          {{ t("checkout.backToCart") }}
        </NuxtLink>
      </header>

      <div v-if="createdOrder" class="checkout-state checkout-success">
        <v-icon icon="mdi-check-circle-outline" size="48" color="success" />
        <h2>{{ t("checkout.successTitle") }}</h2>
        <p>{{ t("checkout.orderNumber", { number: createdOrder.orderNumber }) }}</p>
        <p>{{ t("checkout.successDescription") }}</p>
        <div v-for="payment in createdOrder.payments" :key="payment.id" class="checkout-success-payment">
          <span>{{ payment.stage }} · {{ payment.status }}</span>
          <strong>{{ money(payment.amount) }}</strong>
        </div>
        <div v-if="paymentOptions?.instructions" class="checkout-success-instructions">
          {{ paymentOptions.instructions }}
        </div>
        <div v-if="paymentMethod === 'BANK_TRANSFER' && paymentOptions?.bankAccounts.length" class="checkout-success-accounts">
          <h3>{{ t("checkout.bankAccounts") }}</h3>
          <article v-for="(account, index) in paymentOptions.bankAccounts" :key="`${account.bankName}-${account.accountNumber}-${index}`">
            <strong>{{ account.bankName }}</strong>
            <span>{{ account.accountNumber }}</span>
            <small>{{ t("checkout.accountHolder", { name: account.accountHolder }) }}</small>
          </article>
        </div>
        <NuxtLink to="/" class="checkout-button">{{ t("checkout.continueShopping") }}</NuxtLink>
      </div>
      <div v-else-if="cartLoading" class="checkout-state">
        <v-progress-linear indeterminate color="primary" />
        <p>{{ t("checkout.loading") }}</p>
      </div>

      <div v-else-if="cartError" class="checkout-state">
        <v-icon icon="mdi-alert-circle-outline" size="40" />
        <h2>{{ t("checkout.cartLoadError") }}</h2>
        <p>{{ cartError }}</p>

        <button
          type="button"
          class="checkout-button"
          @click="cartStore.getCart()"
        >
          {{ t("checkout.retry") }}
        </button>
      </div>

      <div v-else-if="!items.length" class="checkout-empty">
        <div class="checkout-empty__icon">
          <v-icon icon="mdi-cart-outline" size="36" />
        </div>

        <h2>{{ t("checkout.emptyTitle") }}</h2>
        <p>{{ t("checkout.emptyDescription") }}</p>

        <NuxtLink to="/" class="checkout-button">
          {{ t("checkout.exploreProducts") }}
          <v-icon icon="mdi-arrow-right" size="18" />
        </NuxtLink>
      </div>

      <div v-else-if="!hasCartItems" class="checkout-empty">
        <div class="checkout-empty__icon">
          <v-icon icon="mdi-checkbox-multiple-blank-outline" size="36" />
        </div>

        <h2>{{ t("checkout.noSelectionTitle") }}</h2>
        <p>{{ t("checkout.noSelectionDescription") }}</p>

        <NuxtLink to="/cart" class="checkout-button">
          {{ t("checkout.backToCart") }}
          <v-icon icon="mdi-arrow-right" size="18" />
        </NuxtLink>
      </div>

      <form v-else class="checkout-layout" :style="layoutStyle" @submit.prevent="submitCheckout">
        <slot>
        <div class="checkout-main">
          <section class="checkout-card">
            <div class="checkout-section-heading">
              <div class="checkout-section-heading__icon">
                <v-icon icon="mdi-account-outline" size="21" />
              </div>

              <div>
                <h2>{{ t("checkout.contactTitle") }}</h2>
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

          <section class="checkout-card">
            <div class="checkout-section-heading">
              <div class="checkout-section-heading__icon">
                <v-icon icon="mdi-truck-outline" size="21" />
              </div>

              <div>
                <h2>{{ t("checkout.fulfillmentTitle") }}</h2>
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

          <section class="checkout-card">
            <div class="checkout-section-heading">
              <div class="checkout-section-heading__icon">
                <v-icon icon="mdi-note-text-outline" size="21" />
              </div>
              <div>
                <h2>{{ t("checkout.notesTitle") }}</h2>
                <p>{{ t("checkout.notesDescription") }}</p>
              </div>
            </div>
            <v-textarea
              v-model="notes"
              v-bind="notesAttrs"
              :label="t('checkout.notes')"
              :placeholder="t('checkout.notesPlaceholder')"
              variant="outlined"
              rows="2"
              auto-grow
              hide-details
            />
          </section>

          <CheckoutPaymentSection
            v-if="paymentOptions"
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
          </p>
        </div>

        <aside class="checkout-summary">
          <h2 class="checkout-summary__title">{{ t("checkout.summaryTitle") }}</h2>

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

          <button
            type="submit"
            class="checkout-button checkout-submit"
            :disabled="preview || submitting || cartLoading || !hasCartItems || !paymentReady"
          >
            <v-progress-circular
              v-if="submitting"
              indeterminate
              size="18"
              width="2"
            />

            <template v-else>
              {{ t("checkout.placeOrder") }}
              <v-icon icon="mdi-arrow-right" size="18" />
            </template>
          </button>

          <div class="checkout-summary__note">
            <v-icon icon="mdi-shield-check-outline" size="19" />
            <p>
              {{ t("checkout.validationNote") }}
            </p>
          </div>
        </aside>
        </slot>
      </form>
    </div>
</template>

<style>
.checkout-page {
  width: 100%;
  max-width: var(--store-width, 1200px);
  margin-inline: auto;
  padding: 16px;
  color: var(--store-text);
}

.checkout-page--embedded { max-width: none; padding: 0; }
.checkout-page .builder-block { min-width: 0; width: 100%; }
.checkout-page .checkout-summary { position: static; }
.checkout-payment-block { display: grid; min-width: 0; gap: 16px; }
.checkout-page .checkout-card > .v-input, .checkout-page .checkout-payment-fields, .checkout-page .checkout-fields, .checkout-page .checkout-summary__products, .checkout-page .checkout-summary__rows, .checkout-page .checkout-summary__divider, .checkout-page .checkout-summary__total { width: 100%; box-sizing: border-box; }
.checkout-page .checkout-summary > * { min-width: 0; max-width: 100%; }

.checkout-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 36px;
}

.checkout-eyebrow {
  margin: 0 0 10px;
  color: var(--store-primary);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.14em;
}

.checkout-title {
  margin: 0;
  color: var(--store-text);
  font-family: var(--store-heading-font), Georgia, serif;
  font-size: clamp(30px, 4vw, 42px);
  font-weight: 700;
  line-height: 1.2;
}

.checkout-description {
  margin: 12px 0 0;
  font-size: 14px;
  line-height: 1.7;
  opacity: 0.7;
}

.checkout-back {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  gap: 8px;
  color: var(--store-text);
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
}

.checkout-back:hover {
  color: var(--store-primary);
}

.checkout-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(290px, 360px);
  align-items: start;
  gap: 28px;
}

.checkout-main {
  display: grid;
  gap: 22px;
  min-width: 0;
}

.checkout-card,
.checkout-summary,
.checkout-empty,
.checkout-state {
  min-width: 0;
  border: 1px solid color-mix(in srgb, var(--store-text) 14%, transparent);
  border-radius: var(--store-radius);
  background: var(--store-surface);
}

.checkout-card {
  padding: 24px;
}

.checkout-section-heading {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 24px;
}

.checkout-section-heading__icon {
  display: flex;
  width: 42px;
  height: 42px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: calc(var(--store-radius) * 0.7);
  background: color-mix(in srgb, var(--store-primary) 10%, transparent);
  color: var(--store-primary);
}

.checkout-section-heading h2,
.checkout-summary__title {
  margin: 0;
  font-family: var(--store-heading-font), Georgia, serif;
  font-size: 19px;
  font-weight: 700;
}

.checkout-section-heading p {
  margin: 5px 0 0;
  font-size: 13px;
  line-height: 1.5;
  opacity: 0.65;
}

.checkout-fields {
  display: grid;
  gap: 6px;
}

.checkout-fields .v-field {
  border-radius: calc(var(--store-radius) * 0.7);
}

.checkout-summary {
  position: sticky;
  top: 24px;
  padding: 24px;
}

.checkout-summary__title {
  margin-bottom: 8px;
}

.checkout-summary__count {
  margin-bottom: 20px;
  font-size: 12px;
  opacity: 0.65;
}

.checkout-summary__products {
  display: grid;
  gap: 20px;
}

.checkout-product {
  display: grid;
  grid-template-columns: 76px minmax(0, 1fr);
  gap: 14px;
}

.checkout-product__image {
  overflow: hidden;
  align-self: start;
  border-radius: calc(var(--store-radius) * 0.7);
  background: var(--store-background);
}

.checkout-image-placeholder {
  display: flex;
  aspect-ratio: 0.8;
  align-items: center;
  justify-content: center;
  opacity: 0.45;
}

.checkout-product__body {
  display: flex;
  min-width: 0;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
}

.checkout-product__name {
  color: var(--store-text);
  font-size: 14px;
  font-weight: 700;
  line-height: 1.45;
  overflow-wrap: anywhere;
  text-decoration: none;
}

.checkout-product__name:hover {
  color: var(--store-primary);
}

.checkout-product__variant {
  margin: 0;
  font-size: 12px;
  opacity: 0.65;
}

.checkout-product__period {
  display: flex;
  align-items: flex-start;
  gap: 5px;
  font-size: 11px;
  line-height: 1.5;
  opacity: 0.75;
}

.checkout-product__period .v-icon {
  flex-shrink: 0;
  color: var(--store-primary);
}

.checkout-product__price {
  margin-top: 2px;
  font-size: 13px;
}

.checkout-summary__divider {
  height: 1px;
  margin: 22px 0;
  background: color-mix(in srgb, var(--store-text) 14%, transparent);
}

.checkout-summary__rows {
  display: grid;
  gap: 14px;
}

.checkout-summary__row {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  font-size: 13px;
}

.checkout-summary__row > span {
  opacity: 0.7;
}

.checkout-summary__row > strong {
  text-align: right;
  font-weight: 600;
}

.checkout-summary__total {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.checkout-summary__total > div > strong {
  font-size: 14px;
}

.checkout-summary__total p {
  margin: 5px 0 0;
  font-size: 12px;
  opacity: 0.65;
}

.checkout-summary__amount {
  font-size: 18px;
  text-align: right;
  overflow-wrap: anywhere;
}

.checkout-button {
  display: inline-flex;
  min-height: 46px;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 12px 18px;
  border: 1px solid var(--store-primary);
  border-radius: var(--store-radius);
  background: var(--store-primary);
  color: white;
  font-size: 13px;
  font-weight: 700;
  text-align: center;
  text-decoration: none;
  cursor: pointer;
  transition: opacity 0.2s ease;
}

.checkout-button:hover {
  opacity: 0.88;
}

.checkout-button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.checkout-submit {
  width: 100%;
  margin-top: 24px;
}

.checkout-summary__note {
  display: flex;
  align-items: flex-start;
  gap: 9px;
  margin-top: 18px;
  font-size: 12px;
  line-height: 1.6;
  opacity: 0.7;
}

.checkout-summary__note .v-icon {
  flex-shrink: 0;
  color: var(--store-primary);
}

.checkout-summary__note p {
  margin: 0;
}

.checkout-terms {
  margin: 0;
  font-size: 12px;
  line-height: 1.7;
  opacity: 0.65;
}

.checkout-error {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 14px 16px;
  border: 1px solid color-mix(in srgb, #c62828 30%, transparent);
  border-radius: var(--store-radius);
  color: #c62828;
  font-size: 13px;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.checkout-error .v-icon {
  flex-shrink: 0;
}

.checkout-retry-link {
  margin-left: auto;
  color: inherit;
  font: inherit;
  font-weight: 700;
  text-decoration: underline;
}

.checkout-empty,
.checkout-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 56px 24px;
  text-align: center;
}

.checkout-state {
  gap: 16px;
}

.checkout-state > .v-progress-linear {
  max-width: 240px;
}

.checkout-state p,
.checkout-empty p {
  max-width: 420px;
  margin: 8px 0 22px;
  font-size: 14px;
  line-height: 1.7;
  opacity: 0.7;
}

.checkout-empty h2,
.checkout-state h2 {
  margin: 20px 0 0;
  font-family: var(--store-heading-font), Georgia, serif;
  font-size: 23px;
}

.checkout-empty__icon {
  display: flex;
  width: 68px;
  height: 68px;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: color-mix(in srgb, var(--store-primary) 10%, transparent);
  color: var(--store-primary);
}

.checkout-success { display: grid; justify-items: center; gap: 12px; max-width: 620px; margin-inline: auto; padding: 36px 24px; text-align: center; }
.checkout-success > p { margin: 0; opacity: .7; }
.checkout-success-payment { display: flex; width: min(100%, 400px); justify-content: space-between; gap: 16px; padding: 12px 14px; border-radius: 10px; background: color-mix(in srgb, var(--store-primary) 8%, transparent); font-size: 13px; }
.checkout-success-instructions { max-width: 450px; font-size: 13px; line-height: 1.6; }
.checkout-success-accounts { display: grid; width: min(100%, 400px); gap: 8px; text-align: left; }
.checkout-success-accounts h3 { margin: 0; font-size: 14px; }
.checkout-success-accounts article { display: grid; gap: 3px; padding: 10px 12px; border: 1px solid color-mix(in srgb, var(--store-text) 14%, transparent); border-radius: 10px; font-size: 13px; }
.checkout-success-accounts small { opacity: .65; }

@media (max-width: 900px) {
  .checkout-layout {
    grid-template-columns: minmax(0, 1fr) minmax(270px, 320px);
    gap: 20px;
  }

  .checkout-card,
  .checkout-summary {
    padding: 20px;
  }
}

@media (max-width: 720px) {
  .checkout-header {
    align-items: flex-start;
    flex-direction: column;
    gap: 18px;
    margin-bottom: 24px;
  }

  .checkout-layout {
    grid-template-columns: minmax(0, 1fr);
  }

  .checkout-summary {
    position: static;
  }
}

@media (max-width: 480px) {
  .checkout-card,
  .checkout-summary {
    padding: 18px 16px;
  }

  .checkout-section-heading {
    align-items: flex-start;
  }

  .checkout-summary__amount {
    font-size: 16px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .checkout-button {
    transition: none;
  }
}
</style>
