<script setup lang="ts">
import type {
  BalanceDueAt,
  InitialPaymentChoice,
  OrderFulfillmentType as FulfillmentType,
  OrderPaymentMethod as PaymentMethod,
} from "~/types/order";

interface BankAccount {
  bankName: string;
  accountNumber: string;
  accountHolder: string;
}

interface PaymentOptions {
  bankTransferEnabled: boolean;
  cashAtPickupEnabled: boolean;
  cashAtReturnEnabled: boolean;
  downPaymentEnabled: boolean;
  downPaymentType: string;
  downPaymentValue: number;
  fullPaymentEnabled: boolean;
  separateDeposit: boolean;
  instructions: string;
  bankAccounts: BankAccount[];
}

const props = defineProps<{
  options: PaymentOptions;
  fulfillmentType: FulfillmentType;
  rentalSubtotal: number;
  depositSubtotal: number;
  errors?: Record<string, string | undefined>;
}>();

const { t } = useI18n();
const paymentMethod = defineModel<PaymentMethod>("paymentMethod", { required: true });
const initialPaymentChoice = defineModel<InitialPaymentChoice>("initialPaymentChoice", { required: true });
const balanceDueAt = defineModel<BalanceDueAt | null>("balanceDueAt", { default: null });

const cashAvailable = computed(() => {
  if (initialPaymentChoice.value === "DOWN_PAYMENT") {
    if (balanceDueAt.value === "PICKUP") return props.options.cashAtPickupEnabled;
    if (balanceDueAt.value === "RETURN") return props.options.cashAtReturnEnabled;
    return props.options.cashAtPickupEnabled || props.options.cashAtReturnEnabled;
  }
  return props.fulfillmentType === "PICKUP"
    ? props.options.cashAtPickupEnabled
    : props.options.cashAtReturnEnabled;
});

const paymentMethods = computed(() => {
  const methods: PaymentMethod[] = [];
  if (props.options.bankTransferEnabled) methods.push("BANK_TRANSFER");
  if (cashAvailable.value) methods.push("CASH");
  return methods;
});

watch(paymentMethods, (methods) => {
  if (!methods.includes(paymentMethod.value) && methods.length) paymentMethod.value = methods[0]!;
}, { immediate: true });

const amountDueNow = computed(() => {
  const base = props.rentalSubtotal + (props.options.separateDeposit ? 0 : props.depositSubtotal);
  if (initialPaymentChoice.value === "FULL_PAYMENT") return base;
  if (props.options.downPaymentType.toLowerCase() === "percentage") {
    return Math.round(base * props.options.downPaymentValue) / 100;
  }
  return Math.min(base, props.options.downPaymentValue);
});

const paymentLabel = (method: PaymentMethod) =>
  method === "BANK_TRANSFER" ? t("checkout.bankTransfer") : t("checkout.cash");
</script>

