package app.sewain.orders;

import app.sewain.exception.ResourceNotFoundException;

import app.sewain.exception.ValidationException;

import app.sewain.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
class CartServiceImpl implements CartService {

	private static final CartStatus ACTIVE = CartStatus.ACTIVE;

	private final CartRepository cartRepository;
	private final CartItemRepository itemRepository;
	private final CartMapper cartMapper;
	private final CartItemMapper itemMapper;

	@Override
	@Transactional(readOnly = true)
	public CartResponse getCart(CartCommand command) {
		validateOwner(command);
		Cart cart = findExistingActiveCart(command).orElse(new Cart());
		List<CartItemResponse> items = itemMapper.toResponses(
				itemRepository.findProjectionsByCartId(cart.getId())
		);

		return cartMapper.toResponse(cart, items);
	}

	@Override
	public CartResponse addItem(
			CartCommand command,
			CartItemCommand itemCommand
	) {
		validateOwner(command);
		validateItem(itemCommand);

		Cart cart = findOrCreateCart(command);

		CartItem item = itemMapper.toEntity(itemCommand);
		cart.addItem(item);
		cart = cartRepository.saveAndFlush(cart);
		List<CartItemResponse> items = itemMapper.toResponses(
				itemRepository.findProjectionsByCartId(cart.getId())
		);
		return cartMapper.toResponse(cart, items);
	}

	@Override
	public CartResponse updateItem(
			CartCommand command,
			UUID itemId,
			CartItemCommand itemCommand
	) {
		validateOwner(command);
		validateItem(itemCommand);

		Cart cart = findActiveCart(command);
		CartItem item = findItem(cart, itemId);

		itemMapper.update(itemCommand, item);
		cart = cartRepository.saveAndFlush(cart);
		List<CartItemResponse> items = itemMapper.toResponses(
				itemRepository.findProjectionsByCartId(cart.getId())
		);
		return cartMapper.toResponse(cart, items);
	}

	@Override
	public CartResponse removeItem(
			CartCommand command,
			UUID itemId
	) {
		Cart cart = findActiveCart(command);
		CartItem item = findItem(cart, itemId);

		cart.removeItem(item);

		return cartMapper.toResponse(cartRepository.save(cart));
	}

	@Override
	public CartResponse clearItems(CartCommand command) {
		Cart cart = findActiveCart(command);

		// orphanRemoval menghapus item yang dilepas dari collection.
		for (CartItem item : new ArrayList<>(cart.getItems())) {
			cart.removeItem(item);
		}

		return cartMapper.toResponse(cartRepository.save(cart));
	}

	private Cart findOrCreateCart(CartCommand command) {
		return findExistingActiveCart(command)
				.orElseGet(() -> {
					Cart cart = new Cart();
					cart.setCustomerId(command.customerId());
					cart.setGuestToken(command.guestToken());
					cart.setStatus(ACTIVE);

					return cartRepository.save(cart);
				});
	}

	private Cart findActiveCart(CartCommand command) {
		validateOwner(command);

		return findExistingActiveCart(command)
				.orElseThrow(() -> new ResourceNotFoundException(
						ErrorCode.CART_NOT_FOUND
				));
	}

	private Optional<Cart> findExistingActiveCart(
			CartCommand command
	) {
		if (command.customerId() != null) {
			return cartRepository.findByCustomerIdAndStatus(
					command.customerId(),
					ACTIVE
			);
		}

		return cartRepository.findByGuestTokenAndStatus(
				command.guestToken(),
				ACTIVE
		);
	}

	private CartItem findItem(Cart cart, UUID itemId) {
		return cart.getItems().stream()
				.filter(item -> item.getId().equals(itemId))
				.findFirst()
				.orElseThrow(() -> new ResourceNotFoundException(
						ErrorCode.CART_ITEM_NOT_FOUND
				));
	}

	private void validateOwner(CartCommand command) {
        // Customer UUIDs supplied by a caller are not authenticated customer identities.
        if (command != null && command.customerId() != null) throw new org.springframework.security.access.AccessDeniedException(ErrorCode.CART_OWNER_INVALID);
		if (command == null
				|| (command.customerId() != null)
				== (command.guestToken() != null)) {
			throw new ValidationException(ErrorCode.CART_OWNER_INVALID);
		}
	}

	private void validateItem(CartItemCommand command) {
		if (command == null
				|| command.rentalEndAt() == null
				|| command.rentalStartAt() == null
				|| !command.rentalEndAt().isAfter(command.rentalStartAt())) {
			throw new ValidationException(
					ErrorCode.CART_ITEM_INVALID
			);
		}

		if (command.rentalPlans() == null
				|| command.rentalSubtotal() == null
				|| command.depositSubtotal() == null
				|| command.totalAmount() == null
				|| command.rentalSubtotal().signum() < 0
				|| command.depositSubtotal().signum() < 0
				|| command.totalAmount().signum() < 0) {
			throw new ValidationException(
					ErrorCode.CART_ITEM_INVALID_AMOUNT
			);
		}
	}
}