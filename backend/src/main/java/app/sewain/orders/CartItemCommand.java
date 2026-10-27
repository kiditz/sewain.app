package app.sewain.orders;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

record CartItemCommand(
		@NotNull UUID productId,
		@NotNull UUID variantId,
		@Min(1) int quantity,
		@NotNull OffsetDateTime rentalStartAt,
		@NotNull OffsetDateTime rentalEndAt,
		@NotNull List<Map<String, Object>> rentalPlans,
		@NotNull BigDecimal rentalSubtotal,
		@NotNull BigDecimal depositSubtotal,
		@NotNull BigDecimal totalAmount
) {
}