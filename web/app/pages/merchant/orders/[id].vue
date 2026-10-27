<script setup lang="ts">
import type {
  OrderStatus,
  MerchantOrderPayment,
  RecordDepositRefundCommand,
  RecordOrderPaymentCommand,
  RecordPaymentRefundCommand,
  RecordOrderReturnCommand,
} from "~/types/order";

definePageMeta({ layout: "merchant", middleware: ["merchant"] });

const { t } = useI18n();
useHead({ title: t("menu.orders") });

const route = useRoute();

const {
  findOrder,
  updateOrderStatus,
  recordPayment,
  recordRefund,
  recordDepositRefund,
  recordReturn,
} = useOrder();

const { formatMoney, formatDate, loadRegionalSettings } = useSettings();
const snackbar = useSnackbarStore();

const orderId = computed(() => String(route.params.id));

const updatingStatus = ref(false);

const paymentDialog = ref(false);
const savingPayment = ref(false);

const refundDialog = ref(false);
const savingRefund = ref(false);
const selectedPayment = ref<MerchantOrderPayment | null>(null);

const depositRefundDialog = ref(false);
const savingDepositRefund = ref(false);

const returnDialog = ref(false);
const savingReturn = ref(false);
const returnForm = ref({ waiveLateFee: false, waiverReason: "" });

const paymentForm = ref({
  stage: "INITIAL" as RecordOrderPaymentCommand["stage"],
  amount: null as number | null,
  method: "BANK_TRANSFER" as "BANK_TRANSFER" | "CASH",
  reference: "",
});

const refundForm = ref({
  amount: null as number | null,
  reason: "",
});

const depositRefundForm = ref({
  amount: null as number | null,
  reason: "",
});

const {
  data: order,
  pending,
  error,
  refresh,
} = await useAsyncData(
  () => `merchant-order-${orderId.value}`,
  () => findOrder(orderId.value),
  { watch: [orderId] },
);

const statusColor = (value: OrderStatus) =>
  ({
    PENDING_PAYMENT: "warning",
    CONFIRMED: "primary",
    PROCESSING: "info",
    READY: "info",
    ACTIVE_RENTAL: "deep-purple",
    COMPLETED: "success",
    CANCELLED: "error",
    EXPIRED: "default",
  })[value];

const formatPlanDuration = (value: number, unit: string) =>
  t(`orders.detail.durationUnits.${unit}.${value === 1 ? "one" : "other"}`, {
    count: value,
  });

const statusProgression: Partial<Record<OrderStatus, OrderStatus>> = {
  CONFIRMED: "PROCESSING",
  PROCESSING: "READY",
  READY: "ACTIVE_RENTAL",
  ACTIVE_RENTAL: "COMPLETED",
};

const nextStatus = computed<OrderStatus | null>(() =>
  order.value
    ? order.value.status === "ACTIVE_RENTAL" && !order.value.returnedAt
      ? null
      : (statusProgression[order.value.status] ?? null)
    : null,
);

const canCancel = computed(() =>
  ["PENDING_PAYMENT", "CONFIRMED", "PROCESSING", "READY"].includes(
    order.value?.status ?? "",
  ),
);

const remainingLateFeeAmount = computed(() => {
  if (!order.value) return 0;
  const received = (order.value.payments ?? [])
    .filter(
      (payment) =>
        payment.stage === "LATE_FEE" &&
        ["SUCCEEDED", "PARTIALLY_REFUNDED"].includes(payment.status),
    )
    .reduce((total, payment) => total + Number(payment.receivedAmount ?? 0), 0);
  return Math.max(0, Number(order.value.lateFeeTotal ?? 0) - received);
});

/**
 * Deposit yang masih dapat dikembalikan.
 * Kurangi refund deposit yang sudah tercatat.
 */
const refundableDeposit = computed(
  () =>
    order.value?.payments?.reduce(
      (total, payment) =>
        total +
        Math.max(
          0,
          Number(payment.depositReceivedAmount ?? 0) -
            Number(payment.depositRefundedAmount ?? 0),
        ),
      0,
    ) ?? 0,
);

const canRefundDeposit = computed(
  () => order.value?.status === "COMPLETED" && refundableDeposit.value > 0,
);

const canRecordReturn = computed(
  () => Boolean(order.value && order.value.status === "ACTIVE_RENTAL" && !order.value.returnedAt),
);

/**
 * Saldo refund per transaksi pembayaran.
 */
const getRefundableAmount = (payment: MerchantOrderPayment) =>
  Math.max(
    0,
    Number(payment.amount ?? 0) - Number(payment.refundedAmount ?? 0),
  );

const canRefundPayment = (payment: MerchantOrderPayment) =>
  ["SUCCEEDED", "PARTIALLY_REFUNDED"].includes(payment.status) &&
   order.value?.status == 'CANCELLED' &&
  getRefundableAmount(payment) > 0;

const selectedRefundableAmount = computed(() =>
  selectedPayment.value ? getRefundableAmount(selectedPayment.value) : 0,
);

