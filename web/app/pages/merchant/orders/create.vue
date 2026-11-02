<script setup lang="ts">
import { toTypedSchema } from "@vee-validate/zod";
import { useFieldArray, useForm } from "vee-validate";
import { QuillEditor } from "@vueup/vue-quill";
import "@vueup/vue-quill/dist/vue-quill.snow.css";
import { z } from "zod";
import type { ProductVariantSelection, RentalPlan } from "~/types/product";
import {
  calculateRentalPlanCombination,
  getRentalDays,
} from "~/utils/rental-plan-calculator";
import type {
  BalanceDueAt,
  OrderForm,
  InitialPaymentChoice,
  MerchantOrderCommand,
  OrderFulfillmentType,
  OrderPaymentMethod,
} from "~/types/order";

definePageMeta({ layout: "merchant", middleware: ["merchant"] });

const { t } = useI18n();
const router = useRouter();
const { createOrder } = useOrder();
const { findByKey, loadRegionalSettings, formatMoney } = useSettings();
const snackbar = useSnackbarStore();
const { apiErrorMessage } = useApiError();

interface PaymentSettings {
  bankTransfer: boolean;
  cashAtPickup: boolean;
  cashAtReturn: boolean;
  downPaymentEnabled: boolean;
  downPaymentType: "percentage" | "fixed";
  downPaymentValue: number;
  fullPaymentEnabled: boolean;
  separateDeposit: boolean;
  instructions: string;
}

const paymentSettings = ref<PaymentSettings>({
  bankTransfer: false,
  cashAtPickup: false,
  cashAtReturn: false,
  downPaymentEnabled: false,
  downPaymentType: "percentage",
  downPaymentValue: 0,
  fullPaymentEnabled: false,
  separateDeposit: false,
  instructions: "",
});
const bankAccounts = ref<Record<string, unknown>[]>([]);
const settingsLoading = ref(true);
const settingsError = ref(false);

const orderSchema = toTypedSchema(
  z
    .object({
      customerName: z
        .string()
        .trim()
        .min(1, t("orders.validation.customerName"))
        .max(150, t("orders.validation.customerNameMax")),
      customerEmail: z
        .string()
        .trim()
        .email(t("orders.validation.customerEmail"))
        .max(255, t("orders.validation.customerEmailMax")),
      customerPhone: z.string().max(30).optional(),
      fulfillmentType: z.enum(["PICKUP", "DELIVERY"]),
      fulfillmentAt: z.string().min(1, t("orders.validation.fulfillmentAt")),
      address: z.string().optional(),
      rentalStartAt: z.string().min(1, t("orders.validation.rentalStartAt")),
      rentalEndAt: z.string().min(1, t("orders.validation.rentalEndAt")),
      paymentMethod: z.enum(["BANK_TRANSFER", "CASH"]),
      initialPaymentChoice: z.enum(["DOWN_PAYMENT", "FULL_PAYMENT"]),
      balanceDueAt: z.enum(["PICKUP", "RETURN"]).optional(),
      deliveryFee: z.number().min(0),
      depositWaived: z.boolean(),
      depositWaiverReason: z.string().max(200).optional(),
      notes: z.string().optional(),
      items: z
        .array(
          z.object({
            productId: z.string().min(1),
            variantId: z.string().min(1),
            productName: z.string(),
            variantName: z.string(),
            sku: z.string(),
            image: z.string().optional(),
            quantity: z.number().int().min(1),
            rentalUnitPrice: z.number().min(0),
            depositUnitAmount: z.number().min(0),
            variantRentalPlans: z.array(
              z.object({
                id: z.string(),
                durationUnit: z.enum(["HOUR", "DAY", "WEEK", "MONTH"]),
                durationValue: z.number(),
                rentalFee: z.number(),
                active: z.boolean(),
              }),
            ),
          }),
        )
        .min(1, t("orders.validation.items")),
    })
    .superRefine((form, ctx) => {
      if (form.rentalEndAt <= form.rentalStartAt) {
        ctx.addIssue({
          code: "custom",
          path: ["rentalEndAt"],
          message: t("orders.validation.dateRange"),
        });
      }
      const requestedDays = getRentalDays(form.rentalStartAt, form.rentalEndAt);
      if (
        requestedDays > 0 &&
        form.items.some(
          (item) =>
            calculateRentalPlanCombination(
              item.variantRentalPlans as RentalPlan[],
              requestedDays,
            ).length === 0,
        )
      ) {
        ctx.addIssue({
          code: "custom",
          path: ["items"],
          message: t("orders.create.noRentalPlan"),
        });
      }
      if (form.initialPaymentChoice === "DOWN_PAYMENT" && !form.balanceDueAt) {
        ctx.addIssue({
          code: "custom",
          path: ["balanceDueAt"],
          message: t("orders.validation.balanceDueAt"),
        });
      }
      if (form.fulfillmentType === "DELIVERY" && !form.address?.trim()) {
        ctx.addIssue({
          code: "custom",
          path: ["address"],
          message: t("orders.validation.address"),
        });
      }
      if (form.depositWaived && !form.depositWaiverReason?.trim()) {
        ctx.addIssue({
          code: "custom",
          path: ["depositWaiverReason"],
          message: t("orders.create.depositWaiverReasonRequired"),
        });
      }
    }),
);

