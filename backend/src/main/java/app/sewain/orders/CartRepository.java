package app.sewain.orders;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface CartRepository extends JpaRepository<Cart, UUID> {

	Optional<Cart> findByCustomerIdAndStatus(
			UUID customerId,
			CartStatus status
	);

	Optional<Cart> findByGuestTokenAndStatus(
			UUID guestToken,
			CartStatus status
	);
}