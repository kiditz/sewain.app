package app.sewain.customers;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface CustomerRepository extends JpaRepository<Customer, UUID>, JpaSpecificationExecutor<Customer> {
	Optional<Customer> findByEmailIgnoreCase(String email);
	long countByEnabledTrue();
	long countByCreatedAtGreaterThanEqual(OffsetDateTime createdAt);

	@Query(value = "select count(*) from (select customer_id from orders where customer_id is not null group by customer_id having count(*) > 1) repeated_customers", nativeQuery = true)
	long countRepeatCustomers();

	@Query(value = """
			select o.customer_id,
			       count(distinct o.id),
			       coalesce(sum(p.amount) filter (where p.status = 'SUCCEEDED'), 0),
			       max(o.created_at)
			from orders o
			left join payments p on p.order_id = o.id
			where o.customer_id in (:customerIds)
			group by o.customer_id
			""", nativeQuery = true)
	List<Object[]> findOrderSummaries(@Param("customerIds") List<UUID> customerIds);

    Optional<Customer> findFirstByEmailIgnoreCaseOrderByCreatedAtAscIdAsc(String email);

    @org.springframework.data.jpa.repository.Modifying
    @Query(value = """
        INSERT INTO customers (id, full_name, email, enabled, source, email_subscribed, email_consent_at, created_at, updated_at)
        VALUES (:id, :name, :email, TRUE, :source, :subscribed, CASE WHEN :subscribed THEN now() ELSE NULL END, now(), now())
        ON CONFLICT DO NOTHING
        """, nativeQuery = true)
    int insertStorefrontContact(@Param("id") UUID id, @Param("name") String name, @Param("email") String email,
                                @Param("source") String source, @Param("subscribed") boolean subscribed);

    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "UPDATE customers SET email_subscribed = TRUE, email_consent_at = now(), updated_at = now() WHERE id = :id AND email_subscribed = FALSE", nativeQuery = true)
    int subscribeEmail(@Param("id") UUID id);
}
