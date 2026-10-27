package app.sewain.orders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

record CartResponse(
		UUID id,
		UUID customerId,
		UUID guestToken,
		String status,
		OffsetDateTime expiresAt,
		List<CartItemResponse> items,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt
) {
}