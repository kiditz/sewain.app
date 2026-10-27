package app.sewain.finance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

interface FinanceTransactionRepository extends JpaRepository<FinanceTransaction, UUID> {
	boolean existsBySourceTypeAndSourceIdAndCategory(String sourceType, UUID sourceId, FinanceCategory category);

	Page<FinanceTransaction> findAllByOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtDesc(
			OffsetDateTime fromAt, OffsetDateTime toAt, Pageable pageable);

	Page<FinanceTransaction> findAllByOccurredAtGreaterThanEqualAndOccurredAtLessThanAndDirectionOrderByOccurredAtDesc(
			OffsetDateTime fromAt, OffsetDateTime toAt, FinanceDirection direction, Pageable pageable);

	@Query("select coalesce(sum(t.amount), 0) from FinanceTransaction t where t.direction = :direction and t.occurredAt >= :fromAt and t.occurredAt < :toAt")
	BigDecimal sumAmount(@Param("direction") FinanceDirection direction, @Param("fromAt") OffsetDateTime fromAt,
			@Param("toAt") OffsetDateTime toAt);

	@Query("select coalesce(sum(t.amount), 0) from FinanceTransaction t where t.category = :category and t.direction = :direction and t.occurredAt >= :fromAt and t.occurredAt < :toAt")
	BigDecimal sumCategory(@Param("category") FinanceCategory category, @Param("direction") FinanceDirection direction,
			@Param("fromAt") OffsetDateTime fromAt, @Param("toAt") OffsetDateTime toAt);

	@Query("select coalesce(sum(t.amount), 0) from FinanceTransaction t where t.category = :category and t.direction = :direction")
	BigDecimal sumLifetimeCategory(@Param("category") FinanceCategory category, @Param("direction") FinanceDirection direction);

	@Query(value = """
			select daily.transaction_date as date,
			       coalesce(sum(case when daily.direction = 'INCOME' then daily.amount else 0 end), 0) as income,
			       coalesce(sum(case when daily.direction = 'EXPENSE' then daily.amount else 0 end), 0) as expense
			from (
			    select (occurred_at at time zone :timezone)::date as transaction_date, direction, amount
			    from finance_transactions
			    where occurred_at >= :fromAt and occurred_at < :toAt
			) daily
			group by daily.transaction_date
			order by daily.transaction_date
			""", nativeQuery = true)
	List<FinanceCashFlowProjection> findCashFlow(@Param("fromAt") OffsetDateTime fromAt,
			@Param("toAt") OffsetDateTime toAt, @Param("timezone") String timezone);
}