const paymentStageOptions = computed(() => {
  if (!order.value) return [];

  const options: Array<{
    title: string;
    value: RecordOrderPaymentCommand["stage"];
  }> = [];

  if (order.value.status === "PENDING_PAYMENT") {
    options.push({
      title: t("orders.detail.paymentStages.INITIAL"),
      value: "INITIAL",
    });
  }

  if (
    remainingLateFeeAmount.value > 0 &&
    order.value.status !== "PENDING_PAYMENT"
  ) {
    options.push({
      title: t("orders.detail.paymentStages.LATE_FEE"),
      value: "LATE_FEE",
    });
  }

  if (
    (order.value.remainingRentalAmount ?? 0) - remainingLateFeeAmount.value > 0 &&
    order.value.status !== "PENDING_PAYMENT"
  ) {
    options.push({
      title: t("orders.detail.paymentStages.BALANCE"),
      value: "BALANCE",
    });
  }

  if (
    (order.value.remainingDepositAmount ?? 0) > 0 &&
    order.value.status !== "PENDING_PAYMENT"
  ) {
    options.push({
      title: t("orders.detail.paymentStages.DEPOSIT"),
      value: "DEPOSIT",
    });
  }

  return options;
});

const paymentStageMaximum = computed(() => {
  if (!order.value) return 0;
  if (paymentForm.value.stage === "LATE_FEE") return remainingLateFeeAmount.value;
  if (paymentForm.value.stage === "BALANCE") {
    return Math.max(
      0,
      (order.value.remainingRentalAmount ?? 0) - remainingLateFeeAmount.value,
    );
  }
  if (paymentForm.value.stage === "DEPOSIT") return order.value.remainingDepositAmount ?? 0;
  return order.value.initialAmountDue ?? 0;
});

watch(
  () => paymentForm.value.stage,
  (stage) => {
    if (!order.value) return;

    paymentForm.value.amount =
      stage === "INITIAL"
        ? (order.value.initialAmountDue ?? null)
        : stage === "LATE_FEE"
          ? remainingLateFeeAmount.value
        : stage === "BALANCE"
            ? Math.max(0, (order.value.remainingRentalAmount ?? 0) - remainingLateFeeAmount.value)
            : (order.value.remainingDepositAmount ?? null);
  },
);

const changeStatus = async (status: OrderStatus) => {
  if (!order.value) return;

  updatingStatus.value = true;

  try {
    await updateOrderStatus(order.value.id, status);
    await refresh();

    snackbar.openSnackbar({
      type: "success",
      message: t("orders.detail.statusSaved"),
    });
  } catch {
    snackbar.openSnackbar({
      type: "error",
      message: t("orders.detail.statusError"),
    });
  } finally {
    updatingStatus.value = false;
  }
};

const openPaymentDialog = () => {
  const nextStage = paymentStageOptions.value[0]?.value ?? "INITIAL";

  const amount =
    nextStage === "INITIAL"
      ? (order.value?.initialAmountDue ?? null)
      : nextStage === "LATE_FEE"
        ? remainingLateFeeAmount.value
        : nextStage === "BALANCE"
          ? Math.max(
              0,
              (order.value?.remainingRentalAmount ?? 0) - remainingLateFeeAmount.value,
            )
          : (order.value?.remainingDepositAmount ?? null);

  paymentForm.value = {
    stage: nextStage,
    amount,
    method: order.value?.paymentMethod ?? "BANK_TRANSFER",
    reference: "",
  };

  paymentDialog.value = true;
};

const canRecordLateFeePayment = computed(
  () =>
    Boolean(
      order.value?.lateFee?.applied &&
        remainingLateFeeAmount.value > 0 &&
        paymentStageOptions.value.some((option) => option.value === "LATE_FEE"),
    ),
);

const openLateFeePaymentDialog = () => {
  if (!order.value || !canRecordLateFeePayment.value) return;

  paymentForm.value = {
    stage: "BALANCE",
    amount: remainingLateFeeAmount.value,
    method: order.value.paymentMethod ?? "BANK_TRANSFER",
    reference: "",
  };
  paymentDialog.value = true;
};

const submitPayment = async () => {
  if (
    !order.value ||
    !paymentForm.value.amount ||
    paymentForm.value.amount <= 0
  ) {
    return;
  }

  savingPayment.value = true;

  try {
    await recordPayment(
      order.value.id,
      paymentForm.value as RecordOrderPaymentCommand,
    );

    paymentDialog.value = false;
    await refresh();

    snackbar.openSnackbar({
      type: "success",
      message: t("orders.detail.paymentSaved"),
    });
  } catch {
    snackbar.openSnackbar({
      type: "error",
      message: t("orders.detail.paymentError"),
    });
  } finally {
    savingPayment.value = false;
  }
};

/**
 * Payment refund
 */
const openRefundDialog = (payment: MerchantOrderPayment) => {
  if (!canRefundPayment(payment)) return;

  selectedPayment.value = payment;

  refundForm.value = {
    amount: getRefundableAmount(payment),
    reason: "",
  };

  refundDialog.value = true;
};

const submitRefund = async () => {
  if (!order.value || !selectedPayment.value) return;

  const amount = refundForm.value.amount;
  const maxAmount = selectedRefundableAmount.value;

  if (!amount || amount <= 0 || amount > maxAmount) return;

  savingRefund.value = true;

  try {
    const command: RecordPaymentRefundCommand = {
      amount,
      reason: refundForm.value.reason.trim() || undefined,
    };

    await recordRefund(order.value.id, selectedPayment.value.id, command);

    refundDialog.value = false;
    selectedPayment.value = null;

    await refresh();

    snackbar.openSnackbar({
      type: "success",
      message: t("orders.detail.refundSaved"),
    });
  } catch {
    snackbar.openSnackbar({
      type: "error",
      message: t("orders.detail.refundError"),
    });
  } finally {
    savingRefund.value = false;
  }
};

