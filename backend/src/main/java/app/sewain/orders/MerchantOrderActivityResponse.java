package app.sewain.orders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

record MerchantOrderActivityResponse(UUID id, UUID orderId, String orderNumber, String customerName,
		String eventType, String previousStatus, String currentStatus, String paymentStage,
		BigDecimal amount, OffsetDateTime createdAt) {}
