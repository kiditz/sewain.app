package app.sewain.orders;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

interface PaymentRepository extends JpaRepository<Payment, UUID> {
	Optional<Payment> findFirstByOrder_IdAndPaymentStageAndStatusOrderByCreatedAtAsc(UUID orderId, PaymentStage stage, PaymentStatus status);
	java.util.List<Payment> findAllByOrder_Id(UUID orderId);
}
