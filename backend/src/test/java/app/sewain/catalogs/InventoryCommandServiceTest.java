package app.sewain.catalogs;

import app.sewain.exception.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InventoryCommandServiceTest {
    private final InventoryUnitRepository inventory=mock(InventoryUnitRepository.class);
    private final ProductVariantRepository variants=mock(ProductVariantRepository.class);
    private final InventoryMapper mapper=mock(InventoryMapper.class);
    private final InventoryCommandServiceImpl service=new InventoryCommandServiceImpl(inventory,variants,mapper);
    @Test void duplicateBarcodeIsCheckedAfterTrimmingBeforeCreatingAnything() {
        when(inventory.existsBySkuBarcode("TD-1")).thenReturn(true);
        assertThrows(ConflictException.class,()->service.create(new InventoryCommand(" TD-1 ",null,null,InventoryUnitStatus.AVAILABLE,null)));
        verifyNoInteractions(mapper,variants); verify(inventory,never()).save(any());
    }
    @Test void newUnitGetsUniqueGeneratedSerialAndResolvedVariant() {
        var variant=new ProductVariant(); var variantId=UUID.randomUUID();
        when(variants.findById(variantId)).thenReturn(Optional.of(variant));
        when(mapper.toEntity(any())).thenAnswer(call->new InventoryUnit());
        when(inventory.save(any())).thenAnswer(call->call.getArgument(0));
        service.create(new InventoryCommand(" TD-1 ",null,variantId,InventoryUnitStatus.AVAILABLE,null));
        service.create(new InventoryCommand(" TD-2 ",null,variantId,InventoryUnitStatus.AVAILABLE,null));
        var capture=org.mockito.ArgumentCaptor.forClass(InventoryUnit.class); verify(inventory,times(2)).save(capture.capture());
        var units=capture.getAllValues();
        assertEquals("TD-1",units.getFirst().getSkuBarcode()); assertSame(variant,units.getFirst().getVariant());
        assertTrue(units.getFirst().getSerialNumber().matches("SN-[A-F0-9]{32}"));
        assertNotEquals(units.getFirst().getSerialNumber(),units.get(1).getSerialNumber());
    }
    @Test void refusesDeletingUnitStillAssignedToProductVariant() {
        var unit=new InventoryUnit(); unit.setId(UUID.randomUUID()); unit.setVariant(new ProductVariant());
        when(inventory.findById(unit.getId())).thenReturn(Optional.of(unit));
        assertThrows(ConflictException.class,()->service.delete(unit.getId()));
        verify(inventory,never()).delete(any(InventoryUnit.class));
    }
    @Test void refusesDeletingUnitReferencedByRentalAllocationEvenWhenUnassigned() {
        var unit=new InventoryUnit(); unit.setId(UUID.randomUUID());
        when(inventory.findById(unit.getId())).thenReturn(Optional.of(unit)); when(inventory.hasRentalAllocation(unit.getId())).thenReturn(true);
        assertThrows(ConflictException.class,()->service.delete(unit.getId())); verify(inventory,never()).delete(any(InventoryUnit.class));
    }
    @Test void deletesOnlyUnassignedUnallocatedUnit() {
        var unit=new InventoryUnit(); unit.setId(UUID.randomUUID());
        when(inventory.findById(unit.getId())).thenReturn(Optional.of(unit));
        service.delete(unit.getId()); verify(inventory).delete(unit);
    }
    @Test void missingVariantCannotPersistOrphanInventory() {
        var command=new InventoryCommand("TD-1",null,UUID.randomUUID(),InventoryUnitStatus.AVAILABLE,null);
        when(mapper.toEntity(command)).thenReturn(new InventoryUnit());
        when(variants.findById(command.variantId())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,()->service.create(command)); verify(inventory,never()).save(any());
    }
}
