package app.sewain.orders;

import java.util.UUID;

interface MerchantOrderCommandService {
    MerchantOrderResponse create(MerchantOrderCommand command);
    MerchantOrderResponse recordPayment(UUID orderId, RecordOrderPaymentCommand command);
    MerchantOrderResponse recordRefund(UUID orderId, UUID paymentId, RecordPaymentRefundCommand command);
    MerchantOrderResponse recordDepositRefund(UUID orderId, RecordDepositRefundCommand command);
    MerchantOrderResponse recordReturn(UUID orderId, RecordOrderReturnCommand command);
    MerchantOrderResponse updateStatus(UUID orderId, OrderStatus next);
}
