package app.sewain.merchants;

import org.springframework.data.repository.ListCrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface MerchantRepository extends ListCrudRepository<Merchant, UUID> {
	Optional<Merchant> findByUserIdAndSlug(UUID userId, String slug);
	List<Merchant> findByUserId(UUID userId);
	Optional<Merchant> findBySlug(String slug);
	Optional<Merchant> findBySchema(String schema);
}
