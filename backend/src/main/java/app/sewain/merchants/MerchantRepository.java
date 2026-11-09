package app.sewain.merchants;

import org.springframework.data.repository.ListCrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface MerchantRepository extends ListCrudRepository<Merchant, UUID> {
    List<Merchant> findByProvisioningStatus(String status);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select m from Merchant m where m.schema = :schema")
    Optional<Merchant> lockBySchema(String schema);
	Optional<Merchant> findByUserIdAndSlug(UUID userId, String slug);
	List<Merchant> findByUserId(UUID userId);
	Optional<Merchant> findBySlug(String slug);
	Optional<Merchant> findBySchema(String schema);
}
