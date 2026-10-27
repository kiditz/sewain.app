package app.sewain.storefront;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

interface StorefrontRepository extends JpaRepository<StorefrontConfiguration, UUID> {
    Optional<StorefrontConfiguration> findByKey(String key);
    List<StorefrontConfiguration> findAllByKeyStartingWithOrderByCreatedAtAsc(String prefix);
}
