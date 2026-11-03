package app.sewain.storefront;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.UUID;

interface StorefrontSubmissionRepository extends JpaRepository<StorefrontSubmission,UUID> {
    @Modifying
    @Query(value = """
        INSERT INTO storefront_submissions (id, kind, name, email, message, consent)
        VALUES (:id, :kind, :name, :email, :message, :consent) ON CONFLICT DO NOTHING
        """, nativeQuery = true)
    int insert(@Param("id") UUID id, @Param("kind") String kind, @Param("name") String name,
               @Param("email") String email, @Param("message") String message, @Param("consent") boolean consent);

    org.springframework.data.domain.Page<StorefrontSubmission> findByCustomerId(UUID customerId, org.springframework.data.domain.Pageable pageable);

    @Modifying
    @Query(value = "UPDATE storefront_submissions SET customer_id = :customerId WHERE id = :id", nativeQuery = true)
    int assignCustomer(@Param("id") UUID id, @Param("customerId") UUID customerId);
}
