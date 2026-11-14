package app.sewain.catalogs;

import app.sewain.exception.*;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class CollectionCommandServiceTest {
    final CollectionRepository repository = mock(CollectionRepository.class);
    final ProductRepository products = mock(ProductRepository.class);
    final CollectionMapper mapper = Mappers.getMapper(CollectionMapper.class);
    final CollectionCommandServiceImpl service;
    CollectionCommandServiceTest() {
        ReflectionTestUtils.setField(mapper, "collectionRuleMapper", Mappers.getMapper(CollectionRuleMapper.class));
        service = new CollectionCommandServiceImpl(repository, products, mapper);
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
    }
    Collection collection() { var c = new Collection(); c.setId(UUID.randomUUID()); c.setName("Tents"); c.setSlug("tents"); return c; }
    CollectionCommand command(UUID parent, CollectionType type, List<CollectionRuleCommand> rules, List<UUID> ids) {
        return new CollectionCommand("Tents", "tents", parent, null, "Rental tents", type, CollectionMatchCondition.ALL, rules, ids);
    }

    @Test void legacyCreateDefaultsToManualAndAppendsAtRoot() {
        when(repository.findMaxSortOrderForRoot()).thenReturn(4);
        var response = service.create(command(null,null,null,null));
        var captor = org.mockito.ArgumentCaptor.forClass(Collection.class);
        verify(repository).save(captor.capture());
        assertEquals(5, captor.getValue().getPosition());
        assertEquals(CollectionType.MANUAL, captor.getValue().getType());
        assertEquals("tents", response.slug());
    }

    @Test void createAppendsUnderExistingParent() {
        var parent = collection(); when(repository.findById(parent.getId())).thenReturn(Optional.of(parent));
        when(repository.findMaxSortOrderByParentId(parent.getId())).thenReturn(7);
        var response = service.create(command(parent.getId(),CollectionType.MANUAL,null,null));
        assertEquals(parent.getId(), response.parentId());
        var captor = org.mockito.ArgumentCaptor.forClass(Collection.class); verify(repository).save(captor.capture());
        assertEquals(8, captor.getValue().getPosition());
    }

    @Test void unknownParentCannotCreateCollection() {
        assertThrows(ResourceNotFoundException.class, () -> service.create(command(UUID.randomUUID(),CollectionType.MANUAL,null,null)));
        verify(repository, never()).save(any());
    }

    @Test void manualUpdateReusesItemsDeduplicatesAndPreservesRequestedOrder() {
        var c = collection(); var a = new Product(); a.setId(UUID.randomUUID()); var b = new Product(); b.setId(UUID.randomUUID());
        var existing = new CollectionItem(); existing.setCollection(c); existing.setProduct(a); existing.setPosition(0); c.getItems().add(existing);
        c.getRules().add(new CollectionRule());
        when(repository.findById(c.getId())).thenReturn(Optional.of(c));
        when(products.findAllById(any())).thenReturn(List.of(a,b));
        service.update(c.getId(),command(null,CollectionType.MANUAL,null,List.of(b.getId(),a.getId(),b.getId())));
        assertEquals(2,c.getItems().size()); assertSame(b,c.getItems().getFirst().getProduct());
        assertSame(existing,c.getItems().get(1)); assertEquals(1,existing.getPosition()); assertTrue(c.getRules().isEmpty());
    }

    @Test void omittedMembershipPreservesItemsButEmptyListClearsThem() {
        var c = collection(); var item = new CollectionItem(); var p = new Product(); p.setId(UUID.randomUUID()); item.setProduct(p); c.getItems().add(item);
        when(repository.findById(c.getId())).thenReturn(Optional.of(c));
        service.update(c.getId(),command(null,CollectionType.MANUAL,null,null)); assertEquals(1,c.getItems().size());
        service.update(c.getId(),command(null,CollectionType.MANUAL,null,List.of())); assertTrue(c.getItems().isEmpty());
    }

    @Test void manualCollectionRejectsMissingProduct() {
        var c = collection(); when(repository.findById(c.getId())).thenReturn(Optional.of(c));
        assertThrows(ResourceNotFoundException.class, () -> service.update(c.getId(),command(null,CollectionType.MANUAL,null,List.of(UUID.randomUUID()))));
    }

    @Test void automatedRulesTrimDeduplicateAndDiscardEmptyValuesAndManualItems() {
        var c = collection(); c.getItems().add(new CollectionItem()); when(repository.findById(c.getId())).thenReturn(Optional.of(c));
        var blank = new CollectionRuleCommand(CollectionRuleAttribute.TITLE,CollectionRuleOperator.CONTAINS,List.of(" "));
        var rule = new CollectionRuleCommand(CollectionRuleAttribute.TAG,CollectionRuleOperator.EQUALS,List.of(" hiking ","hiking","camping"));
        service.update(c.getId(),command(null,CollectionType.AUTOMATED,List.of(blank,rule),null));
        assertTrue(c.getItems().isEmpty()); assertEquals(1,c.getRules().size());
        var stored = c.getRules().getFirst(); assertSame(c,stored.getCollection()); assertEquals(0,stored.getPosition());
        assertEquals(List.of("hiking","camping"),stored.getValues().stream().map(CollectionRuleValue::getValue).toList());
    }

    @Test void automatedCollectionNeedsNonEmptyRules() {
        assertThrows(ValidationException.class, () -> service.create(command(null,CollectionType.AUTOMATED,List.of(),null)));
        assertThrows(ValidationException.class, () -> service.create(command(null,CollectionType.AUTOMATED,null,null)));
    }

    @Test void reorderChangesOnlySelectedSiblingPositions() {
        var a=collection(); var b=collection(); var c=collection();
        when(repository.findAllById(List.of(c.getId(),a.getId()))).thenReturn(List.of(a,c));
        when(repository.findRootCategories()).thenReturn(List.of(a,b,c));
        service.reorder(new CollectionReorderCommand(List.of(c.getId(),a.getId())));
        assertEquals(0,c.getPosition()); assertEquals(2,a.getPosition());
    }

    @Test void reorderRejectsMissingCollectionsAndDifferentParents() {
        assertThrows(ResourceNotFoundException.class, () -> service.reorder(new CollectionReorderCommand(List.of(UUID.randomUUID()))));
        var a=collection(); var b=collection(); b.setParent(collection()); var ids=List.of(a.getId(),b.getId());
        when(repository.findAllById(ids)).thenReturn(List.of(a,b));
        assertThrows(ValidationException.class, () -> service.reorder(new CollectionReorderCommand(ids)));
    }

    @Test void emptyReorderIsNoOpAndDeleteRequiresExistingEntity() {
        service.reorder(new CollectionReorderCommand(List.of())); verify(repository,never()).findAllById(any());
        var c=collection(); assertThrows(ResourceNotFoundException.class, () -> service.delete(c.getId()));
        when(repository.findById(c.getId())).thenReturn(Optional.of(c)); service.delete(c.getId()); verify(repository).delete(c);
    }
}
