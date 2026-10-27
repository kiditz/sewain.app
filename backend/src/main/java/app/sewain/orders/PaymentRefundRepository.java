package app.sewain.orders;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface PaymentRefundRepository extends JpaRepository<PaymentRefund, UUID> {
	List<PaymentRefund> findAllByPayment_Order_Id(UUID orderId);
}