/**
 * Deposit refund
 */
const openDepositRefundDialog = () => {
  depositRefundForm.value = {
    amount: refundableDeposit.value,
    reason: "",
  };

  depositRefundDialog.value = true;
};

const submitDepositRefund = async () => {
  const amount = depositRefundForm.value.amount;

  if (
    !order.value ||
    !amount ||
    amount <= 0 ||
    amount > refundableDeposit.value
  ) {
    return;
  }

  savingDepositRefund.value = true;

  try {
    await recordDepositRefund(
      order.value.id,
      depositRefundForm.value as RecordDepositRefundCommand,
    );

    depositRefundDialog.value = false;
    await refresh();

    snackbar.openSnackbar({
      type: "success",
      message: t("orders.detail.depositRefundSaved"),
    });
  } catch {
    snackbar.openSnackbar({
      type: "error",
      message: t("orders.detail.depositRefundError"),
    });
  } finally {
    savingDepositRefund.value = false;
  }
};

const openReturnDialog = () => {
  returnForm.value = { waiveLateFee: false, waiverReason: "" };
  returnDialog.value = true;
};

const submitReturn = async () => {
  if (!order.value) return;
  const reason = returnForm.value.waiverReason.trim();
  if (returnForm.value.waiveLateFee && !reason) return;

  savingReturn.value = true;
  try {
    const command: RecordOrderReturnCommand = {
      waiveLateFee: returnForm.value.waiveLateFee,
      waiverReason: returnForm.value.waiveLateFee ? reason : undefined,
    };
    await recordReturn(order.value.id, command);
    returnDialog.value = false;
    await refresh();
    snackbar.openSnackbar({
      type: "success",
      message: t("orders.detail.returnSaved"),
    });
  } catch {
    snackbar.openSnackbar({
      type: "error",
      message: t("orders.detail.returnError"),
    });
  } finally {
    savingReturn.value = false;
  }
};

onMounted(async () => {
  await loadRegionalSettings();

  if (error.value) {
    snackbar.openSnackbar({
      type: "error",
      message: t("orders.detail.loadError"),
    });
  }
});
</script>