const {
  defineField,
  errors,
  handleSubmit,
  isSubmitting,
  values,
  setFieldValue,
} = useForm({
  validationSchema: orderSchema,
  initialValues: {
    customerName: "",
    customerEmail: "",
    customerPhone: "",
    fulfillmentType: "PICKUP" as OrderFulfillmentType,
    fulfillmentAt: "",
    address: "",
    rentalStartAt: "",
    rentalEndAt: "",
    paymentMethod: "BANK_TRANSFER" as OrderPaymentMethod,
    initialPaymentChoice: "DOWN_PAYMENT" as InitialPaymentChoice,
    balanceDueAt: undefined as BalanceDueAt | undefined,
    deliveryFee: 0,
    depositWaived: false,
    depositWaiverReason: "",
    notes: "",
    items: [] as Array<{
      productId: string;
      variantId: string;
      productName: string;
      variantName: string;
      sku: string;
      image?: string;
      quantity: number;
      rentalUnitPrice: number;
      depositUnitAmount: number;
      variantRentalPlans: RentalPlan[];
    }>,
  },
});
const [customerName] = defineField("customerName");
const [customerEmail] = defineField("customerEmail");
const [customerPhone] = defineField("customerPhone");
const [fulfillmentType] = defineField("fulfillmentType");
const [fulfillmentAt] = defineField("fulfillmentAt");
const [address] = defineField("address");
const [rentalStartAt] = defineField("rentalStartAt");
const [rentalEndAt] = defineField("rentalEndAt");
const [paymentMethod] = defineField("paymentMethod");
const [initialPaymentChoice] = defineField("initialPaymentChoice");
const [balanceDueAt] = defineField("balanceDueAt");
const [deliveryFee] = defineField("deliveryFee");
const [depositWaived] = defineField("depositWaived");
const [depositWaiverReason] = defineField("depositWaiverReason");
const [notes] = defineField("notes");
const { fields: itemFields, push, remove, replace } = useFieldArray<OrderForm>("items");

watch(depositWaived, (waived) => {
  if (waived && !values.depositWaiverReason?.trim())
    setFieldValue(
      "depositWaiverReason",
      t("orders.create.idCardDepositWaiver"),
    );
  if (!waived) setFieldValue("depositWaiverReason", "");
});

const variantDialogOpen = ref(false);
const bankTransferAvailable = computed(
  () => paymentSettings.value.bankTransfer && bankAccounts.value.length > 0,
);
const paymentMethods = computed(() => [
  ...(bankTransferAvailable.value
    ? [{ title: t("orders.payment.bankTransfer"), value: "BANK_TRANSFER" }]
    : []),
  ...(paymentSettings.value.cashAtPickup || paymentSettings.value.cashAtReturn
    ? [{ title: t("orders.payment.cash"), value: "CASH" }]
    : []),
]);
const paymentChoices = computed(() => [
  ...(paymentSettings.value.downPaymentEnabled
    ? [{ title: t("orders.payment.downPayment"), value: "DOWN_PAYMENT" }]
    : []),
  ...(paymentSettings.value.fullPaymentEnabled
    ? [{ title: t("orders.payment.fullPayment"), value: "FULL_PAYMENT" }]
    : []),
]);

