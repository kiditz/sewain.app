package app.sewain.orders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

record MerchantOrderPaymentResponse(UUID id, PaymentStage stage, PaymentType type,
		BigDecimal amount, BigDecimal refundedAmount, BigDecimal receivedAmount,
		BigDecimal rentalReceivedAmount, BigDecimal depositReceivedAmount,
		BigDecimal rentalRefundedAmount, BigDecimal depositRefundedAmount,
		String currency, PaymentStatus status, PaymentMethod method,
		String reference, OffsetDateTime paidAt, OffsetDateTime createdAt) {}
