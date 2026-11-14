package app.sewain.catalogs;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class InventoryMapperTest {
    final InventoryMapper mapper=Mappers.getMapper(InventoryMapper.class);
    @Test void unattachedUnitIsDeletableUnlessItHasRentalHistory() {
        var unit=new InventoryUnit(); unit.setId(UUID.randomUUID()); unit.setSkuBarcode("TENT-001");
        assertTrue(mapper.toResponse(unit,Set.of()).deletable());
        assertFalse(mapper.toResponse(unit,Set.of(unit.getId())).deletable());
        assertNull(mapper.toResponse(unit,Set.of()).variantId());
    }
    @Test void assignedUnitMapsCatalogIdentityAndIsNeverDeletable() {
        var product=new Product(); product.setId(UUID.randomUUID()); product.setName("Tent");
        var variant=new ProductVariant(); variant.setId(UUID.randomUUID()); variant.setProduct(product); variant.setName("4 Person"); variant.setSku("TENT-4P");
        var unit=new InventoryUnit(); unit.setId(UUID.randomUUID()); unit.setVariant(variant);
        var response=mapper.toResponse(unit,Set.of());
        assertFalse(response.deletable()); assertEquals(product.getId(),response.productId()); assertEquals("Tent",response.productName());
        assertEquals(variant.getId(),response.variantId()); assertEquals("4 Person",response.variantName()); assertEquals("TENT-4P",response.variantSku());
        variant.setProduct(null); assertNull(mapper.toResponse(unit,Set.of()).productId());
    }
    @Test void createAndUpdateMapEditableFieldsWithoutReplacingIdentityOrVariant() {
        var command=new InventoryCommand("BARCODE","SERIAL",UUID.randomUUID(),InventoryUnitStatus.MAINTENANCE,"Needs repair");
        var unit=mapper.toEntity(command); assertNull(unit.getVariant()); assertEquals("SERIAL",unit.getSerialNumber());
        var id=UUID.randomUUID(); unit.setId(id); var variant=new ProductVariant(); unit.setVariant(variant);
        mapper.update(new InventoryCommand("NEW",null,null,InventoryUnitStatus.AVAILABLE,null),unit);
        assertEquals(id,unit.getId()); assertSame(variant,unit.getVariant()); assertEquals("NEW",unit.getSkuBarcode());
        assertEquals("SERIAL",unit.getSerialNumber()); assertEquals("Needs repair",unit.getConditionNotes());
        assertEquals(InventoryUnitStatus.AVAILABLE,unit.getStatus());
    }
}