const rentalDays = computed(() =>
  getRentalDays(values.rentalStartAt, values.rentalEndAt),
);
const itemTotals = computed(
  () =>
    values.items?.map((item) => {
      const combination = calculateRentalPlanCombination(
        item.variantRentalPlans as RentalPlan[],
        rentalDays.value,
      );
      return {
        combination,
        rental:
          combination.reduce(
            (sum, selection) =>
              sum + selection.plan.rentalFee * selection.quantity,
            0,
          ) * item.quantity,
        deposit: item.depositUnitAmount * item.quantity,
      };
    }) ?? [],
);
const hasInvalidRentalPlanCombination = computed(
  () =>
    rentalDays.value > 0 &&
    itemTotals.value.some((item) => item.combination.length === 0),
);
const rentalSubtotal = computed(() =>
  itemTotals.value.reduce((sum, item) => sum + item.rental, 0),
);
const depositSubtotal = computed(() =>
  itemTotals.value.reduce((sum, item) => sum + item.deposit, 0),
);
const chargedDepositSubtotal = computed(() =>
  values.depositWaived ? 0 : depositSubtotal.value,
);
const totalAmount = computed(
  () =>
    rentalSubtotal.value +
    chargedDepositSubtotal.value +
    Number(values.deliveryFee || 0),
);
const initialAmount = computed(() => {
  const base =
    rentalSubtotal.value +
    (paymentSettings.value.separateDeposit ? 0 : chargedDepositSubtotal.value);
  if (values.initialPaymentChoice === "FULL_PAYMENT") return base;
  if (paymentSettings.value.downPaymentType === "fixed")
    return Math.min(base, paymentSettings.value.downPaymentValue);
  return Math.round((base * paymentSettings.value.downPaymentValue) / 100);
});

const addSelection = (selection: ProductVariantSelection) => {
  const activePlan = selection.variant.rentalPlans.find((plan) => plan.active);
  push({
    productId: selection.product.id,
    variantId: selection.variant.id,
    productName: selection.product.name,
    variantName: selection.variant.name,
    sku: selection.variant.sku,
    image: selection.variant.mediaList?.[0] || selection.product.media,
    quantity: 1,
    rentalUnitPrice: activePlan?.rentalFee ?? 0,
    depositUnitAmount: selection.variant.depositAmount ?? 0,
    variantRentalPlans: selection.variant.rentalPlans,
  });
};
const addSelections = (selections: ProductVariantSelection[]) => {
  for (const selection of selections) addSelection(selection);
};

const isoDate = (value: string) =>
  value ? new Date(value).toISOString() : undefined;
const isoRentalDate = (value: string) =>
  value ? new Date(`${value}T00:00:00`).toISOString() : undefined;
const submit = handleSubmit(async (form) => {
  const startAt = isoRentalDate(form.rentalStartAt)!;
  const endAt = isoRentalDate(form.rentalEndAt)!;
  const fulfillmentAtIso = isoDate(form.fulfillmentAt)!;
  const command: MerchantOrderCommand = {
    customerName: form.customerName,
    customerEmail: form.customerEmail,
    customerPhone: form.customerPhone || undefined,
    fulfillmentType: form.fulfillmentType,
    fulfillmentAddress:
      form.fulfillmentType === "DELIVERY"
        ? { address: form.address }
        : undefined,
    pickupAt: form.fulfillmentType === "PICKUP" ? fulfillmentAtIso : undefined,
    deliveryAt:
      form.fulfillmentType === "DELIVERY" ? fulfillmentAtIso : undefined,
    returnAt: endAt,
    paymentMethod: form.paymentMethod,
    initialPaymentChoice: form.initialPaymentChoice,
    balanceDueAt:
      form.initialPaymentChoice === "DOWN_PAYMENT"
        ? form.balanceDueAt
        : undefined,
    deliveryFee: Number(form.deliveryFee || 0),
    depositWaived: form.depositWaived,
    depositWaiverReason: form.depositWaived
      ? form.depositWaiverReason?.trim()
      : undefined,
    notes: form.notes || undefined,
    items: form.items.map((item) => ({
      productId: item.productId,
      variantId: item.variantId,
      quantity: Number(item.quantity),
      rentalStartAt: startAt,
      rentalEndAt: endAt,
    })),
  };
  try {
    const order = await createOrder(command);
    snackbar.openSnackbar({
      type: "success",
      message: t("orders.create.success"),
    });
    await router.push(`/merchant/orders/${order.id}`);
  } catch (error: any) {
    snackbar.openSnackbar({
      type: "error",
      message: apiErrorMessage(error),
    });
  }
});

