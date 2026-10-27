package app.sewain.orders;

import java.util.UUID;

interface CartService {

	CartResponse getCart(CartCommand command);

	CartResponse addItem(
			CartCommand command,
			CartItemCommand itemCommand
	);

	CartResponse updateItem(
			CartCommand command,
			UUID itemId,
			CartItemCommand itemCommand
	);

	CartResponse removeItem(
			CartCommand command,
			UUID itemId
	);

	CartResponse clearItems(CartCommand command);
}