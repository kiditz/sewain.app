package app.sewain.orders;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface MerchantOrderActivityRepository extends JpaRepository<MerchantOrderActivity, UUID> {
	List<MerchantOrderActivity> findTop20ByOrderByCreatedAtDesc();
}
