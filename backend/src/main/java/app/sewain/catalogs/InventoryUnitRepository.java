package app.sewain.catalogs;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

interface InventoryUnitRepository
		extends JpaRepository<InventoryUnit, UUID>,
		JpaSpecificationExecutor<InventoryUnit> {


	Optional<InventoryUnit> findByIdAndVariantId(UUID id, UUID variantId);

	boolean existsBySkuBarcode(String skuBarcode);

	boolean existsBySkuBarcodeAndIdNot(String skuBarcode, UUID id);

	/**
	 * Hindari N+1 saat me-render nama produk & varian di daftar inventaris.
	 */
	@Override
	@EntityGraph(attributePaths = {"variant", "variant.product"})
	Page<InventoryUnit> findAll(
			Specification<InventoryUnit> spec,
			Pageable pageable
	);

	/*
	 * RentalAllocation berada di package orders. Query memakai nama entity
	 * (JPQL) supaya package catalogs tidak bergantung langsung ke orders.
	 */
	@Query("""
			select count(ra)
			from RentalAllocation ra
			where ra.inventoryUnitId = :unitId
			""")
	long countRentalAllocations(@Param("unitId") UUID unitId);

	@Query("""
			select distinct ra.inventoryUnitId
			from RentalAllocation ra
			where ra.inventoryUnitId in :unitIds
			""")
	Set<UUID> findAllocatedUnitIds(@Param("unitIds") Collection<UUID> unitIds);

	/**
	 * Apakah unit pernah dipakai di rental allocation (status apa pun).
	 */
	default boolean hasRentalAllocation(UUID unitId) {
		return countRentalAllocations(unitId) > 0;
	}
}
