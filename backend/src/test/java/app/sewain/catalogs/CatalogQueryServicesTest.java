package app.sewain.catalogs;

import app.sewain.exception.*;
import app.sewain.shared.PageQuery;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class CatalogQueryServicesTest {
    @Test void attributeListsOptionsSearchAndDetailUseRealMapping() {
        var repository=mock(AttributeRepository.class);
        var service=new AttributeQueryServiceImpl(repository,Mappers.getMapper(AttributeMapper.class));
        var attribute=new Attribute();attribute.setId(UUID.randomUUID());attribute.setName("Capacity");attribute.setSlug("capacity");
        when(repository.findAll()).thenReturn(List.of(attribute));
        when(repository.findAll(any(Specification.class),any(Pageable.class))).thenReturn(new PageImpl<>(List.of(attribute)));
        when(repository.findById(attribute.getId())).thenReturn(Optional.of(attribute));
        assertEquals("Capacity",service.findAll().getFirst().name());
        assertEquals(1,service.findOptions().size());assertEquals(1,service.findAll(null).totalItems());
        assertEquals(attribute.getId(),service.findById(attribute.getId()).id());
        assertThrows(ResourceNotFoundException.class,()->service.findById(UUID.randomUUID()));
    }

    @Test void inventorySearchLoadsAllocationHistoryOnceForUnassignedUnitsOnly() {
        var repository=mock(InventoryUnitRepository.class);
        var service=new InventoryQueryServiceImpl(repository,Mappers.getMapper(InventoryMapper.class));
        var free=new InventoryUnit();free.setId(UUID.randomUUID());
        var rented=new InventoryUnit();rented.setId(UUID.randomUUID());
        var attached=new InventoryUnit();attached.setId(UUID.randomUUID());attached.setVariant(new ProductVariant());
        when(repository.findAll(any(Specification.class),any(Pageable.class))).thenReturn(new PageImpl<>(List.of(free,rented,attached)));
        when(repository.findAllocatedUnitIds(List.of(free.getId(),rented.getId()))).thenReturn(Set.of(rented.getId()));
        var page=service.search(null);assertEquals(3,page.totalItems());
        assertTrue(page.items().get(0).deletable());assertFalse(page.items().get(1).deletable());assertFalse(page.items().get(2).deletable());
        verify(repository,times(1)).findAllocatedUnitIds(anyCollection());
    }

    @Test void inventoryDetailAndEmptyPagesDoNotIssueUnnecessaryHistoryQueries() {
        var repository=mock(InventoryUnitRepository.class);
        var service=new InventoryQueryServiceImpl(repository,Mappers.getMapper(InventoryMapper.class));
        var unit=new InventoryUnit();unit.setId(UUID.randomUUID());unit.setVariant(new ProductVariant());
        when(repository.findById(unit.getId())).thenReturn(Optional.of(unit));
        assertFalse(service.findById(unit.getId()).deletable());
        assertThrows(ResourceNotFoundException.class,()->service.findById(UUID.randomUUID()));
        when(repository.findAll(any(Specification.class),any(Pageable.class))).thenReturn(Page.empty());
        assertEquals(0,service.search(PageQuery.defaultQuery()).totalItems());
        verify(repository,never()).findAllocatedUnitIds(anyCollection());
    }

    final CollectionRepository collections=mock(CollectionRepository.class);
    final CollectionItemRepository membership=mock(CollectionItemRepository.class);
    final ProductRepository products=mock(ProductRepository.class);
    final ProductMapper productMapper=mock(ProductMapper.class);
    final CollectionMapper collectionMapper=Mappers.getMapper(CollectionMapper.class);
    final CollectionQueryServiceImpl collectionsService;
    CatalogQueryServicesTest() {
        ReflectionTestUtils.setField(collectionMapper,"collectionRuleMapper",Mappers.getMapper(CollectionRuleMapper.class));
        collectionsService=new CollectionQueryServiceImpl(collections,membership,products,collectionMapper,productMapper);
    }
    Collection collection(CollectionType type) {
        var collection=new Collection();collection.setId(UUID.randomUUID());collection.setName("Camping");collection.setSlug("camping");collection.setType(type);
        when(collections.findById(collection.getId())).thenReturn(Optional.of(collection));
        when(collections.findBySlug("camping")).thenReturn(Optional.of(collection));return collection;
    }
    @Test void collectionListsAndDetailMapIdentityAndMissingRecords() {
        var c=collection(CollectionType.MANUAL);
        when(collections.findAll(any(Sort.class))).thenReturn(List.of(c));
        when(collections.findAll(any(Specification.class),any(Pageable.class))).thenReturn(new PageImpl<>(List.of(c)));
        assertEquals(c.getId(),collectionsService.findCategories().getFirst().id());
        assertEquals(1,collectionsService.findAll(null).totalItems());
        assertEquals(c.getId(),collectionsService.findById(c.getId()).id());assertEquals("camping",collectionsService.findBySlug("camping").slug());
        assertThrows(ResourceNotFoundException.class,()->collectionsService.findBySlug("unknown"));
        assertThrows(ResourceNotFoundException.class,()->collectionsService.findById(UUID.randomUUID()));
    }
    @Test void manualProductsEscapeKeywordAndForwardApprovedFilters() {
        var c=collection(CollectionType.MANUAL);var id=UUID.randomUUID();
        when(membership.findProducts(any(),anyString(),any(),anyBoolean(),anyList(),anyBoolean(),any())).thenReturn(Page.empty());
        var query=PageQuery.defaultQuery();query.setSearch(Map.of("keyword"," TeNt_%! ","status","ACTIVE","id",id.toString(),"available","true"));
        collectionsService.findProducts(c.getId(),query);
        verify(membership).findProducts(eq(c.getId()),eq("%tent!_!%!!%"),eq(ProductStatus.ACTIVE),eq(true),eq(List.of(id)),eq(true),any());
    }
    @Test void manualProductsWithoutFiltersUseWildcardAndNoIdFilter() {
        var c=collection(CollectionType.MANUAL);
        when(membership.findProducts(any(),anyString(),any(),anyBoolean(),anyList(),anyBoolean(),any())).thenReturn(Page.empty());
        collectionsService.findProducts(c.getId(),null);
        verify(membership).findProducts(eq(c.getId()),eq("%"),isNull(),eq(false),eq(List.of(new UUID(0,0))),eq(false),any());
    }
    @Test void invalidCollectionStatusAndIdsAreRejectedBeforeQuery() {
        var c=collection(CollectionType.MANUAL);var query=PageQuery.defaultQuery();
        for(var filter:List.of(Map.of("status","UNKNOWN"),Map.of("id","not-a-uuid"))) {
            query.setSearch(filter);assertThrows(ValidationException.class,()->collectionsService.findProducts(c.getId(),query));
        }
        verifyNoInteractions(membership,products);
    }
    @Test void automatedProductsUseSpecificationAndStableNameIdSort() {
        var c=collection(CollectionType.AUTOMATED);
        when(products.findAll(any(Specification.class),any(Pageable.class))).thenReturn(Page.empty());
        assertEquals(0,collectionsService.findProducts(c.getId(),null).totalItems());
        var pageable=org.mockito.ArgumentCaptor.forClass(Pageable.class);verify(products).findAll(any(Specification.class),pageable.capture());
        assertEquals(List.of("name","id"),pageable.getValue().getSort().stream().map(Sort.Order::getProperty).toList());verifyNoInteractions(membership);
    }
}
