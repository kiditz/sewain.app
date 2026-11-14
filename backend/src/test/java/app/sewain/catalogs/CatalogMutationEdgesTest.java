package app.sewain.catalogs;

import app.sewain.exception.*;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class CatalogMutationEdgesTest {
    @Test void productTypeSlugCollisionsUseIncrementingSuffixAndUpdateExcludesCurrentId() {
        var repository=mock(ProductTypeRepository.class);
        var service=new ProductTypeCommandServiceImpl(repository,Mappers.getMapper(ProductTypeMapper.class));
        when(repository.save(any())).thenAnswer(c->c.getArgument(0));
        when(repository.existsBySlug("camping")).thenReturn(true);when(repository.existsBySlug("camping-2")).thenReturn(true);
        assertEquals("camping-3",service.create(new ProductTypeCommand("Camping")).slug());
        var entity=new ProductType();entity.setId(UUID.randomUUID());when(repository.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(repository.existsBySlugAndIdNot("camping",entity.getId())).thenReturn(true);
        var result=service.update(entity.getId(),new ProductTypeCommand("Camping"));assertEquals(entity.getId(),result.id());assertEquals("camping-2",result.slug());
        service.delete(entity.getId());verify(repository).delete(entity);
    }
    @Test void productTypeRejectsEmptySlugAndUnknownIds() {
        var repository=mock(ProductTypeRepository.class);var service=new ProductTypeCommandServiceImpl(repository,Mappers.getMapper(ProductTypeMapper.class));
        assertThrows(ValidationException.class,()->service.create(new ProductTypeCommand("!!!")));
        assertThrows(ResourceNotFoundException.class,()->service.update(UUID.randomUUID(),new ProductTypeCommand("Camping")));
        assertThrows(ResourceNotFoundException.class,()->service.delete(UUID.randomUUID()));
    }
    @Test void inventoryUpdateNormalizesSkuAndSerialAndUsesAllocationHistoryForDeletionFlag() {
        var repository=mock(InventoryUnitRepository.class);var variants=mock(ProductVariantRepository.class);
        var service=new InventoryCommandServiceImpl(repository,variants,Mappers.getMapper(InventoryMapper.class));
        var unit=new InventoryUnit();unit.setId(UUID.randomUUID());when(repository.findById(unit.getId())).thenReturn(Optional.of(unit));
        when(repository.hasRentalAllocation(unit.getId())).thenReturn(true);
        var result=service.update(unit.getId(),new InventoryCommand(" BARCODE "," SERIAL ",null,InventoryUnitStatus.AVAILABLE,"Checked"));
        assertEquals("BARCODE",result.skuBarcode());assertEquals("SERIAL",result.serialNumber());assertFalse(result.deletable());
        when(repository.hasRentalAllocation(unit.getId())).thenReturn(false);
        assertTrue(service.update(unit.getId(),new InventoryCommand("BARCODE","SERIAL",null,InventoryUnitStatus.AVAILABLE,null)).deletable());
    }
    @Test void inventoryUpdateRejectsDuplicateSkuOrUnknownUnitBeforeMutating() {
        var repository=mock(InventoryUnitRepository.class);var service=new InventoryCommandServiceImpl(repository,mock(ProductVariantRepository.class),Mappers.getMapper(InventoryMapper.class));
        var id=UUID.randomUUID();var command=new InventoryCommand(" BARCODE ",null,null,InventoryUnitStatus.AVAILABLE,null);
        assertThrows(ResourceNotFoundException.class,()->service.update(id,command));
        var unit=new InventoryUnit();when(repository.findById(id)).thenReturn(Optional.of(unit));when(repository.existsBySkuBarcodeAndIdNot("BARCODE",id)).thenReturn(true);
        assertThrows(ConflictException.class,()->service.update(id,command));assertNull(unit.getSkuBarcode());
    }
}
