package app.sewain.orders;

import java.util.UUID;

record CartCommand(
		UUID customerId,
		UUID guestToken
) {
}