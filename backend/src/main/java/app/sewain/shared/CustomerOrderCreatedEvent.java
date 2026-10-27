package app.sewain.shared;

import java.util.UUID;

public record CustomerOrderCreatedEvent(
		UUID orderId,
		String fullName,
		String email,
		String phone
) {}
