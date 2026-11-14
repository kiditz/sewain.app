package app.sewain.orders;

import app.sewain.exception.*;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class CartWorkflowTest {
    final CartRepository carts=mock(CartRepository.class);
    final CartItemRepository repository=mock(CartItemRepository.class);
    final CartMapper mapper=Mappers.getMapper(CartMapper.class);
    final CartItemMapper items=Mappers.getMapper(CartItemMapper.class);
    final CartServiceImpl service;
    final CartCommand owner=new CartCommand(null,UUID.randomUUID());
    final OffsetDateTime start=OffsetDateTime.parse("2026-12-01T00:00:00Z");
    CartWorkflowTest() {
        ReflectionTestUtils.setField(items,"rentalPlansMapper",new RentalPlansMapper(new ObjectMapper()));
        ReflectionTestUtils.setField(mapper,"cartItemMapper",items);
        service=new CartServiceImpl(carts,repository,mapper,items);
        when(carts.save(any())).thenAnswer(c->{Cart cart=c.getArgument(0);if(cart.getId()==null)cart.setId(UUID.randomUUID());return cart;});
        when(carts.saveAndFlush(any())).thenAnswer(c->c.getArgument(0));
    }
    CartItemCommand item(OffsetDateTime from,OffsetDateTime to,List<Map<String,Object>> plans,BigDecimal rent,BigDecimal deposit,BigDecimal total) {
        return new CartItemCommand(UUID.randomUUID(),UUID.randomUUID(),2,from,to,plans,rent,deposit,total);
    }
    CartItemCommand valid() {return item(start,start.plusDays(2),List.of(Map.of("durationUnit","DAY")),new BigDecimal("200"),new BigDecimal("50"),new BigDecimal("250"));}
    Cart existing() {
        var cart=new Cart();cart.setId(UUID.randomUUID());cart.setGuestToken(owner.guestToken());
        when(carts.findByGuestTokenAndStatus(owner.guestToken(),CartStatus.ACTIVE)).thenReturn(Optional.of(cart));return cart;
    }
    CartItem stored(Cart cart) {var item=items.toEntity(valid());item.setId(UUID.randomUUID());cart.addItem(item);return item;}

    @Test void firstAddCreatesGuestCartAndAssociatesNewItem() {
        var result=service.addItem(owner,valid());
        var capture=org.mockito.ArgumentCaptor.forClass(Cart.class);verify(carts).saveAndFlush(capture.capture());
        var cart=capture.getValue();assertEquals(owner.guestToken(),cart.getGuestToken());assertNull(cart.getCustomerId());
        assertEquals(CartStatus.ACTIVE,cart.getStatus());assertEquals(1,cart.getItems().size());assertSame(cart,cart.getItems().getFirst().getCart());
        assertEquals(cart.getId(),result.id());verify(repository).findProjectionsByCartId(cart.getId());
    }
    @Test void addToExistingCartDoesNotCreateAnotherCart() {
        var cart=existing();stored(cart);service.addItem(owner,valid());
        assertEquals(2,cart.getItems().size());verify(carts,never()).save(any());
    }
    @Test void updateChangesOnlyOwnedItemAndPreservesIdentity() {
        var cart=existing();var item=stored(cart);var id=item.getId();var replacement=valid();
        service.updateItem(owner,id,replacement);assertEquals(id,item.getId());assertSame(cart,item.getCart());
        assertEquals(replacement.variantId(),item.getVariantId());assertEquals(replacement.rentalPlans(),item.getRentalPlans());
        verify(carts).saveAndFlush(cart);
    }
    @Test void unknownCartOrForeignItemCannotBeChanged() {
        assertThrows(ResourceNotFoundException.class,()->service.updateItem(owner,UUID.randomUUID(),valid()));
        existing();assertThrows(ResourceNotFoundException.class,()->service.updateItem(owner,UUID.randomUUID(),valid()));
        assertThrows(ResourceNotFoundException.class,()->service.removeItem(owner,UUID.randomUUID()));
        verify(carts,never()).saveAndFlush(any());
    }
    @Test void removeAndClearDetachItemsFromCart() {
        var cart=existing();var first=stored(cart);var second=stored(cart);
        service.removeItem(owner,first.getId());assertFalse(cart.getItems().contains(first));assertNull(first.getCart());assertEquals(1,cart.getItems().size());
        service.clearItems(owner);assertTrue(cart.getItems().isEmpty());assertNull(second.getCart());
    }
    @Test void invalidRentalPeriodsAreRejectedBeforeCartCreation() {
        var commands=Arrays.asList(null,item(null,start,List.of(),BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO),
                item(start,null,List.of(),BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO),
                item(start,start,List.of(),BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO),
                item(start,start.minusDays(1),List.of(),BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO));
        for(var command:commands)assertThrows(ValidationException.class,()->service.addItem(owner,command));
        verifyNoInteractions(carts,repository);
    }
    @Test void missingAndNegativeAmountsOrPlansAreRejected() {
        var zero=BigDecimal.ZERO;var negative=BigDecimal.ONE.negate();
        var commands=List.of(item(start,start.plusDays(1),null,zero,zero,zero),
                item(start,start.plusDays(1),List.of(),null,zero,zero),item(start,start.plusDays(1),List.of(),zero,null,zero),
                item(start,start.plusDays(1),List.of(),zero,zero,null),item(start,start.plusDays(1),List.of(),negative,zero,zero),
                item(start,start.plusDays(1),List.of(),zero,negative,zero),item(start,start.plusDays(1),List.of(),zero,zero,negative));
        for(var command:commands)assertThrows(ValidationException.class,()->service.addItem(owner,command));
        verifyNoInteractions(carts,repository);
    }
    @Test void zeroPricedRentalIsAcceptedAndEmptyCartCanBeClearedRepeatedly() {
        var cart=existing();service.addItem(owner,item(start,start.plusDays(1),List.of(),BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO));
        assertEquals(1,cart.getItems().size());service.clearItems(owner);service.clearItems(owner);assertTrue(cart.getItems().isEmpty());
    }
}
