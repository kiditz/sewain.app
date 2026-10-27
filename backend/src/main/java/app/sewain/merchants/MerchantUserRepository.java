package app.sewain.merchants;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface MerchantUserRepository
		extends JpaRepository<MerchantUser, MerchantUserId> {

	@Query("""
    select exists (
        select 1
        from Merchant m
        join MerchantUser mu
          on mu.id.merchantId = m.id
        where m.schema = :schema
          and mu.id.userId = :userId
    )
    """)
	boolean existsBySchemaAndUserId(
			@Param("schema") String slug,
			@Param("userId") UUID userId
	);

	Optional<MerchantUser> findByIdMerchantIdAndIdUserId(
			UUID merchantId,
			UUID userId
	);
}