<template>
  <v-container fluid class="pa-4 pa-md-6">
    <div class="d-flex align-center ga-3 mb-6">
      <v-btn
        icon="mdi-arrow-left"
        variant="text"
        to="/merchant/orders"
        :aria-label="t('orders.detail.back')"
      />
      <div class="flex-grow-1">
        <h1 class="text-headline-small font-weight-bold">
          {{ order?.orderNumber ?? t("orders.detail.title") }}
        </h1>
        <p class="text-body-medium text-medium-emphasis mt-1">
          {{ t("orders.detail.subtitle") }}
        </p>
      </div>
      <v-btn
        variant="outlined"
        prepend-icon="mdi-refresh"
        :loading="pending"
        @click="refresh"
        >{{ t("orders.detail.refresh") }}</v-btn
      >
    </div>

    <v-alert v-if="error" type="error" variant="tonal" class="mb-4">{{
      t("orders.detail.loadError")
    }}</v-alert>
    <v-skeleton-loader v-else-if="pending" type="article, table" />
    <template v-else-if="order">
      <v-row>
        <v-col cols="12" md="8">
          <v-card rounded="lg" variant="outlined" class="mb-4">
            <v-card-text class="d-flex flex-wrap align-center ga-3">
              <v-btn
                v-if="
                  paymentStageOptions.length &&
                  !['CANCELLED', 'EXPIRED', 'COMPLETED'].includes(order.status)
                "
                prepend-icon="mdi-cash-check"
                variant="tonal"
                :disabled="updatingStatus"
                @click="openPaymentDialog"
              >
                {{ t("orders.detail.recordPayment") }}
              </v-btn>
              <v-btn
                v-if="nextStatus"
                color="primary"
                :loading="updatingStatus"
                @click="changeStatus(nextStatus)"
              >
                {{ t(`orders.detail.actions.${nextStatus}`) }}
              </v-btn>
              <v-btn
                v-if="canCancel"
                color="error"
                variant="outlined"
                :loading="updatingStatus"
                @click="changeStatus('CANCELLED')"
              >
                {{ t("orders.detail.actions.CANCELLED") }}
              </v-btn>
              <v-btn
                v-if="canRecordReturn"
                color="warning"
                variant="tonal"
                prepend-icon="mdi-package-down"
                @click="openReturnDialog"
              >
                {{ t("orders.detail.recordReturn") }}
              </v-btn>
              <v-btn
                v-if="canRefundDeposit"
                color="warning"
                variant="tonal"
                prepend-icon="mdi-cash-refund"
                @click="openDepositRefundDialog"
              >
                {{ t("orders.detail.refundDeposit") }} ·
                {{ formatMoney(refundableDeposit) }}
              </v-btn>
            </v-card-text>
          </v-card>
          <v-card rounded="lg" variant="outlined" class="mb-4">
            <v-card-title
              class="d-flex align-center justify-space-between pa-4"
            >
              <span>{{ t("orders.detail.overview") }}</span>
              <v-chip :color="statusColor(order.status)" variant="tonal">{{
                t(`orders.status.${order.status}`)
              }}</v-chip>
            </v-card-title>
            <v-divider />
            <v-card-text>
              <v-row>
                <v-col cols="12" sm="6"
                  ><div class="text-body-small text-medium-emphasis">
                    {{ t("orders.detail.orderNumber") }}
                  </div>
                  <div class="font-weight-medium">
                    {{ order.orderNumber }}
                  </div></v-col
                >
                <v-col cols="12" sm="6"
                  ><div class="text-body-small text-medium-emphasis">
                    {{ t("orders.detail.createdAt") }}
                  </div>
                  <div class="font-weight-medium">
                    {{ formatDate(order.createdAt) }}
                  </div></v-col
                >
                <v-col cols="12" sm="6"
                  ><div class="text-body-small text-medium-emphasis">
                    {{ t("orders.detail.fulfillment") }}
                  </div>
                  <div class="font-weight-medium">
                    {{ t(`orders.fulfillment.${order.fulfillmentType}`) }}
                  </div></v-col
                >
                <v-col cols="12" sm="6"
                  ><div class="text-body-small text-medium-emphasis">
                    {{ t("orders.detail.paymentMethod") }}
                  </div>
                  <div class="font-weight-medium">
                    {{
                      order.paymentMethod
                        ? t(`orders.payment.${order.paymentMethod}`)
                        : "—"
                    }}
                  </div></v-col
                >
                <v-col cols="12" sm="6">
                  <div class="text-body-small text-medium-emphasis">
                    {{ t("orders.detail.pickupAt") }}
                  </div>
                  <div class="font-weight-medium">
                    {{
                      order.pickupAt ? formatDate(order.pickupAt, true) : "—"
                    }}
                  </div>
                </v-col>
                <v-col cols="12" sm="6">
                  <div class="text-body-small text-medium-emphasis">
                    {{ t("orders.detail.returnAt") }}
                  </div>
                  <div class="font-weight-medium">
                    {{
                      order.returnAt ? formatDate(order.returnAt, true) : "—"
                    }}
                  </div>
                </v-col>
              </v-row>
            </v-card-text>
          </v-card>

          <v-card
            v-if="order.lateFee?.enabled || order.lateFee?.applied"
            rounded="lg"
            variant="outlined"
            class="mb-4"
          >
            <v-card-title class="pa-4">{{ t("orders.detail.lateFee") }}</v-card-title>
            <v-divider />
            <v-card-text>
              <v-alert
                v-if="order.lateFeeWaived"
                type="info"
                variant="tonal"
                class="mb-4"
              >
                {{ t("orders.detail.lateFeeWaived", { reason: order.lateFeeWaiverReason }) }}
              </v-alert>
              <div class="d-flex justify-space-between ga-3 mb-2">
                <span>{{ t("orders.detail.returnDueAt") }}</span>
                <span class="text-right">{{ order.lateFee.dueAt ? formatDate(order.lateFee.dueAt, true) : "—" }}</span>
              </div>
              <div class="d-flex justify-space-between ga-3 mb-2">
                <span>{{ t("orders.detail.lateBy") }}</span>
                <span>{{ t("orders.detail.minutes", { count: order.lateFee.lateMinutes }) }}</span>
              </div>
              <div class="d-flex justify-space-between ga-3 mb-2">
                <span>{{ t("orders.detail.gracePeriod") }}</span>
                <span>{{ t("orders.detail.minutes", { count: order.lateFee.graceMinutes }) }}</span>
              </div>
              <div
                v-if="order.lateFee.unit && order.lateFee.billedUnits > 0"
                class="d-flex justify-space-between ga-3 mb-2"
              >
                <span>{{ t("orders.detail.lateFeeCalculation") }}</span>
                <span class="text-right">
                  {{ formatMoney(order.lateFee.unitAmount) }} ×
                  {{ order.lateFee.billedUnits }}
                  {{ t(`orders.detail.lateFeeUnits.${order.lateFee.unit}`) }}
                  = {{ formatMoney(order.lateFee.amount) }}
                </span>
              </div>
              <v-divider class="my-3" />
              <div class="d-flex justify-space-between font-weight-bold">
                <span>{{ order.lateFee.applied ? t("orders.detail.lateFeeCharged") : t("orders.detail.lateFeeEstimate") }}</span>
                <span>{{ formatMoney(order.lateFee.applied ? (order.lateFeeTotal ?? order.lateFee.amount) : order.lateFee.amount) }}</span>
              </div>
              <v-alert
                v-if="!order.lateFee.applied && order.lateFee.amount > 0"
                type="info"
                variant="tonal"
                density="compact"
                class="mt-4"
              >
                {{ t("orders.detail.lateFeeNeedsReturn") }}
              </v-alert>
              <div v-if="canRecordLateFeePayment" class="d-flex justify-end mt-4">
                <v-btn
                  color="primary"
                  prepend-icon="mdi-cash-check"
                  @click="openLateFeePaymentDialog"
                >
                  {{ t("orders.detail.recordLateFeePayment") }}
                </v-btn>
              </div>
            </v-card-text>
          </v-card>

          <v-card rounded="lg" variant="outlined" class="mb-4">
            <v-card-title class="pa-4">{{
              t("orders.detail.products")
            }}</v-card-title>
            <v-divider />
            <div
              v-if="order.items?.length"
              class="d-flex flex-column ga-4 pa-4"
            >
              <v-card
                v-for="(item, index) in order.items"
                :key="`${item.variantId}-${index}`"
                rounded="lg"
                variant="outlined"
              >
                <v-card-text class="pa-4 pa-md-5">
                  <div
                    class="d-flex flex-column flex-sm-row align-sm-start justify-space-between ga-3"
                  >
                    <div class="flex-grow-1 min-width-0">
                      <div class="text-title-medium font-weight-bold text-wrap">
                        {{ item.productName }}
                      </div>
                      <div
                        v-if="item.variantName"
                        class="text-body-medium text-medium-emphasis mt-1"
                      >
                        {{ item.variantName }}
                      </div>
                    </div>
                    <div class="text-sm-right flex-shrink-0">
                      <div class="text-caption text-medium-emphasis">
                        {{ t("orders.detail.rentalTotal") }}
                      </div>
                      <div class="text-title-medium font-weight-bold">
                        {{ formatMoney(item.lineTotal) }}
                      </div>
                    </div>
                  </div>

                  <v-row density="compact" class="mt-3">
                    <v-col cols="12" sm="4">
                      <div class="text-caption text-medium-emphasis">
                        {{ t("orders.detail.sku") }}
                      </div>
                      <div
                        class="text-body-small font-weight-medium text-break"
                      >
                        {{ item.sku || "—" }}
                      </div>
                    </v-col>
                    <v-col cols="12" sm="4">
                      <div class="text-caption text-medium-emphasis">
                        {{ t("orders.detail.quantity") }}
                      </div>
                      <div class="text-body-small font-weight-medium">
                        {{ item.quantity }} {{ t("orders.detail.rentalUnits") }}
                      </div>
                    </v-col>
                    <v-col cols="12" sm="4">
                      <div class="text-caption text-medium-emphasis">
                        {{ t("orders.detail.rentalPeriod") }}
                      </div>
                      <div class="text-body-small font-weight-medium">
                        {{ formatDate(item.rentalStartAt) }} –
                        {{ formatDate(item.rentalEndAt) }}
                      </div>
                    </v-col>
                  </v-row>

                  <template v-if="item.rentalDurationValue > 0">
                    <v-divider class="my-4" />
                    <div class="text-label-large font-weight-medium mb-2">
                      {{ t("orders.detail.priceBreakdown") }}
                    </div>
                    <v-row density="compact">
                      <v-col cols="12" sm="4">
                        <div class="text-caption text-medium-emphasis">
                          {{ t("orders.detail.requestedDuration") }}
                        </div>
                        <div class="text-body-medium font-weight-medium">
                          {{
                            formatPlanDuration(
                              item.rentalPriceSnapshot?.requestedDurationDays ??
                                item.rentalDurationValue,
                              "DAY",
                            )
                          }}
                        </div>
                      </v-col>
                      <v-col cols="12" sm="4">
                        <div class="text-caption text-medium-emphasis">
                          {{ t("orders.detail.coveredDuration") }}
                        </div>
                        <div class="text-body-medium font-weight-medium">
                          {{
                            formatPlanDuration(
                              item.rentalPriceSnapshot?.coveredDurationDays ??
                                item.rentalDurationValue,
                              "DAY",
                            )
                          }}
                        </div>
                      </v-col>
                      <v-col cols="12" sm="4">
                        <div class="text-caption text-medium-emphasis">
                          {{ t("orders.detail.effectiveDailyPrice") }}
                        </div>
                        <div class="text-body-medium font-weight-medium">
                          {{
                            formatMoney(
                              item.rentalUnitPrice /
                                (item.rentalPriceSnapshot
                                  ?.requestedDurationDays ??
                                  item.rentalDurationValue),
                            )
                          }}
                        </div>
                      </v-col>
                    </v-row>

                    <v-card
                      v-if="item.rentalPriceSnapshot?.selections?.length"
                      rounded="md"
                      variant="tonal"
                      class="mt-3"
                    >
                      <v-card-text class="pa-3">
                        <div class="text-body-small font-weight-medium mb-2">
                          {{ t("orders.detail.selectedPlans") }}
                        </div>
                        <div
                          v-for="(selection, selectionIndex) in item
                            .rentalPriceSnapshot?.selections ?? []"
                          :key="`${selection.rentalPlanId}-${selectionIndex}`"
                          class="d-flex flex-column flex-sm-row justify-space-between ga-1 py-1"
                        >
                          <span class="text-body-small"
                            >{{ selection.quantity }} ×
                            {{ t("orders.detail.plan") }}
                            {{
                              formatPlanDuration(
                                selection.durationValue,
                                selection.durationUnit,
                              )
                            }}</span
                          >
                          <span class="text-body-small"
                            >{{ formatMoney(selection.unitPrice) }} ×
                            {{ selection.quantity }} =
                            <strong>{{
                              formatMoney(
                                selection.unitPrice * selection.quantity,
                              )
                            }}</strong></span
                          >
                        </div>
                      </v-card-text>
                    </v-card>

                    <div
                      class="d-flex flex-column flex-sm-row justify-space-between ga-1 mt-4"
                    >
                      <span class="text-body-small">{{
                        t("orders.detail.perRentalUnit")
                      }}</span>
                      <span class="text-body-medium font-weight-bold"
                        >{{ formatMoney(item.rentalUnitPrice) }} ×
                        {{ item.quantity }} =
                        {{ formatMoney(item.lineTotal) }}</span
                      >
                    </div>
                    <div
                      class="d-flex flex-column flex-sm-row justify-space-between ga-1 mt-2 text-body-small text-medium-emphasis"
                    >
                      <span>{{
                        order.depositWaived
                          ? t("orders.detail.standardDepositPerUnit")
                          : t("orders.detail.depositPerUnit")
                      }}</span>
                      <span
                        >{{ formatMoney(item.depositUnitAmount) }} ×
                        {{ item.quantity }} =
                        {{
                          formatMoney(item.depositUnitAmount * item.quantity)
                        }}</span
                      >
                    </div>
                  </template>
                </v-card-text>
              </v-card>
            </div>
            <v-card-text v-else class="text-medium-emphasis">{{
              t("orders.detail.noProducts")
            }}</v-card-text>
          </v-card>

          <v-card rounded="lg" variant="outlined">
            <v-card-title class="pa-4">{{
              t("orders.detail.customer")
            }}</v-card-title
            ><v-divider />
            <v-card-text>
              <div class="text-title-medium font-weight-medium">
                {{ order.customerName }}
              </div>
              <a
                class="text-body-medium"
                :href="`mailto:${order.customerEmail}`"
                >{{ order.customerEmail }}</a
              >
            </v-card-text>
          </v-card>
        </v-col>
        <v-col cols="12" md="4">
          <v-card rounded="lg" variant="outlined">
            <v-card-title class="pa-4">{{
              t("orders.detail.paymentSummary")
            }}</v-card-title
            ><v-divider />
            <v-card-text>
              <v-alert
                v-if="order.depositWaived"
                type="info"
                variant="tonal"
                class="mb-4"
              >
                {{
                  t("orders.detail.depositWaived", {
                    reason: order.depositWaiverReason,
                  })
                }}
              </v-alert>
              <div class="d-flex justify-space-between mb-3">
                <span>{{ t("orders.create.rentalSubtotal") }}</span
                ><span>{{ formatMoney(order.subtotal) }}</span>
              </div>
              <div class="d-flex justify-space-between mb-3">
                <span>{{ t("orders.create.deposit") }}</span
                ><span>{{ formatMoney(order.depositTotal) }}</span>
              </div>
              <div class="d-flex justify-space-between mb-3">
                <span>{{ t("orders.create.deliveryFee") }}</span
                ><span>{{ formatMoney(order.deliveryFee) }}</span>
              </div>
              <div
                v-if="(order.lateFeeTotal ?? 0) > 0"
                class="d-flex justify-space-between mb-3"
              >
                <span>{{ t("orders.detail.lateFee") }}</span>
                <span>{{ formatMoney(order.lateFeeTotal ?? 0) }}</span>
              </div>
              <v-divider class="my-4" />
              <div class="d-flex justify-space-between mb-3">
                <span>{{ t("orders.detail.totalReceived") }}</span
                ><span>{{ formatMoney(order.totalPaidAmount ?? 0) }}</span>
              </div>
              <div class="d-flex justify-space-between mb-3">
                <span>{{ t("orders.detail.remainingRental") }}</span
                ><span>{{
                  formatMoney(order.remainingRentalAmount ?? 0)
                }}</span>
              </div>
              <div
                v-if="order.balanceDueAt"
                class="text-caption text-medium-emphasis mb-3"
              >
                {{
                  t("orders.detail.balanceDueAt", {
                    when: t(
                      order.balanceDueAt === "PICKUP"
                        ? "orders.create.dueAtPickup"
                        : "orders.create.dueAtReturn",
                    ),
                  })
                }}
              </div>
              <div
                v-if="(order.remainingDepositAmount ?? 0) > 0"
                class="d-flex justify-space-between mb-3"
              >
                <span>{{ t("orders.detail.remainingDeposit") }}</span
                ><span>{{
                  formatMoney(order.remainingDepositAmount ?? 0)
                }}</span>
              </div>
              <div
                class="d-flex justify-space-between text-title-medium font-weight-bold"
              >
                <span>{{ t("orders.detail.remainingTotal") }}</span
                ><span>{{ formatMoney(order.remainingAmount ?? 0) }}</span>
              </div>
              <v-divider class="my-4" />
              <div
                class="d-flex justify-space-between text-title-medium font-weight-bold"
              >
                <span>{{ t("orders.create.total") }}</span
                ><span>{{ formatMoney(order.totalAmount) }}</span>
              </div>
            </v-card-text>
          </v-card>
          <v-card rounded="lg" variant="outlined" class="mt-4">
            <v-card-title class="pa-4">{{
              t("orders.detail.paymentHistory")
            }}</v-card-title>
            <v-divider />
            <v-list v-if="order.payments?.length" lines="two" class="py-0">
              <template
                v-for="(payment, index) in order.payments"
                :key="payment.id"
              >
                <v-list-item class="py-3">
                  <template #title>
                    <div class="d-flex align-center justify-space-between ga-2">
                      <span>{{
                        t(`orders.detail.paymentStages.${payment.stage}`)
                      }}</span>

                      <span class="font-weight-medium">{{
                        formatMoney(payment.amount)
                      }}</span>
                    </div>
                  </template>
                  <template #subtitle>
                    <div class="d-flex flex-wrap align-center ga-2 mt-1">
                      <v-chip
                        size="x-small"
                        :color="
                          payment.status === 'SUCCEEDED'
                            ? 'success'
                            : payment.status === 'PENDING'
                              ? 'warning'
                              : 'default'
                        "
                        variant="tonal"
                        >{{
                          t(`orders.paymentStatus.${payment.status}`)
                        }}</v-chip
                      >
                      <span>{{
                        payment.method
                          ? t(`orders.payment.${payment.method}`)
                          : "—"
                      }}</span>
                      <span>{{
                        formatDate(payment.paidAt ?? payment.createdAt)
                      }}</span>
                    </div>
                    <div
                      v-if="
                        ['SUCCEEDED', 'PARTIALLY_REFUNDED'].includes(
                          payment.status,
                        )
                      "
                      class="text-caption mt-1"
                    >
                      {{ t("orders.detail.netReceived") }}:
                      {{ formatMoney(payment.receivedAmount) }}
                    </div>
                    <div v-if="payment.reference" class="text-caption mt-1">
                      {{ t("orders.detail.paymentReference") }}:
                      {{ payment.reference }}
                    </div>
                    <div
                      v-if="payment.refundedAmount > 0"
                      class="text-caption mt-1"
                    >
                      {{ t("orders.detail.refunded") }}:
                      {{ formatMoney(payment.refundedAmount) }}
                    </div>
                    <div
                      v-if="
                        payment.depositReceivedAmount +
                          payment.depositRefundedAmount >
                        0
                      "
                      class="text-caption mt-1"
                    >
                      {{ t("orders.detail.depositRetained") }}:
                      {{ formatMoney(payment.depositReceivedAmount) }} ·
                      {{ t("orders.detail.depositRefunded") }}:
                      {{ formatMoney(payment.depositRefundedAmount) }}
                    </div>
                    <div
                      v-if="canRefundPayment(payment)"
                      class="d-flex justify-end mt-3"
                    >
                      <v-btn
                        color="warning"
                        variant="flat"
                        size="small"
                        prepend-icon="mdi-cash-refund"
                        :disabled="savingRefund"
                        @click="openRefundDialog(payment)"
                      >
                        {{ t("orders.detail.refundPayment") }}
                        ·
                        {{ formatMoney(getRefundableAmount(payment)) }}
                      </v-btn>
                    </div>
                  </template>
                </v-list-item>
                <v-divider v-if="index < order.payments.length - 1" />
              </template>
            </v-list>
            <v-card-text v-else class="text-medium-emphasis">{{
              t("orders.detail.noPayments")
            }}</v-card-text>
          </v-card>
        </v-col>
      </v-row>
      <v-dialog
        v-model="refundDialog"
        max-width="480"
        :persistent="savingRefund"
      >
        <v-card>
          <v-card-title class="pa-4">
            {{ t("orders.detail.refundPayment") }}
          </v-card-title>

          <v-divider />

          <v-card-text class="d-flex flex-column ga-4 pa-4">
            <div v-if="selectedPayment">
              <div class="text-body-small text-medium-emphasis">
                {{ t("orders.detail.paymentStage") }}
              </div>

              <div class="text-title-medium font-weight-medium">
                {{ t(`orders.detail.paymentStages.${selectedPayment.stage}`) }}
              </div>

              <div class="text-body-small text-medium-emphasis mt-3">
                {{ t("orders.detail.paymentReference") }}
              </div>

              <div class="text-body-medium">
                {{ selectedPayment.reference || "—" }}
              </div>
            </div>

            <v-alert type="info" variant="tonal">
              {{
                t("orders.detail.refundAvailable", {
                  amount: formatMoney(selectedRefundableAmount),
                })
              }}
            </v-alert>

            <v-text-field
              v-model.number="refundForm.amount"
              type="number"
              min="0.01"
              step="0.01"
              :max="selectedRefundableAmount"
              :label="t('orders.detail.refundAmount')"
              :disabled="savingRefund"
              :error="
                !!refundForm.amount &&
                (refundForm.amount <= 0 ||
                  refundForm.amount > selectedRefundableAmount)
              "
            />

            <v-textarea
              v-model="refundForm.reason"
              rows="3"
              :label="t('orders.detail.refundReason')"
              :disabled="savingRefund"
            />
          </v-card-text>

          <v-divider />

          <v-card-actions class="pa-4">
            <v-spacer />

            <v-btn
              variant="text"
              :disabled="savingRefund"
              @click="refundDialog = false"
            >
              {{ t("orders.create.cancel") }}
            </v-btn>

            <v-btn
              color="warning"
              prepend-icon="mdi-cash-refund"
              :loading="savingRefund"
              :disabled="
                !selectedPayment ||
                !refundForm.amount ||
                refundForm.amount <= 0 ||
                refundForm.amount > selectedRefundableAmount
              "
              @click="submitRefund"
            >
              {{ t("orders.detail.confirmRefund") }}
            </v-btn>
          </v-card-actions>
        </v-card>
      </v-dialog>
      <v-dialog v-model="paymentDialog" max-width="480">
        <v-card>
          <v-card-title>{{ t("orders.detail.recordPayment") }}</v-card-title>
          <v-card-text class="d-flex flex-column ga-3">
            <v-select
              v-model="paymentForm.stage"
              :items="paymentStageOptions"
              :label="t('orders.detail.paymentStage')"
            />
            <v-text-field
              v-model.number="paymentForm.amount"
              type="number"
              min="0.01"
              :max="paymentStageMaximum"
              step="0.01"
              :label="t('orders.detail.amountReceived')"
            />
            <v-select
              v-model="paymentForm.method"
              :items="[
                {
                  title: t('orders.payment.BANK_TRANSFER'),
                  value: 'BANK_TRANSFER',
                },
                { title: t('orders.payment.CASH'), value: 'CASH' },
              ]"
              :label="t('orders.detail.paymentMethod')"
            />
            <v-text-field
              v-model="paymentForm.reference"
              :label="t('orders.detail.paymentReference')"
            />
          </v-card-text>
          <v-card-actions>
            <v-spacer />
            <v-btn variant="text" @click="paymentDialog = false">{{
              t("orders.create.cancel")
            }}</v-btn>
            <v-btn
              color="primary"
              :loading="savingPayment"
              :disabled="
                !paymentForm.amount ||
                paymentForm.amount <= 0 ||
                paymentForm.amount > paymentStageMaximum
              "
              @click="submitPayment"
              >{{ t("orders.detail.savePayment") }}</v-btn
            >
          </v-card-actions>
        </v-card>
      </v-dialog>

      <v-dialog v-model="depositRefundDialog" max-width="480">
        <v-card>
          <v-card-title>{{ t("orders.detail.refundDeposit") }}</v-card-title>
          <v-card-text class="d-flex flex-column ga-3">
            <v-alert type="info" variant="tonal">{{
              t("orders.detail.depositRefundAvailable", {
                amount: formatMoney(refundableDeposit),
              })
            }}</v-alert>
            <v-text-field
              v-model.number="depositRefundForm.amount"
              type="number"
              min="0.01"
              step="0.01"
              :max="refundableDeposit"
              :label="t('orders.detail.depositRefundAmount')"
            />
            <v-textarea
              v-model="depositRefundForm.reason"
              rows="2"
              :label="t('orders.detail.depositRefundReason')"
            />
          </v-card-text>
          <v-card-actions>
            <v-spacer />
            <v-btn variant="text" @click="depositRefundDialog = false">{{
              t("orders.create.cancel")
            }}</v-btn>
            <v-btn
              color="primary"
              :loading="savingDepositRefund"
              :disabled="
                !depositRefundForm.amount ||
                depositRefundForm.amount <= 0 ||
                depositRefundForm.amount > refundableDeposit
              "
              @click="submitDepositRefund"
              >{{ t("orders.detail.confirmDepositRefund") }}</v-btn
            >
          </v-card-actions>
        </v-card>
      </v-dialog>

      <v-dialog v-model="returnDialog" max-width="560" :persistent="savingReturn">
        <v-card>
          <v-card-title>{{ t("orders.detail.recordReturn") }}</v-card-title>
          <v-divider />
          <v-card-text class="d-flex flex-column ga-3">
            <v-alert
              v-if="order.lateFee?.enabled"
              :type="order.lateFee.amount > 0 ? 'warning' : 'success'"
              variant="tonal"
            >
              <div>{{ t("orders.detail.returnFeeEstimate") }}</div>
              <div v-if="order.lateFee.dueAt" class="mt-1">
                {{ t("orders.detail.returnDueAt") }}:
                {{ formatDate(order.lateFee.dueAt, true) }}
              </div>
              <div class="font-weight-bold mt-1">
                {{ formatMoney(order.lateFee.amount) }}
              </div>
              <div v-if="order.lateFee.unit && order.lateFee.billedUnits > 0" class="text-body-small mt-1">
                {{ formatMoney(order.lateFee.unitAmount) }} ×
                {{ order.lateFee.billedUnits }}
                {{ t(`orders.detail.lateFeeUnits.${order.lateFee.unit}`) }}
              </div>
            </v-alert>
            <v-alert v-else type="info" variant="tonal">
              {{ t("orders.detail.noLateFee") }}
            </v-alert>
            <v-checkbox
              v-if="(order.lateFee?.amount ?? 0) > 0"
              v-model="returnForm.waiveLateFee"
              :label="t('orders.detail.waiveLateFee')"
              color="primary"
              hide-details
            />
            <v-textarea
              v-if="returnForm.waiveLateFee"
              v-model="returnForm.waiverReason"
              :label="t('orders.detail.waiverReason')"
              rows="2"
              maxlength="200"
              counter
            />
            <p class="text-body-small text-medium-emphasis mb-0">
              {{ t("orders.detail.returnFeeFinalNote") }}
            </p>
          </v-card-text>
          <v-divider />
          <v-card-actions class="pa-4">
            <v-spacer />
            <v-btn variant="text" :disabled="savingReturn" @click="returnDialog = false">
              {{ t("orders.create.cancel") }}
            </v-btn>
            <v-btn
              color="primary"
              prepend-icon="mdi-package-down"
              :loading="savingReturn"
              :disabled="returnForm.waiveLateFee && !returnForm.waiverReason.trim()"
              @click="submitReturn"
            >
              {{ t("orders.detail.confirmReturn") }}
            </v-btn>
          </v-card-actions>
        </v-card>
      </v-dialog>
    </template>
  </v-container>
</template>
