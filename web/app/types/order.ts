import type { RentalPlan } from "./product";

export type OrderStatus =
  | "PENDING_PAYMENT"
  | "CONFIRMED"
  | "PROCESSING"
  | "READY"
  | "ACTIVE_RENTAL"
  | "COMPLETED"
  | "CANCELLED"
  | "EXPIRED";

export const ORDER_FULFILLMENT_TYPES = ["PICKUP", "DELIVERY"] as const;
export type OrderFulfillmentType = (typeof ORDER_FULFILLMENT_TYPES)[number];

export const ORDER_PAYMENT_METHODS = ["BANK_TRANSFER", "CASH"] as const;
export type OrderPaymentMethod = (typeof ORDER_PAYMENT_METHODS)[number];

export const INITIAL_PAYMENT_CHOICES = ["DOWN_PAYMENT", "FULL_PAYMENT"] as const;
export type InitialPaymentChoice = (typeof INITIAL_PAYMENT_CHOICES)[number];

export const BALANCE_DUE_AT_OPTIONS = ["PICKUP", "RETURN"] as const;
export type BalanceDueAt = (typeof BALANCE_DUE_AT_OPTIONS)[number];

export interface MerchantOrderLateFee {
  enabled: boolean;
  applied: boolean;
  dueAt: string | null;
  evaluatedAt: string | null;
  lateMinutes: number;
  graceMinutes: number;
  unit: "HOUR" | "DAY" | null;
  unitAmount: number;
  billedUnits: number;
  amount: number;
}

export interface RecordOrderReturnCommand {
  returnedAt?: string;
  waiveLateFee: boolean;
  waiverReason?: string;
}

export interface MerchantOrderItem {
  productId: string;
  variantId: string;
  productName: string;
  variantName: string;
  sku: string;
  image: string;
  rentalStartAt: string;
  rentalEndAt: string;
  rentalDurationValue: number;
  rentalDurationUnit: string;
  quantity: number;
  variantRentalPlans: RentalPlan[];
  rentalUnitPrice: number;
  depositUnitAmount: number;
  discountAmount: number;
  taxAmount: number;
  lineTotal: number;
  rentalPriceSnapshot?: RentalPriceSnapshot | null;
}

export interface RentalPriceSnapshot {
  requestedDurationDays: number;
  coveredDurationDays: number;
  selections: Array<{
    rentalPlanId: string;
    durationUnit: "HOUR" | "DAY" | "WEEK" | "MONTH";
    durationValue: number;
    durationDays: number;
    quantity: number;
    unitPrice: number;
  }>;
  totalPrice: number;
}

export interface MerchantOrderPayment {
  id: string;
  stage: "INITIAL" | "BALANCE" | "DEPOSIT" | "LATE_FEE";
  type: "ORDER" | "DEPOSIT" | "RENTAL";
  amount: number;
  refundedAmount: number;
  receivedAmount: number;
  rentalReceivedAmount: number;
  depositReceivedAmount: number;
  rentalRefundedAmount: number;
  depositRefundedAmount: number;
  currency: string;
  status:
    | "PENDING"
    | "SUCCEEDED"
    | "FAILED"
    | "CANCELLED"
    | "EXPIRED"
    | "PARTIALLY_REFUNDED"
    | "REFUNDED";
  method: OrderPaymentMethod | null;
  reference?: string | null;
  paidAt?: string | null;
  createdAt: string;
}

export interface MerchantOrder {
  id: string;
  orderNumber: string;
  status: OrderStatus;
  fulfillmentType: OrderFulfillmentType;
  paymentMethod: OrderPaymentMethod | null;
  customerName: string;
  customerEmail: string;
  subtotal: number;
  depositTotal: number;
  depositWaived: boolean;
  depositWaiverReason?: string | null;
  deliveryFee: number;
  totalAmount: number;
  createdAt: string;
  balanceDueAt?: BalanceDueAt | null;
  items?: MerchantOrderItem[];
  payments?: MerchantOrderPayment[];
  totalPaidAmount?: number;
  remainingAmount?: number;
  remainingRentalAmount?: number;
  remainingDepositAmount?: number;
  initialAmountDue?: number;
  pickupAt?: string | null;
  deliveryAt?: string | null;
  returnAt?: string | null;
  returnedAt?: string | null;
  lateFeeTotal?: number;
  lateFeeWaived?: boolean;
  lateFeeWaiverReason?: string | null;
  lateFee?: MerchantOrderLateFee | null;
}

export interface MerchantOrderItemCommand {
  productId: string;
  variantId: string;
  quantity: number;
  rentalStartAt: string;
  rentalEndAt: string;
}

export interface MerchantOrderCommand {
  customerName: string;
  customerEmail: string;
  customerPhone?: string;
  fulfillmentType: OrderFulfillmentType;
  fulfillmentAddress?: Record<string, unknown>;
  pickupAt?: string;
  deliveryAt?: string;
  returnAt: string;
  paymentMethod: OrderPaymentMethod;
  initialPaymentChoice: InitialPaymentChoice;
  balanceDueAt?: BalanceDueAt;
  deliveryFee?: number;
  depositWaived: boolean;
  depositWaiverReason?: string;
  notes?: string;
  items: MerchantOrderItemCommand[];
}

export interface RecordOrderPaymentCommand {
  stage: "INITIAL" | "BALANCE" | "DEPOSIT" | "LATE_FEE";
  amount: number;
  method: OrderPaymentMethod;
  reference?: string;
}

export interface RecordDepositRefundCommand {
  amount: number;
  reason?: string;
}

export interface RecordPaymentRefundCommand {
  amount: number;
  reason?: string;
}

export interface OrderForm {
  productId: string;
  variantId: string;
  productName: string;
  variantName: string;
  sku: string;
  image: string | null;
  quantity: number;
  rentalUnitPrice: number;
  depositUnitAmount: number;
  variantRentalPlans: RentalPlan[];
}
