package app.sewain.catalogs;
import app.sewain.exception.*;
import app.sewain.shared.DurationUnit;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class OrderCatalogServiceTest {
 final ProductVariantRepository variants=mock(ProductVariantRepository.class);
 final VariantRentalPlanRepository plans=mock(VariantRentalPlanRepository.class);
 final InventoryUnitLockRepository inventory=mock(InventoryUnitLockRepository.class);
 final OrderCatalogService service=new OrderCatalogService(variants,plans,inventory);
 final UUID id=UUID.randomUUID(); final OffsetDateTime start=OffsetDateTime.parse("2026-01-01T10:00:00Z"),end=start.plusDays(2);
 ProductVariant variant(){var p=new Product();p.setId(UUID.randomUUID());p.setName("Tent");p.setStatus(ProductStatus.ACTIVE);var v=new ProductVariant();v.setId(id);v.setProduct(p);v.setName("Large");v.setSku("TENT");v.setActive(true);v.setDepositAmount(BigDecimal.TEN);when(variants.findById(id)).thenReturn(Optional.of(v));return v;}
 VariantRentalPlan plan(ProductVariant v,boolean active){var p=new VariantRentalPlan();p.setId(UUID.randomUUID());p.setVariant(v);p.setActive(active);p.setDurationUnit(DurationUnit.DAY);p.setDurationValue(1);p.setRentalFee(new BigDecimal("100"));return p;}
 @Test void quoteUsesOnlyActivePlansForRequestedVariantAndIncludesSnapshot(){var v=variant();var other=new ProductVariant();other.setId(UUID.randomUUID());when(plans.findAll()).thenReturn(List.of(plan(v,true),plan(v,false),plan(other,true)));var q=service.getPricing(id,start,end,ZoneOffset.UTC);assertEquals(0,new BigDecimal("200").compareTo(q.rentalPrice()));assertEquals(2,q.durationDays());assertEquals(v.getProduct().getId(),q.productId());assertEquals(BigDecimal.TEN,q.depositUnitAmount());var selections=(List<?>)q.rentalPriceSnapshot().get("selections");assertEquals(1,selections.size());assertEquals("DAY",((Map<?,?>)selections.getFirst()).get("durationUnit"));}
 @Test void unknownInactiveOrUnpublishedVariantCannotBeQuoted(){assertThrows(ResourceNotFoundException.class,()->service.getPricing(id,start,end,ZoneOffset.UTC));var v=variant();v.setActive(false);assertThrows(ConflictException.class,()->service.getPricing(id,start,end,ZoneOffset.UTC));v.setActive(true);v.getProduct().setStatus(ProductStatus.DRAFT);assertThrows(ConflictException.class,()->service.getPricing(id,start,end,ZoneOffset.UTC));}
 @Test void reservationAppliesBufferAndRequiresExactQuantity(){var units=List.of(UUID.randomUUID(),UUID.randomUUID());when(inventory.lockAvailableUnits(id,start,end.plusHours(2),2)).thenReturn(units);assertEquals(units,service.reserveUnits(id,start,end,2,2));assertThrows(ConflictException.class,()->service.reserveUnits(id,start,end,3,0));}
}
