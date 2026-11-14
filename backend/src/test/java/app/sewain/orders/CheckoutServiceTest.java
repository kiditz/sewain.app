package app.sewain.orders;

import app.sewain.exception.*;
import app.sewain.settings.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class CheckoutServiceTest {
    final CartService carts=mock(CartService.class);
    final MerchantOrderCommandService orders=mock(MerchantOrderCommandService.class);
    final SettingFacade settings=mock(SettingFacade.class);
    final ObjectMapper json=new ObjectMapper();
    final CheckoutServiceImpl service=new CheckoutServiceImpl(carts,orders,settings,json);
    final UUID guest=UUID.randomUUID();
    final OffsetDateTime start=OffsetDateTime.parse("2026-12-01T10:00:00Z");
    CheckoutCommand command(FulfillmentType type,String address) {
        return new CheckoutCommand("Customer","customer@example.invalid","08123",type,address,
                PaymentMethod.CASH,InitialPaymentChoice.FULL_PAYMENT,null,"Notes");
    }
    CartItemResponse item(OffsetDateTime from,OffsetDateTime to) {
        return new CartItemResponse(UUID.randomUUID(),UUID.randomUUID(),"Tent","tent",List.of(),UUID.randomUUID(),"Large",2,
                from,to,List.of(),null,null,null,null,null);
    }
    void cart(List<CartItemResponse> items) {
        when(carts.getCart(any())).thenReturn(new CartResponse(UUID.randomUUID(),null,guest,"ACTIVE",null,items,null,null));
    }
    @Test void checkoutRequiresGuestAndNonEmptyCart() {
        assertThrows(ValidationException.class,()->service.checkout(null,command(FulfillmentType.PICKUP,null)));
        verifyNoInteractions(carts); cart(List.of());
        assertThrows(ValidationException.class,()->service.checkout(guest,command(FulfillmentType.PICKUP,null)));
        verifyNoInteractions(orders);
    }
    @Test void pickupUsesEarliestStartLatestEndAndClearsCartOnlyAfterOrderCreation() {
        var first=item(start,start.plusDays(1)); var second=item(start.minusHours(2),start.plusDays(3)); cart(List.of(first,second));
        var result=service.checkout(guest,command(FulfillmentType.PICKUP,null));
        var captured=org.mockito.ArgumentCaptor.forClass(MerchantOrderCommand.class); verify(orders).create(captured.capture());
        var order=captured.getValue(); assertEquals(start.minusHours(2),order.pickupAt()); assertEquals(start.plusDays(3),order.returnAt());
        assertNull(order.deliveryAt()); assertNull(order.fulfillmentAddress()); assertEquals(2,order.items().size());
        assertEquals(first.variantId(),order.items().getFirst().variantId()); assertEquals(2,order.items().getFirst().quantity());
        var sequence=inOrder(orders,carts); sequence.verify(orders).create(any()); sequence.verify(carts).clearItems(any());
    }
    @Test void deliveryTrimsAddressAndUsesDeliveryDate() {
        cart(List.of(item(start,start.plusDays(1)))); service.checkout(guest,command(FulfillmentType.DELIVERY," Main street "));
        var captured=org.mockito.ArgumentCaptor.forClass(MerchantOrderCommand.class); verify(orders).create(captured.capture());
        assertEquals(Map.of("address","Main street"),captured.getValue().fulfillmentAddress());
        assertEquals(start,captured.getValue().deliveryAt()); assertNull(captured.getValue().pickupAt());
    }
    @Test void deliveryRequiresAddressAndOrderFailureKeepsCart() {
        cart(List.of(item(start,start.plusDays(1))));
        for(String address:Arrays.asList(null,""," ")) assertThrows(ValidationException.class,()->service.checkout(guest,command(FulfillmentType.DELIVERY,address)));
        when(orders.create(any())).thenThrow(new ConflictException("Inventory unavailable"));
        assertThrows(ConflictException.class,()->service.checkout(guest,command(FulfillmentType.PICKUP,null)));
        verify(carts,never()).clearItems(any());
    }
    @Test void bankTransferRequiresEnabledFlagAndActualAccount() {
        when(settings.getValue(SettingKey.PAYMENT)).thenReturn(json.readTree("{\"bankTransfer\":true,\"downPaymentValue\":30,\"cashAtPickup\":true}"));
        when(settings.getValue(SettingKey.REGIONAL)).thenReturn(json.readTree("{\"currency\":\"JPY\"}"));
        when(settings.getValue(SettingKey.PAYMENT_BANK_ACCOUNTS)).thenReturn(json.readTree("{\"accounts\":[]}"));
        assertFalse(service.paymentOptions().bankTransferEnabled());
        when(settings.getValue(SettingKey.PAYMENT_BANK_ACCOUNTS)).thenReturn(json.readTree("{\"accounts\":[{\"bank\":\"Test Bank\"}]}"));
        var options=service.paymentOptions(); assertTrue(options.bankTransferEnabled()); assertTrue(options.cashAtPickupEnabled());
        assertEquals("JPY",options.currency()); assertEquals(1,options.bankAccounts().size());
        when(settings.getValue(SettingKey.PAYMENT)).thenReturn(json.readTree("{\"bankTransfer\":false,\"downPaymentValue\":0}"));
        assertFalse(service.paymentOptions().bankTransferEnabled());
    }
}
