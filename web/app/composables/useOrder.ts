import type {
  MerchantOrder,
  MerchantOrderCommand,
  RecordDepositRefundCommand,
  RecordOrderPaymentCommand,
  RecordOrderReturnCommand,
  RecordPaymentRefundCommand,
} from "~/types/order";
import type { PageResponse } from "~/types/pagination";

export const useOrder = () => {
  const api = useApi();

  const findOrders = (query: Record<string, unknown>) =>
    api<PageResponse<MerchantOrder>>("/merchant/orders", { query });

  const findOrder = (id: string) =>
    api<MerchantOrder>(`/merchant/orders/${id}`);

  const createOrder = (command: MerchantOrderCommand) =>
    api<MerchantOrder>("/merchant/orders", {
      method: "POST",
      body: command,
    });

  const recordPayment = (id: string, command: RecordOrderPaymentCommand) =>
    api<MerchantOrder>(`/merchant/orders/${id}/payments`, {
      method: "POST",
      body: command,
    });

  const recordDepositRefund = (
    id: string,
    command: RecordDepositRefundCommand,
  ) =>
    api<MerchantOrder>(`/merchant/orders/${id}/deposit-refunds`, {
      method: "POST",
      body: command,
    });

  const recordRefund = (
    id: string,
    paymentId: string,
    command: RecordPaymentRefundCommand,
  ) =>
    api<MerchantOrder>(`/merchant/orders/${id}/payments/${paymentId}/refunds`, {
      method: "POST",
      body: command,
    });

  const recordReturn = (id: string, command: RecordOrderReturnCommand) =>
    api<MerchantOrder>(`/merchant/orders/${id}/return`, {
      method: "POST",
      body: command,
    });

  const updateOrderStatus = (id: string, status: MerchantOrder["status"]) =>
    api<MerchantOrder>(`/merchant/orders/${id}/status`, {
      method: "PATCH",
      body: { status },
    });

  return {
    findOrders,
    findOrder,
    createOrder,
    recordPayment,
    recordDepositRefund,
    recordReturn,
    updateOrderStatus,
    recordRefund,
  };
};
