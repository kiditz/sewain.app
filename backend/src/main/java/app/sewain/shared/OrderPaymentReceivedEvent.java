package app.sewain.shared;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderPaymentReceivedEvent(UUID paymentId, UUID orderId, String orderNumber,
		BigDecimal rentalAmount, BigDecimal depositAmount, String currency, String method,
		OffsetDateTime paidAt, String tenantSchema) {}
