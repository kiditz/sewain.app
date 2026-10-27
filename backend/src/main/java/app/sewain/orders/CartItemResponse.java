package app.sewain.orders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

record CartItemResponse(
		UUID id,
		UUID productId,
		String productName,
		String productSlug,
		List<String> mediaList,
		UUID variantId,
		String variantName,
		int quantity,
		OffsetDateTime rentalStartAt,
		OffsetDateTime rentalEndAt,
		List<Map<String, Object>> rentalPlans,
		BigDecimal rentalSubtotal,
		BigDecimal depositSubtotal,
		BigDecimal totalAmount,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt
) {
}