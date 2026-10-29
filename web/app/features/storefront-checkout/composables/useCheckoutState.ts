import { useCartView } from "~/features/storefront-builder/composables/useCartView";

import { toTypedSchema } from "@vee-validate/zod";
import { useForm } from "vee-validate";
import { z } from "zod";
import {
  BALANCE_DUE_AT_OPTIONS,
  INITIAL_PAYMENT_CHOICES,
  ORDER_FULFILLMENT_TYPES,
  ORDER_PAYMENT_METHODS,
  type BalanceDueAt,
  type InitialPaymentChoice,
  type OrderFulfillmentType,
  type OrderPaymentMethod,
} from "~/types/order";


export function useCheckoutState(preview = false) {
  const cartStore = useCartStore();
  const api = useApi();
  const { t } = useI18n();


  const { items, loading: cartLoading, error: cartError } = useCartView(() => preview);
  const cartItems = items;
  const cartItemCount = computed(() => items.value.length);
  const cartRentalSubtotal = computed(() => items.value.reduce((sum, item) => sum + item.rentalSubtotal, 0));
  const cartDepositSubtotal = computed(() => items.value.reduce((sum, item) => sum + item.depositSubtotal, 0));
  const cartTotalAmount = computed(() => cartRentalSubtotal.value + cartDepositSubtotal.value);

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
    bankAccounts: { bankName: string; accountNumber: string; accountHolder: string }[];
    currency: string;
  }

  interface CheckoutResponse {
    id: string;
    orderNumber: string;
    status: string;
    payments: { id: string; amount: number; status: string; stage: string; method: string }[];
  }

  const checkoutSchema = toTypedSchema(
    z.object({
      customerName: z
        .string()
        .trim()
        .min(1, t("checkout.validation.fullNameRequired"))
        .max(150, t("checkout.validation.fullNameMax")),
      email: z
        .string()
        .trim()
        .min(1, t("checkout.validation.emailRequired"))
        .email(t("checkout.validation.emailInvalid"))
        .max(254, t("checkout.validation.emailMax")),
      phone: z
        .string()
        .trim()
        .min(1, t("checkout.validation.phoneRequired"))
        .regex(/^[+()\d\s.-]{8,20}$/, t("checkout.validation.phoneInvalid")),
      fulfillmentType: z.enum(ORDER_FULFILLMENT_TYPES),
      deliveryAddress: z.string().trim().max(1000, t("checkout.validation.deliveryAddressMax")),
      paymentMethod: z.enum(ORDER_PAYMENT_METHODS),
      initialPaymentChoice: z.enum(INITIAL_PAYMENT_CHOICES),
      balanceDueAt: z.enum(BALANCE_DUE_AT_OPTIONS).nullable(),
      notes: z.string().trim().max(2000, t("checkout.validation.notesMax")),
    }).superRefine((values, ctx) => {
      if (values.fulfillmentType === "DELIVERY" && !values.deliveryAddress) {
        ctx.addIssue({ code: "custom", path: ["deliveryAddress"], message: t("checkout.validation.deliveryAddressRequired") });
      }
      if (values.initialPaymentChoice === "DOWN_PAYMENT" && !values.balanceDueAt) {
        ctx.addIssue({ code: "custom", path: ["balanceDueAt"], message: t("checkout.validation.balanceDueAtRequired") });
      }
    }),
  );

  const { defineField, errors, handleSubmit, setFieldValue } = useForm({
    validationSchema: checkoutSchema,
    initialValues: {
      customerName: "",
      email: "",
      phone: "",
      fulfillmentType: "PICKUP",
      deliveryAddress: "",
      paymentMethod: "BANK_TRANSFER",
      initialPaymentChoice: "FULL_PAYMENT",
      balanceDueAt: null,
      notes: "",
    },
  });

  const [customerName, customerNameAttrs] = defineField("customerName");
  const [email, emailAttrs] = defineField("email");
  const [phone, phoneAttrs] = defineField("phone");
  const [deliveryAddress, deliveryAddressAttrs] = defineField("deliveryAddress");
  const [notes, notesAttrs] = defineField("notes");
  const fulfillmentType = ref<OrderFulfillmentType>("PICKUP");
  const paymentMethod = ref<OrderPaymentMethod>("BANK_TRANSFER");
  const initialPaymentChoice = ref<InitialPaymentChoice>("FULL_PAYMENT");
  const balanceDueAt = ref<BalanceDueAt | null>(null);
  watch(fulfillmentType, (value) => setFieldValue("fulfillmentType", value), { immediate: true });
  watch(paymentMethod, (value) => setFieldValue("paymentMethod", value), { immediate: true });
  watch(initialPaymentChoice, (value) => setFieldValue("initialPaymentChoice", value), { immediate: true });
  watch(balanceDueAt, (value) => setFieldValue("balanceDueAt", value), { immediate: true });

  const submitting = ref(false);
  const submitError = ref<string | null>(null);
  const createdOrder = ref<CheckoutResponse | null>(null);
  const paymentOptions = ref<PaymentOptions | null>(null);

  async function loadPaymentOptions() {
    try {
      paymentOptions.value = await api<PaymentOptions>("checkout/payment-options");
      if (paymentOptions.value.downPaymentEnabled && !paymentOptions.value.fullPaymentEnabled) {
        initialPaymentChoice.value = "DOWN_PAYMENT";
      }
      if (!paymentOptions.value.downPaymentEnabled && paymentOptions.value.fullPaymentEnabled) {
        initialPaymentChoice.value = "FULL_PAYMENT";
      }
      if (!paymentOptions.value.bankTransferEnabled && (paymentOptions.value.cashAtPickupEnabled || paymentOptions.value.cashAtReturnEnabled)) {
        paymentMethod.value = "CASH";
      }
      submitError.value = null;
    } catch {
      submitError.value = t("checkout.paymentOptionsError");
    }
  }

  async function load() {
    if (preview) {
      paymentOptions.value = { bankTransferEnabled: true, cashAtPickupEnabled: true, cashAtReturnEnabled: true, downPaymentEnabled: true, downPaymentType: "PERCENTAGE", downPaymentValue: 50, fullPaymentEnabled: true, separateDeposit: false, instructions: "", bankAccounts: [], currency: "IDR" };
      return;
    }
    await Promise.all([loadPaymentOptions(), cartStore.getCart().catch(() => undefined)]);
  }

  const hasCartItems = computed(() => items.value.length > 0);
  const cashAllowed = computed(() => {
    if (initialPaymentChoice.value !== "DOWN_PAYMENT") {
      return fulfillmentType.value === "PICKUP"
        ? paymentOptions.value?.cashAtPickupEnabled
        : paymentOptions.value?.cashAtReturnEnabled;
    }
    if (balanceDueAt.value === "PICKUP") return paymentOptions.value?.cashAtPickupEnabled;
    if (balanceDueAt.value === "RETURN") return paymentOptions.value?.cashAtReturnEnabled;
    return Boolean(paymentOptions.value?.cashAtPickupEnabled || paymentOptions.value?.cashAtReturnEnabled);
  });
  const paymentReady = computed(() => {
    if (!paymentOptions.value) return false;
    const options = paymentOptions.value;
    const planAvailable = initialPaymentChoice.value === "DOWN_PAYMENT"
      ? options.downPaymentEnabled
      : options.fullPaymentEnabled;
    const methodAvailable = paymentMethod.value === "BANK_TRANSFER"
      ? options.bankTransferEnabled
      : Boolean(cashAllowed.value);
    return planAvailable && methodAvailable
      && (initialPaymentChoice.value !== "DOWN_PAYMENT" || balanceDueAt.value !== null);
  });



  const submitCheckout = handleSubmit(async (values) => {
    submitError.value = null;

    if (preview || submitting.value || !hasCartItems.value) {
      return;
    }

    submitting.value = true;

    try {
      const order = await api<CheckoutResponse>("checkout", {
        method: "POST",
        body: {
          customerName: values.customerName,
          customerEmail: values.email,
          customerPhone: values.phone,
          fulfillmentType: values.fulfillmentType,
          deliveryAddress: values.fulfillmentType === "DELIVERY" ? values.deliveryAddress : null,
          paymentMethod: values.paymentMethod,
          initialPaymentChoice: values.initialPaymentChoice,
          balanceDueAt: values.initialPaymentChoice === "DOWN_PAYMENT" ? values.balanceDueAt : null,
          notes: values.notes || null,
        },
      });
      createdOrder.value = order;
      cartStore.reset();
    } catch (cause: unknown) {
      submitError.value =
        cause instanceof Error
          ? cause.message
          : t("checkout.createOrderError");
    } finally {
      submitting.value = false;
    }
  });

  return { cartStore, t, items, cartLoading, cartError, cartItems, cartItemCount, cartRentalSubtotal, cartDepositSubtotal, cartTotalAmount, customerName, customerNameAttrs, email, emailAttrs, phone, phoneAttrs, deliveryAddress, deliveryAddressAttrs, notes, notesAttrs, fulfillmentType, paymentMethod, initialPaymentChoice, balanceDueAt, errors, submitting, submitError, createdOrder, paymentOptions, loadPaymentOptions, hasCartItems, paymentReady, submitCheckout, load };
}
