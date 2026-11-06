package app.sewain.orders;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class CartOwnershipTest {
 @Test void noPublicOperationAcceptsAnAssertedCustomerIdentity(){
  var carts=mock(CartRepository.class);var items=mock(CartItemRepository.class);var service=new CartServiceImpl(carts,items,mock(CartMapper.class),mock(CartItemMapper.class));var forged=new CartCommand(UUID.randomUUID(),null);var item=UUID.randomUUID();
  assertThrows(org.springframework.security.access.AccessDeniedException.class,()->service.getCart(forged));
  assertThrows(org.springframework.security.access.AccessDeniedException.class,()->service.addItem(forged,null));
  assertThrows(org.springframework.security.access.AccessDeniedException.class,()->service.updateItem(forged,item,null));
  assertThrows(org.springframework.security.access.AccessDeniedException.class,()->service.removeItem(forged,item));
  assertThrows(org.springframework.security.access.AccessDeniedException.class,()->service.clearItems(forged));verifyNoInteractions(carts,items);
 }
 @Test void rejectsMissingIdentityAndUsesOnlyTheSuppliedGuestCapability(){
  var carts=mock(CartRepository.class);var items=mock(CartItemRepository.class);var service=new CartServiceImpl(carts,items,mock(CartMapper.class),mock(CartItemMapper.class));
  assertThrows(app.sewain.exception.ValidationException.class,()->service.getCart(new CartCommand(null,null)));verifyNoInteractions(carts);
  var token=UUID.randomUUID();when(carts.findByGuestTokenAndStatus(token,CartStatus.ACTIVE)).thenReturn(Optional.empty());
  service.getCart(new CartCommand(null,token));verify(carts).findByGuestTokenAndStatus(token,CartStatus.ACTIVE);verify(carts,never()).findByGuestTokenAndStatus(isNull(),any());
 }
}
