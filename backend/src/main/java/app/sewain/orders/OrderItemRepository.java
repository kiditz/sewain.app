package app.sewain.orders;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
	List<OrderItem> findAllByOrder_IdOrderByCreatedAtAsc(UUID orderId);
}
