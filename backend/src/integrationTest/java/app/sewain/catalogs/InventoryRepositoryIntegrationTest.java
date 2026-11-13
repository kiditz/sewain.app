package app.sewain.catalogs;

import app.sewain.exception.ConflictException;
import app.sewain.tenant.TenantContext;
import app.sewain.testsupport.JpaIntegrationSupport;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InventoryRepositoryIntegrationTest extends JpaIntegrationSupport {
    @Test
    void filtersStatusAndJoinedProductNameAndLoadsEntityGraphOutsideTransaction() {
        var tent = catalogFixture("Alpine Tent", "AVAILABLE");
        catalogFixture("Alpine Tent", "MAINTENANCE");
        catalogFixture("Cooking Stove", "AVAILABLE");
        var repository = context.getBean(InventoryUnitRepository.class);

        var page = inTransaction(() -> repository.findAll(
                InventorySpecifications.search(Map.of("status", "AVAILABLE", "variant.product.name", "Alpine")),
                PageRequest.of(0, 10)));

        assertEquals(1, page.getTotalElements());
        var unit = page.getContent().getFirst();
        assertEquals(tent.unitId(), unit.getId());
        // EntityGraph should make this safe after the repository transaction is closed.
        assertEquals("Alpine Tent", unit.getVariant().getProduct().getName());
    }

    @Test
    void paginatesFilteredInventoryWithoutDuplicateOrMissingRows() {
        var first = catalogFixture("Tent A", "AVAILABLE");
        var second = catalogFixture("Tent B", "AVAILABLE");
        catalogFixture("Tent C", "MAINTENANCE");
        var repository = context.getBean(InventoryUnitRepository.class);
        var specification = InventorySpecifications.search(Map.of("status", "AVAILABLE"));
        var sort = Sort.by("skuBarcode").and(Sort.by("id"));

        var firstPage = inTransaction(() -> repository.findAll(specification, PageRequest.of(0, 1, sort)));
        var secondPage = inTransaction(() -> repository.findAll(specification, PageRequest.of(1, 1, sort)));

        assertEquals(2, firstPage.getTotalElements());
        assertEquals(2, firstPage.getTotalPages());
        assertEquals(2, secondPage.getTotalElements());
        var actual = List.of(firstPage.getContent().getFirst().getSkuBarcode(),
                secondPage.getContent().getFirst().getSkuBarcode());
        assertEquals(List.of("UNIT-" + first.unitId(), "UNIT-" + second.unitId()).stream().sorted().toList(), actual);
    }

    @Test
    void lookupRequiresMatchingVariantAndUsesCurrentTenantForEachTransaction() {
        var first = catalogFixture("Tenant A tent", "AVAILABLE");
        var otherVariant = catalogFixture("Other variant", "AVAILABLE");
        var repository = context.getBean(InventoryUnitRepository.class);
        assertTrue(repository.findByIdAndVariantId(first.unitId(), first.variantId()).isPresent());
        assertTrue(repository.findByIdAndVariantId(first.unitId(), otherVariant.variantId()).isEmpty());

        var otherTenant = migratedTenant();
        TenantContext.set(otherTenant);
        try {
            assertTrue(repository.findById(first.unitId()).isEmpty(), "Known IDs must not cross tenant boundaries");
            assertFalse(repository.existsBySkuBarcode("UNIT-" + first.unitId()));
        } finally {
            TenantContext.set(tenant);
        }
        assertTrue(repository.findById(first.unitId()).isPresent());
    }

    @Test
    void reservationRejectsInsufficientAvailableQuantityWithoutRetainingPartialLocks() {
        var fixture = catalogFixture("Only available tent", "AVAILABLE");
        jdbc.update("INSERT INTO " + tenant + ".inventory_units(id,variant_id,sku_barcode,status)"
                        + " VALUES (gen_random_uuid(),?,?,'MAINTENANCE')",
                fixture.variantId(), "MAINTENANCE-" + fixture.variantId());
        var catalog = context.getBean(OrderCatalogService.class);

        assertThrows(ConflictException.class, () -> catalog.reserveUnits(fixture.variantId(), start, end, 2, 0));
        assertEquals(List.of(fixture.unitId()), catalog.reserveUnits(fixture.variantId(), start, end, 1, 0));
    }
}
