package app.sewain.storefront;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

interface StorefrontRepository extends JpaRepository<StorefrontConfiguration, UUID> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select c from StorefrontConfiguration c where c.key = :key")
    Optional<StorefrontConfiguration> lockByKey(@org.springframework.data.repository.query.Param("key") String key);
    Optional<StorefrontConfiguration> findByKey(String key);
    List<StorefrontConfiguration> findAllByKeyStartingWithOrderByCreatedAtAsc(String prefix);
}
