package app.sewain.catalogs;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
class InventoryUnitLockRepository {
	@PersistenceContext
	private EntityManager entityManager;

	@SuppressWarnings("unchecked")
	List<UUID> lockAvailableUnits(UUID variantId, OffsetDateTime start, OffsetDateTime end, int quantity) {
		return entityManager.createNativeQuery("""
			select iu.id from inventory_units iu
			where iu.variant_id = :variantId and iu.status = 'AVAILABLE'
			and not exists (
				select 1 from rental_allocations ra
				where ra.inventory_unit_id = iu.id and ra.status in ('HELD', 'CONFIRMED')
				and tstzrange(ra.rental_start_at, ra.rental_end_at, '[)') && tstzrange(:startAt, :endAt, '[)')
			)
			order by iu.id for update skip locked limit :quantity
			""")
				.setParameter("variantId", variantId).setParameter("startAt", start)
				.setParameter("endAt", end).setParameter("quantity", quantity).getResultList();
	}
}
