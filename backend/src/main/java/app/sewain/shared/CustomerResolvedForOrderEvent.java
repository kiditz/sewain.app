package app.sewain.shared;

import java.util.UUID;

public record CustomerResolvedForOrderEvent(
		UUID orderId,
		UUID customerId
) {}
