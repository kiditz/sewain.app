package app.sewain.shared;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderPaymentRefundedEvent(UUID refundId, UUID paymentId, UUID orderId,
		String orderNumber, BigDecimal rentalAmount, BigDecimal depositAmount, String currency,
		String method, OffsetDateTime processedAt, String tenantSchema) {}