<template>
  <section class="checkout-card">
    <div class="checkout-section-heading">
      <div class="checkout-section-heading__icon">
        <v-icon icon="mdi-credit-card-outline" size="21" />
      </div>
      <div>
        <h2>{{ t("checkout.paymentTitle") }}</h2>
        <p>{{ t("checkout.paymentDescription") }}</p>
      </div>
    </div>

    <div class="checkout-payment-fields">
      <div>
        <p class="checkout-payment-label">{{ t("checkout.paymentPlan") }}</p>
        <v-radio-group v-model="initialPaymentChoice" hide-details>
          <v-radio
            v-if="options.fullPaymentEnabled"
            value="FULL_PAYMENT"
            :label="t('checkout.fullPayment')"
          />
          <v-radio
            v-if="options.downPaymentEnabled"
            value="DOWN_PAYMENT"
            :label="t('checkout.downPayment', { value: options.downPaymentValue, type: options.downPaymentType === 'percentage' ? '%' : '' })"
          />
        </v-radio-group>
      </div>

      <div v-if="initialPaymentChoice === 'DOWN_PAYMENT'">
        <p class="checkout-payment-label">{{ t("checkout.balanceDueAt") }}</p>
        <v-radio-group v-model="balanceDueAt" :error-messages="errors?.balanceDueAt" hide-details="auto">
          <v-radio value="PICKUP" :label="t('checkout.balanceAtPickup')" />
          <v-radio value="RETURN" :label="t('checkout.balanceAtReturn')" />
        </v-radio-group>
      </div>

      <div>
        <p class="checkout-payment-label">{{ t("checkout.paymentMethod") }}</p>
        <v-radio-group v-model="paymentMethod" :error-messages="errors?.paymentMethod" hide-details="auto">
          <v-radio v-for="method in paymentMethods" :key="method" :value="method" :label="paymentLabel(method)" />
        </v-radio-group>
        <p v-if="paymentMethods.length === 0" class="checkout-payment-error">
          {{ t("checkout.noPaymentMethods") }}
        </p>
      </div>

      <div class="checkout-payment-total">
        <span>{{ initialPaymentChoice === "FULL_PAYMENT" ? t("checkout.amountDue") : t("checkout.downPaymentAmount") }}</span>
        <strong>{{ money(amountDueNow) }}</strong>
      </div>
      <p v-if="options.separateDeposit" class="checkout-payment-hint">
        {{ t("checkout.depositCollectedSeparately", { amount: money(depositSubtotal) }) }}
      </p>

      <div v-if="paymentMethod === 'BANK_TRANSFER' && options.bankTransferEnabled" class="checkout-bank-accounts">
        <h3>{{ t("checkout.bankAccounts") }}</h3>
        <article v-for="(account, index) in options.bankAccounts" :key="`${account.bankName}-${account.accountNumber}-${index}`" class="checkout-bank-account">
          <strong>{{ account.bankName }}</strong>
          <span>{{ account.accountNumber }}</span>
          <small>{{ t("checkout.accountHolder", { name: account.accountHolder }) }}</small>
        </article>
        <p v-if="options.instructions" class="checkout-payment-hint">{{ options.instructions }}</p>
      </div>
    </div>
  </section>
</template>

<style scoped>
.checkout-payment-fields { display: grid; gap: 18px; }
.checkout-payment-label { margin: 0 0 4px; font-size: 13px; font-weight: 700; }
.checkout-payment-total { display: flex; justify-content: space-between; gap: 12px; padding: 14px 16px; border-radius: 12px; background: color-mix(in srgb, var(--store-primary) 8%, transparent); font-size: 14px; }
.checkout-payment-total strong { font-size: 16px; }
.checkout-payment-hint { margin: 0; font-size: 12px; line-height: 1.6; opacity: .7; }
.checkout-payment-error { margin: 0; color: #c62828; font-size: 12px; }
.checkout-bank-accounts { display: grid; gap: 10px; }
.checkout-bank-accounts h3 { margin: 0; font-size: 14px; }
.checkout-bank-account { display: grid; gap: 4px; padding: 12px 14px; border: 1px solid color-mix(in srgb, var(--store-text) 14%, transparent); border-radius: calc(var(--store-radius) * .7); font-size: 13px; }
.checkout-bank-account small { opacity: .65; }
.checkout-section-heading { display: flex; align-items: center; gap: 14px; margin-bottom: 24px; }
.checkout-section-heading__icon { display: flex; width: 42px; height: 42px; flex-shrink: 0; align-items: center; justify-content: center; border-radius: calc(var(--store-radius) * .7); background: color-mix(in srgb, var(--store-primary) 10%, transparent); color: var(--store-primary); }
.checkout-section-heading h2 { margin: 0; font-family: var(--store-heading-font), Georgia, serif; font-size: 19px; font-weight: 700; }
.checkout-section-heading p { margin: 5px 0 0; font-size: 13px; line-height: 1.5; opacity: .65; }
</style>
