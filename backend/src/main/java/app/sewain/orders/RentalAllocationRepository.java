package app.sewain.orders;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

interface RentalAllocationRepository extends JpaRepository<RentalAllocation, UUID> {
	List<RentalAllocation> findAllByOrderItem_Order_Id(UUID orderId);
}
