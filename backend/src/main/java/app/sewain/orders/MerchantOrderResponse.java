package app.sewain.orders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.List;

record MerchantOrderResponse(
		UUID id,
		String orderNumber,
		OrderStatus status,
		FulfillmentType fulfillmentType,
		OffsetDateTime pickupAt,
		OffsetDateTime deliveryAt,
		OffsetDateTime returnAt,
		PaymentMethod paymentMethod,
		String customerName, String customerEmail,
		BigDecimal subtotal,
		BigDecimal depositTotal,
		boolean depositWaived,
		String depositWaiverReason,
		BigDecimal deliveryFee,
		BigDecimal totalAmount,
		OffsetDateTime createdAt,
		List<MerchantOrderItemResponse> items,
		List<MerchantOrderPaymentResponse> payments,
		BigDecimal totalPaidAmount,
		BigDecimal remainingAmount,
		BigDecimal remainingRentalAmount,
		BigDecimal remainingDepositAmount,
		BigDecimal initialAmountDue,
		BalanceDueAt balanceDueAt,
		OffsetDateTime returnedAt,
		BigDecimal lateFeeTotal,
		boolean lateFeeWaived,
		String lateFeeWaiverReason,
		MerchantOrderLateFeeResponse lateFee) {
}
