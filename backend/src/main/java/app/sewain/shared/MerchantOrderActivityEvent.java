package app.sewain.shared;

import java.math.BigDecimal;
import java.util.UUID;

public record MerchantOrderActivityEvent(
		UUID orderId,
		String orderNumber,
		String customerName,
		String eventType,
		String previousStatus,
		String currentStatus,
		String paymentStage,
		BigDecimal amount,
		String tenantSchema
) {}