onMounted(async () => {
  settingsLoading.value = true;
  try {
    await loadRegionalSettings();
    const [payment, accounts] = await Promise.all([
      findByKey("PAYMENT"),
      findByKey("PAYMENT_BANK_ACCOUNTS"),
    ]);
    paymentSettings.value = {
      ...paymentSettings.value,
      ...(payment.value as Partial<PaymentSettings>),
    };
    const accountValue = accounts.value as {
      accounts?: Record<string, unknown>[];
    } | null;
    bankAccounts.value = accountValue?.accounts ?? [];
    if (!bankTransferAvailable.value && paymentMethods.value.length)
      setFieldValue(
        "paymentMethod",
        paymentMethods.value[0]!.value as OrderPaymentMethod,
      );
    if (paymentChoices.value.length)
      setFieldValue(
        "initialPaymentChoice",
        paymentChoices.value[0]!.value as InitialPaymentChoice,
      );
    settingsError.value = false;
  } catch {
    settingsError.value = true;
  } finally {
    settingsLoading.value = false;
  }
});

const formatCurrency = (amount: number) => formatMoney(amount);
</script>

<template>
  <v-container fluid class="pa-4 pa-md-6">
    <div class="d-flex align-center ga-3 mb-6">
      <v-btn
        icon="mdi-arrow-left"
        variant="text"
        to="/merchant/orders"
        :aria-label="t('orders.create.back')"
      />
      <div>
        <h1 class="text-headline-small font-weight-bold">
          {{ t("orders.create.title") }}
        </h1>
        <p class="text-body-medium text-medium-emphasis mt-1">
          {{ t("orders.create.subtitle") }}
        </p>
      </div>
    </div>

    <v-alert v-if="settingsError" type="error" variant="tonal" class="mb-4">{{
      t("orders.create.settingsError")
    }}</v-alert>
    <v-alert
      v-else-if="
        !settingsLoading && (!paymentMethods.length || !paymentChoices.length)
      "
      type="warning"
      variant="tonal"
      class="mb-4"
      >{{ t("orders.create.paymentUnavailable") }}</v-alert
    >

    <v-form @submit.prevent="submit">
      <v-row>
        <v-col cols="12" lg="8">
          <v-card variant="outlined" rounded="lg" class="mb-5">
            <v-card-title class="pa-4">{{
              t("orders.create.customerSection")
            }}</v-card-title>
            <v-divider />
            <v-card-text>
              <v-row>
                <v-col cols="12" md="6"
                  ><v-text-field
                    v-model="customerName"
                    :label="t('orders.create.customerName')"
                    :error-messages="errors.customerName"
                    variant="outlined"
                /></v-col>
                <v-col cols="12" md="6"
                  ><v-text-field
                    v-model="customerEmail"
                    :label="t('orders.create.customerEmail')"
                    :error-messages="errors.customerEmail"
                    type="email"
                    variant="outlined"
                /></v-col>
                <v-col cols="12"
                  ><v-text-field
                    v-model="customerPhone"
                    :label="t('orders.create.customerPhone')"
                    :error-messages="errors.customerPhone"
                    type="tel"
                    variant="outlined"
                /></v-col>
              </v-row>
            </v-card-text>
          </v-card>

          <v-card variant="outlined" rounded="lg" class="mb-5">
            <v-card-title class="pa-4">{{
              t("orders.create.rentalSection")
            }}</v-card-title
            ><v-divider />
            <v-card-text
              ><v-row>
                <v-col cols="12" md="6"
                  ><v-text-field
                    v-model="rentalStartAt"
                    type="date"
                    :label="t('orders.create.startAt')"
                    :error-messages="errors.rentalStartAt"
                    variant="outlined"
                /></v-col>
                <v-col cols="12" md="6"
                  ><v-text-field
                    v-model="rentalEndAt"
                    type="date"
                    :label="t('orders.create.endAt')"
                    :error-messages="errors.rentalEndAt"
                    variant="outlined"
                /></v-col> </v-row
            ></v-card-text>
          </v-card>

          <v-card variant="outlined" rounded="lg" class="mb-5">
            <v-card-title
              class="d-flex justify-space-between align-center pa-4"
            >
              <span>{{ t("orders.create.productsSection") }}</span>
              <v-btn
                color="primary"
                prepend-icon="mdi-plus"
                variant="tonal"
                @click="variantDialogOpen = true"
                >{{ t("orders.create.addProduct") }}</v-btn
              > </v-card-title
            ><v-divider />
            <v-card-text>
              <v-alert
                v-if="errors.items"
                type="error"
                variant="tonal"
                class="mb-3"
                >{{ errors.items }}</v-alert
              >
              <v-alert
                v-if="
                  values.items?.length &&
                  rentalDays > 0 &&
                  hasInvalidRentalPlanCombination
                "
                type="warning"
                variant="tonal"
                class="mb-3"
                >{{ t("orders.create.noRentalPlan") }}</v-alert
              >
              <v-list v-if="itemFields.length" lines="two">
                <v-list-item
                  v-for="(entry, index) in itemFields"
                  :key="entry.key"
                  class="px-0"
                >
                  <template #prepend
                    ><v-avatar rounded="lg" size="56"
                      ><v-img
                        v-if="entry.value.image"
                        :src="entry.value.image"
                        cover /><v-icon
                        v-else
                        icon="mdi-image-outline" /></v-avatar
                  ></template>
                  <v-list-item-title
                    >{{ entry.value.productName }} ·
                    {{ entry.value.variantName }}</v-list-item-title
                  >
                  <v-list-item-subtitle
                    >{{ entry.value.sku }} ·
                    {{ formatCurrency(itemTotals[index]?.rental ?? 0) }}
                    {{ t("orders.create.rentalTotal") }} ·
                    {{ t("orders.create.deposit") }}
                    {{
                      formatCurrency(itemTotals[index]?.deposit ?? 0)
                    }}</v-list-item-subtitle
                  >
                  <template #append>
                    <div class="d-flex align-center ga-2">
                      <v-text-field
                        :model-value="entry.value.quantity"
                        type="number"
                        min="1"
                        density="compact"
                        variant="outlined"
                        hide-details
                        style="width: 90px"
                        :aria-label="t('orders.create.quantity')"
                        @update:model-value="
                          setFieldValue(
                            `items.${index}.quantity`,
                            Math.max(1, Number($event) || 1),
                          )
                        "
                      />
                      <v-btn
                        icon="mdi-delete-outline"
                        variant="text"
                        color="error"
                        :aria-label="t('orders.create.removeProduct')"
                        @click="remove(index)"
                      />
                    </div>
                  </template>
                </v-list-item>
              </v-list>
              <div v-else class="py-8 text-center text-medium-emphasis">
                {{ t("orders.create.emptyProducts") }}
              </div>
            </v-card-text>
          </v-card>

          <v-card variant="outlined" rounded="lg" class="mb-5">
            <v-card-title class="pa-4">{{
              t("orders.create.fulfillmentSection")
            }}</v-card-title
            ><v-divider />
            <v-card-text>
              <v-radio-group
                v-model="fulfillmentType"
                inline
                :label="t('orders.create.fulfillmentType')"
              >
                <v-radio
                  :label="t('orders.create.pickup')"
                  value="PICKUP"
                /><v-radio
                  :label="t('orders.create.delivery')"
                  value="DELIVERY"
                />
              </v-radio-group>
              <v-text-field
                v-model="fulfillmentAt"
                type="datetime-local"
                :label="
                  fulfillmentType === 'PICKUP'
                    ? t('orders.create.pickupAt')
                    : t('orders.create.deliveryAt')
                "
                :error-messages="errors.fulfillmentAt"
                variant="outlined"
              />
              <v-textarea
                v-if="fulfillmentType === 'DELIVERY'"
                v-model="address"
                :label="t('orders.create.deliveryAddress')"
                :error-messages="errors.address"
                variant="outlined"
                rows="3"
              />
              <v-text-field
                v-if="fulfillmentType === 'DELIVERY'"
                v-model.number="deliveryFee"
                type="number"
                min="0"
                :label="t('orders.create.deliveryFee')"
                variant="outlined"
              />
            </v-card-text>
          </v-card>

          <v-card variant="outlined" rounded="lg" class="mb-5">
            <v-card-title class="pa-4">{{
              t("orders.create.paymentSection")
            }}</v-card-title
            ><v-divider />
            <v-card-text>
              <v-select
                v-model="initialPaymentChoice"
                :items="paymentChoices"
                :label="t('orders.create.initialPayment')"
                :error-messages="errors.initialPaymentChoice"
                variant="outlined"
              />
              <v-select
                v-if="initialPaymentChoice === 'DOWN_PAYMENT'"
                v-model="balanceDueAt"
                :items="[
                  { title: t('orders.create.dueAtPickup'), value: 'PICKUP' },
                  { title: t('orders.create.dueAtReturn'), value: 'RETURN' },
                ]"
                :label="t('orders.create.balanceDueAt')"
                :error-messages="errors.balanceDueAt"
                variant="outlined"
              />
              <v-select
                v-model="paymentMethod"
                :items="paymentMethods"
                :label="t('orders.create.paymentMethod')"
                :error-messages="errors.paymentMethod"
                variant="outlined"
              />
              <v-checkbox
                v-model="depositWaived"
                :label="t('orders.create.depositWaived')"
                :hint="t('orders.create.depositWaivedHint')"
                persistent-hint
                density="compact"
              />
              <v-text-field
                v-if="depositWaived"
                v-model="depositWaiverReason"
                :label="t('orders.create.depositWaiverReason')"
                :error-messages="errors.depositWaiverReason"
                variant="outlined"
                maxlength="200"
              />
              <v-alert
                v-if="paymentSettings.instructions"
                type="info"
                variant="tonal"
                >{{ paymentSettings.instructions }}</v-alert
              >
            </v-card-text>
          </v-card>

          <v-card variant="outlined" rounded="lg">
            <v-card-title class="pa-4">{{
              t("orders.create.notes")
            }}</v-card-title
            ><v-divider />
            <v-card-text
              ><QuillEditor
                v-model:content="notes"
                content-type="html"
                theme="snow"
                :placeholder="t('orders.create.notesPlaceholder')"
                style="min-height: 140px"
            /></v-card-text>
          </v-card>
        </v-col>

        <v-col cols="12" lg="4">
          <v-card variant="outlined" rounded="lg" class="summary-card">
            <v-card-title class="pa-4">{{
              t("orders.create.summary")
            }}</v-card-title
            ><v-divider />
            <v-card-text>
              <div class="d-flex justify-space-between mb-3">
                <span>{{ t("orders.create.rentalSubtotal") }}</span
                ><span>{{ formatCurrency(rentalSubtotal) }}</span>
              </div>
              <div class="d-flex justify-space-between mb-3">
                <span>{{ t("orders.create.deposit") }}</span
                ><span>{{ formatCurrency(chargedDepositSubtotal) }}</span>
              </div>
              <v-alert
                v-if="depositWaived"
                type="info"
                variant="tonal"
                density="compact"
                class="mb-3"
                >{{
                  t("orders.create.depositWaivedSummary", {
                    amount: formatCurrency(depositSubtotal),
                  })
                }}</v-alert
              >
              <div
                v-if="fulfillmentType === 'DELIVERY'"
                class="d-flex justify-space-between mb-3"
              >
                <span>{{ t("orders.create.deliveryFee") }}</span
                ><span>{{ formatCurrency(Number(deliveryFee || 0)) }}</span>
              </div>
              <v-divider class="my-4" />
              <div class="d-flex justify-space-between font-weight-bold">
                <span>{{ t("orders.create.total") }}</span
                ><span>{{ formatCurrency(totalAmount) }}</span>
              </div>
              <div
                class="d-flex justify-space-between text-medium-emphasis mt-3"
              >
                <span>{{ t("orders.create.initialAmount") }}</span
                ><span>{{ formatCurrency(initialAmount) }}</span>
              </div>
              <v-btn
                block
                class="mt-5"
                color="primary"
                size="large"
                type="submit"
                :loading="isSubmitting"
                :disabled="
                  settingsLoading ||
                  settingsError ||
                  !paymentMethods.length ||
                  !paymentChoices.length ||
                  hasInvalidRentalPlanCombination
                "
                >{{ t("orders.create.submit") }}</v-btn
              >
              <v-btn block class="mt-2" variant="text" to="/merchant/orders">{{
                t("orders.create.cancel")
              }}</v-btn>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
    </v-form>
    <ProductVariantDialog
      v-model="variantDialogOpen"
      multiple
      @selected-multiple="addSelections"
    />
  </v-container>
</template>

<style scoped>
.summary-card {
  position: sticky;
  top: 24px;
}
@media (max-width: 1279px) {
  .summary-card {
    position: static;
  }
}
</style>
