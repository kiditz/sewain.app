package app.sewain.catalogs;

import app.sewain.exception.*;
import app.sewain.shared.DurationUnit;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ProductCommandServiceTest {
    final ProductRepository products=mock(ProductRepository.class);
    final AttributeValueRepository values=mock(AttributeValueRepository.class);
    final InventoryUnitRepository inventory=mock(InventoryUnitRepository.class);
    final ProductTypeRepository types=mock(ProductTypeRepository.class);
    final TagRepository tags=mock(TagRepository.class);
    final ProductMapper mapper=Mappers.getMapper(ProductMapper.class);
    final ProductVariantMapper variants=Mappers.getMapper(ProductVariantMapper.class);
    final VariantRentalPlanMapper plans=Mappers.getMapper(VariantRentalPlanMapper.class);
    final InventoryUnitMapper units=Mappers.getMapper(InventoryUnitMapper.class);
    final ProductCommandServiceImpl service;
    Product managed;
    boolean generateVariantIds=true;

    ProductCommandServiceTest() {
        ReflectionTestUtils.setField(mapper,"productVariantMapper",variants);
        ReflectionTestUtils.setField(mapper,"productTypeMapper",Mappers.getMapper(ProductTypeMapper.class));
        ReflectionTestUtils.setField(mapper,"tagMapper",Mappers.getMapper(TagMapper.class));
        ReflectionTestUtils.setField(variants,"productVariantValueMapper",Mappers.getMapper(ProductVariantValueMapper.class));
        ReflectionTestUtils.setField(variants,"variantRentalPlanMapper",plans);
        ReflectionTestUtils.setField(variants,"inventoryUnitMapper",units);
        service=new ProductCommandServiceImpl(products,values,inventory,mapper,variants,plans,types,tags,units);
        when(products.saveAndFlush(any(Product.class))).thenAnswer(call->{
            managed=call.getArgument(0); managed.setId(UUID.randomUUID()); return managed;
        });
        // Simulate persistence assigning IDs at flush; persistence itself is tested separately.
        doAnswer(call->{
            if(generateVariantIds && managed!=null) managed.getVariants().stream().filter(v->v.getId()==null).forEach(v->v.setId(UUID.randomUUID()));
            return null;
        }).when(products).flush();
    }
    ProductCommand command(UUID type,List<UUID> tagIds,List<ProductVariantCommand> variantCommands) {
        return new ProductCommand("Tent","tent","/tent.png","Rental tent",ProductStatus.ACTIVE,type,tagIds,variantCommands);
    }
    ProductVariantCommand variant(UUID id,List<UUID> attributeIds,List<VariantRentalPlanCommand> rentalPlans,List<InventoryUnitCommand> inventoryUnits) {
        return new ProductVariantCommand(id,"4 Person","TENT-4P",List.of("/large.png"),new BigDecimal("500000"),new BigDecimal("50000"),true,attributeIds,rentalPlans,inventoryUnits);
    }
    Product existing() {
        managed=new Product(); managed.setId(UUID.randomUUID()); managed.setName("Old tent"); managed.setSlug("old-tent");
        when(products.findById(managed.getId())).thenReturn(Optional.of(managed)); return managed;
    }
    ProductVariant existingVariant() {
        var v=new ProductVariant(); v.setId(UUID.randomUUID()); v.setProduct(managed); v.setName("Old"); v.setSku("OLD"); managed.getVariants().add(v); return v;
    }
    AttributeValue attributeValue(Attribute attribute,String value) {
        var v=new AttributeValue(); v.setId(UUID.randomUUID()); v.setAttribute(attribute); v.setValue(value); v.setSlug(value.toLowerCase()); return v;
    }

    @Test void createBuildsCompleteCatalogGraphAndReturnsRealMappedResponse() {
        var type=new ProductType(); type.setId(UUID.randomUUID()); type.setName("Camping");
        var tag=new Tag(); tag.setId(UUID.randomUUID()); tag.setName("Outdoor"); tag.setSlug("outdoor");
        var attribute=new Attribute(); attribute.setId(UUID.randomUUID()); attribute.setName("Capacity"); attribute.setSlug("capacity");
        var value=attributeValue(attribute,"Large");
        when(types.findById(type.getId())).thenReturn(Optional.of(type)); when(tags.findAllById(any())).thenReturn(List.of(tag));
        when(values.findAllById(List.of(value.getId()))).thenReturn(List.of(value));
        var plan=new VariantRentalPlanCommand(DurationUnit.DAY,1,new BigDecimal("100000"),true);
        var unit=new InventoryUnitCommand(null,"BARCODE",null,InventoryUnitStatus.AVAILABLE,"New");
        var response=service.create(command(type.getId(),List.of(tag.getId(),tag.getId()),List.of(variant(null,List.of(value.getId()),List.of(plan),List.of(unit)))));
        assertEquals(managed.getId(),response.id()); assertEquals(1,response.variants().size()); assertEquals(1,response.tags().size());
        assertSame(type,managed.getType()); assertEquals(Set.of(tag),managed.getTags());
        var stored=managed.getVariants().getFirst(); assertSame(managed,stored.getProduct()); assertNotNull(stored.getId());
        assertEquals("TENT-4P",stored.getSku()); assertEquals(List.of("/large.png"),stored.getMediaList());
        var selected=stored.getValues().getFirst(); assertSame(attribute,selected.getAttribute()); assertSame(value,selected.getAttributeValue());
        assertEquals(stored.getId(),selected.getId().getVariantId()); assertEquals(attribute.getId(),selected.getId().getAttributeId());
        assertSame(stored,stored.getRentalPlans().getFirst().getVariant());
        var physical=stored.getInventoryUnits().getFirst(); assertSame(stored,physical.getVariant()); assertNull(physical.getId());
        assertTrue(physical.getSerialNumber().matches("SN-[A-F0-9]{32}")); assertEquals("BARCODE",physical.getSkuBarcode());
    }

    @Test void createRejectsDuplicateSlugAndMissingProductType() {
        when(products.existsBySlug("tent")).thenReturn(true);
        assertThrows(ConflictException.class,()->service.create(command(null,null,null)));
        when(products.existsBySlug("tent")).thenReturn(false);
        assertThrows(ResourceNotFoundException.class,()->service.create(command(UUID.randomUUID(),null,null)));
        verify(products,never()).saveAndFlush(any());
    }

    @Test void updatePreservesVariantChildrenWhenOptionalListsAreOmitted() {
        var product=existing(); var v=existingVariant(); var plan=new VariantRentalPlan(); v.getRentalPlans().add(plan);
        var value=new ProductVariantValue(); v.getValues().add(value); var unit=new InventoryUnit(); v.getInventoryUnits().add(unit);
        service.update(product.getId(),command(null,null,List.of(variant(v.getId(),null,null,null))));
        assertSame(v,product.getVariants().getFirst()); assertEquals("4 Person",v.getName());
        assertSame(plan,v.getRentalPlans().getFirst()); assertSame(unit,v.getInventoryUnits().getFirst());
        assertSame(value,v.getValues().getFirst());
    }

    @Test void replacingRentalPlansAttachesNewChildrenAndEmptyListClearsThem() {
        var product=existing(); var v=existingVariant(); var old=new VariantRentalPlan(); v.getRentalPlans().add(old);
        service.update(product.getId(),command(null,null,List.of(variant(v.getId(),null,List.of(new VariantRentalPlanCommand(DurationUnit.WEEK,1,BigDecimal.TEN,false)),null))));
        assertEquals(1,v.getRentalPlans().size()); assertNotSame(old,v.getRentalPlans().getFirst()); assertSame(v,v.getRentalPlans().getFirst().getVariant());
        assertFalse(v.getRentalPlans().getFirst().isActive());
        service.update(product.getId(),command(null,null,List.of(variant(v.getId(),null,List.of(),null)))); assertTrue(v.getRentalPlans().isEmpty());
    }

    @Test void removingVariantUnlinksPhysicalInventoryRatherThanDeletingIt() {
        var product=existing(); var removed=existingVariant(); var unit=new InventoryUnit(); unit.setVariant(removed); removed.getInventoryUnits().add(unit);
        service.update(product.getId(),command(null,null,List.of()));
        assertTrue(product.getVariants().isEmpty()); assertNull(unit.getVariant()); assertTrue(removed.getInventoryUnits().isEmpty());
        verify(inventory).save(unit); verify(inventory,never()).delete(any(InventoryUnit.class));
    }

    @Test void addingVariantDuringEditDoesNotRemoveItAsAnUnretainedVariant() {
        var product=existing(); var old=existingVariant();
        service.update(product.getId(),command(null,null,List.of(variant(null,null,null,null))));
        assertEquals(1,product.getVariants().size()); assertNotSame(old,product.getVariants().getFirst()); assertNotNull(product.getVariants().getFirst().getId());
    }

    @Test void unknownAndRepeatedVariantIdsAreRejected() {
        var product=existing(); var v=existingVariant();
        assertThrows(ResourceNotFoundException.class,()->service.update(product.getId(),command(null,null,List.of(variant(UUID.randomUUID(),null,null,null)))));
        var repeated=variant(v.getId(),null,null,null);
        assertThrows(ValidationException.class,()->service.update(product.getId(),command(null,null,List.of(repeated,repeated))));
    }

    @Test void attributesRejectDuplicateIdsMissingValuesAndTwoValuesOfSameAttribute() {
        var product=existing(); var v=existingVariant(); var id=UUID.randomUUID();
        assertThrows(ValidationException.class,()->service.update(product.getId(),command(null,null,List.of(variant(v.getId(),List.of(id,id),null,null)))));
        assertThrows(ValidationException.class,()->service.update(product.getId(),command(null,null,List.of(variant(v.getId(),List.of(id),null,null)))));
        var a=new Attribute(); a.setId(UUID.randomUUID()); var first=attributeValue(a,"Small"); var second=attributeValue(a,"Large");
        var ids=List.of(first.getId(),second.getId()); when(values.findAllById(ids)).thenReturn(List.of(first,second));
        assertThrows(ValidationException.class,()->service.update(product.getId(),command(null,null,List.of(variant(v.getId(),ids,null,null)))));
    }

    @Test void explicitEmptyAttributeListRemovesSelections() {
        var product=existing(); var v=existingVariant(); v.getValues().add(new ProductVariantValue());
        service.update(product.getId(),command(null,null,List.of(variant(v.getId(),List.of(),null,null)))); assertTrue(v.getValues().isEmpty());
    }

    @Test void inventoryUpdateRequiresVariantOwnershipAndRetainsOmittedUnits() {
        var product=existing(); var v=existingVariant(); var unit=new InventoryUnit(); unit.setId(UUID.randomUUID()); unit.setVariant(v); unit.setSerialNumber("ORIGINAL");
        unit.setStatus(InventoryUnitStatus.MAINTENANCE); v.getInventoryUnits().add(unit);
        when(inventory.findByIdAndVariantId(unit.getId(),v.getId())).thenReturn(Optional.of(unit));
        service.update(product.getId(),command(null,null,List.of(variant(v.getId(),null,null,List.of(new InventoryUnitCommand(unit.getId(),"UPDATED",null,null,"Checked"))))));
        assertEquals("UPDATED",unit.getSkuBarcode()); assertEquals("ORIGINAL",unit.getSerialNumber()); assertEquals(InventoryUnitStatus.MAINTENANCE,unit.getStatus());
        service.update(product.getId(),command(null,null,List.of(variant(v.getId(),null,null,List.of())))); assertTrue(v.getInventoryUnits().contains(unit));
        assertThrows(ResourceNotFoundException.class,()->service.update(product.getId(),command(null,null,List.of(variant(v.getId(),null,null,List.of(new InventoryUnitCommand(UUID.randomUUID(),"FOREIGN",null,null,null)))))));
    }

    @Test void explicitInventoryStatusIsAppliedAndAssociationIsPreserved() {
        var product=existing(); var v=existingVariant(); var unit=new InventoryUnit(); unit.setId(UUID.randomUUID()); unit.setVariant(v);
        when(inventory.findByIdAndVariantId(unit.getId(),v.getId())).thenReturn(Optional.of(unit));
        service.update(product.getId(),command(null,null,List.of(variant(v.getId(),null,null,List.of(new InventoryUnitCommand(unit.getId(),"BARCODE",null,InventoryUnitStatus.DAMAGED,"Broken"))))));
        assertEquals(InventoryUnitStatus.DAMAGED,unit.getStatus()); assertSame(v,unit.getVariant());
    }

    @Test void missingGeneratedVariantIdFailsBeforeDependentChildrenAreCreated() {
        generateVariantIds=false;
        assertThrows(IllegalStateException.class,()->service.create(command(null,null,List.of(variant(null,List.of(),List.of(),List.of())))));
        verifyNoInteractions(inventory,values);
    }

    @Test void unknownProductAndConflictingEditSlugAreRejected() {
        assertThrows(ResourceNotFoundException.class,()->service.update(UUID.randomUUID(),command(null,null,null)));
        var product=existing(); when(products.existsBySlugAndIdNot("tent",product.getId())).thenReturn(true);
        assertThrows(ConflictException.class,()->service.update(product.getId(),command(null,null,null)));
    }

    @Test void deleteRequiresExistingProductAndDelegatesToRepository() {
        var id=UUID.randomUUID(); assertThrows(ResourceNotFoundException.class,()->service.delete(id));
        var product=existing(); service.delete(product.getId()); verify(products).delete(product);
    }
}
