
package app.sewain.orders;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
class CartController {

	private final CartService cartService;

	@GetMapping
	public CartResponse getCart(
			@RequestHeader(value = "X-Customer-Id", required = false) UUID customerId,
			@RequestHeader(value = "X-Guest-Token", required = false) UUID guestToken
	) {
		return cartService.getCart(new CartCommand(customerId, guestToken));
	}

	@PostMapping("/items")
	public CartResponse addItem(
			@RequestHeader(value = "X-Customer-Id", required = false) UUID customerId,
			@RequestHeader(value = "X-Guest-Token", required = false) UUID guestToken,
			@Valid @RequestBody CartItemCommand itemCommand
	) {
		return cartService.addItem(new CartCommand(customerId, guestToken), itemCommand);
	}

	@PutMapping("/items/{itemId}")
	public CartResponse updateItem(
			@RequestHeader(value = "X-Customer-Id", required = false) UUID customerId,
			@RequestHeader(value = "X-Guest-Token", required = false) UUID guestToken,
			@PathVariable UUID itemId,
			@Valid @RequestBody CartItemCommand itemCommand
	) {
		return cartService.updateItem(new CartCommand(customerId, guestToken), itemId, itemCommand);
	}

	@DeleteMapping("/items/{itemId}")
	public CartResponse removeItem(
			@RequestHeader(value = "X-Customer-Id", required = false) UUID customerId,
			@RequestHeader(value = "X-Guest-Token", required = false) UUID guestToken,
			@PathVariable UUID itemId
	) {
		return cartService.removeItem(new CartCommand(customerId, guestToken), itemId);
	}

	@DeleteMapping("/items")
	public CartResponse clearItems(
			@RequestHeader(value = "X-Customer-Id", required = false) UUID customerId,
			@RequestHeader(value = "X-Guest-Token", required = false) UUID guestToken
	) {
		return cartService.clearItems(new CartCommand(customerId, guestToken));
	}
}