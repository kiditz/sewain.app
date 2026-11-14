package app.sewain.orders;

import app.sewain.catalogs.OrderCatalogService;
import app.sewain.exception.ValidationException;
import app.sewain.settings.*;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class MerchantOrderCreationServiceTest {
    final RentalOrderRepository orders=mock(RentalOrderRepository.class);
    final OrderItemRepository items=mock(OrderItemRepository.class);
    final RentalAllocationRepository allocations=mock(RentalAllocationRepository.class);
    final PaymentRepository payments=mock(PaymentRepository.class);
    final OrderCatalogService catalog=mock(OrderCatalogService.class);
    final SettingFacade settings=mock(SettingFacade.class);
    final ApplicationEventPublisher events=mock(ApplicationEventPublisher.class);
    final ObjectMapper json=new ObjectMapper();
    final UUID product=UUID.randomUUID(),variant=UUID.randomUUID();
    final OffsetDateTime start=OffsetDateTime.now(ZoneOffset.UTC).plusDays(7).withHour(12),end=start.plusDays(2);
    final MerchantOrderCreationService service=new MerchantOrderCreationService(orders,Mappers.getMapper(MerchantOrderMapper.class),
            items,catalog,settings,json,allocations,events,payments);
    MerchantOrderCreationServiceTest() {
        setting(SettingKey.PAYMENT,"{\"fullPaymentEnabled\":true,\"downPaymentEnabled\":true,\"downPaymentType\":\"percentage\",\"downPaymentValue\":30,\"cashAtPickup\":true,\"cashAtReturn\":true,\"bankTransfer\":true}");
        setting(SettingKey.PAYMENT_BANK_ACCOUNTS,"{\"accounts\":[{\"bank\":\"Test\"}]}");
        setting(SettingKey.REGIONAL,"{\"timezone\":\"UTC\",\"currency\":\"IDR\"}");
        setting(SettingKey.RENTAL,"{\"bufferHours\":2,\"minimumNoticeHours\":0,\"maximumAdvanceDays\":90,\"operatingDays\":[\"MONDAY\",\"TUESDAY\",\"WEDNESDAY\",\"THURSDAY\",\"FRIDAY\",\"SATURDAY\",\"SUNDAY\"],\"openingTime\":\"00:00\",\"closingTime\":\"23:59\"}");
        setting(SettingKey.RENTAL_PLAN,"[]");
        when(catalog.getPricing(eq(variant),any(),any(),any())).thenReturn(new OrderCatalogService.VariantPricing(
                product,variant,"Tent","Large","TENT",new BigDecimal("50"),new BigDecimal("200"),2,Map.of("totalPrice",200)));
        when(catalog.reserveUnits(eq(variant),any(),any(),eq(2),eq(2))).thenReturn(List.of(UUID.randomUUID(),UUID.randomUUID()));
        when(orders.saveAndFlush(any())).thenAnswer(call->{RentalOrder o=call.getArgument(0);o.setId(UUID.randomUUID());return o;});
        when(items.saveAndFlush(any())).thenAnswer(call->{OrderItem i=call.getArgument(0);i.setId(UUID.randomUUID());return i;});
    }
    void setting(SettingKey key,String value) { when(settings.getValue(key)).thenReturn(json.readTree(value)); }
    MerchantOrderCommand command(InitialPaymentChoice choice,boolean waive,String reason) {
        return new MerchantOrderCommand("Customer","OWNER@EXAMPLE.INVALID",null,FulfillmentType.PICKUP,null,start,null,end,
                PaymentMethod.CASH,choice,choice==InitialPaymentChoice.DOWN_PAYMENT?BalanceDueAt.PICKUP:null,
                BigDecimal.TEN,waive,reason,null,List.of(new MerchantOrderItemCommand(product,variant,2,start,end)));
    }
    RentalOrder stored() { var capture=org.mockito.ArgumentCaptor.forClass(RentalOrder.class); verify(orders).saveAndFlush(capture.capture());return capture.getValue(); }
    @Test void fullPaymentIncludesQuantityDepositDeliveryAndBufferedAllocations() {
        var result=service.create(command(InitialPaymentChoice.FULL_PAYMENT,false,null));
        assertEquals(OrderStatus.PENDING_PAYMENT,result.status()); amount("510",result.totalAmount());
        var order=stored(); assertEquals("owner@example.invalid",order.getCustomerEmail()); amount("100",order.getDepositTotal());
        assertEquals(new BigDecimal("510"),order.getPaymentTermsSnapshot().get("initialAmount"));
        var capture=org.mockito.ArgumentCaptor.forClass(RentalAllocation.class); verify(allocations,times(2)).save(capture.capture());
        capture.getAllValues().forEach(a->{assertEquals(end.plusHours(2),a.getRentalEndAt());assertEquals(RentalAllocationStatus.HELD,a.getStatus());assertNotNull(a.getHoldExpiresAt());});
        verify(payments).save(any(Payment.class));
    }
    @Test void percentageDownPaymentUsesOrderTotalAndRetainsBalanceDueAt() {
        service.create(command(InitialPaymentChoice.DOWN_PAYMENT,false,null)); var order=stored();
        amount("153",(BigDecimal)order.getPaymentTermsSnapshot().get("initialAmount")); assertEquals(BalanceDueAt.PICKUP,order.getBalanceDueAt());
    }
    @Test void fixedDownPaymentIsCappedAtInitialBase() {
        setting(SettingKey.PAYMENT,"{\"downPaymentEnabled\":true,\"downPaymentType\":\"fixed\",\"downPaymentValue\":9999,\"cashAtPickup\":true}");
        service.create(command(InitialPaymentChoice.DOWN_PAYMENT,false,null)); amount("510",(BigDecimal)stored().getPaymentTermsSnapshot().get("initialAmount"));
    }
    @Test void waiverRemovesDepositAndTrimsReason() {
        service.create(command(InitialPaymentChoice.FULL_PAYMENT,true," Loyal customer ")); var order=stored();
        amount("410",order.getTotalAmount()); amount("0",order.getDepositTotal()); assertEquals("Loyal customer",order.getDepositWaiverReason());
    }
    @Test void waiverRequiresReasonAndReasonRequiresWaiver() {
        assertThrows(ValidationException.class,()->service.create(command(InitialPaymentChoice.FULL_PAYMENT,true," ")));
        assertThrows(ValidationException.class,()->service.create(command(InitialPaymentChoice.FULL_PAYMENT,false,"Reason")));
        verify(orders,never()).saveAndFlush(any());
    }
    @Test void disabledFullAndDownPaymentChoicesAreRejected() {
        setting(SettingKey.PAYMENT,"{}");
        assertThrows(ValidationException.class,()->service.create(command(InitialPaymentChoice.FULL_PAYMENT,false,null)));
        assertThrows(ValidationException.class,()->service.create(command(InitialPaymentChoice.DOWN_PAYMENT,false,null)));
        verify(orders,never()).saveAndFlush(any());
    }
    @Test void closedDayAndOutsideHoursAreRejectedBeforePricing() {
        setting(SettingKey.RENTAL,"{\"operatingDays\":[]}");
        assertThrows(ValidationException.class,()->service.create(command(InitialPaymentChoice.FULL_PAYMENT,false,null)));
        setting(SettingKey.RENTAL,"{\"operatingDays\":[\""+start.getDayOfWeek().name()+"\"],\"openingTime\":\"23:00\",\"closingTime\":\"23:59\"}");
        assertThrows(ValidationException.class,()->service.create(command(InitialPaymentChoice.FULL_PAYMENT,false,null)));
        verifyNoInteractions(catalog);
    }
    @Test void minimumNoticeAndMaximumAdvanceAreEnforced() {
        setting(SettingKey.RENTAL,"{\"minimumNoticeHours\":10000}");
        assertThrows(ValidationException.class,()->service.create(command(InitialPaymentChoice.FULL_PAYMENT,false,null)));
        setting(SettingKey.RENTAL,"{\"maximumAdvanceDays\":1}");
        assertThrows(ValidationException.class,()->service.create(command(InitialPaymentChoice.FULL_PAYMENT,false,null)));
        verify(orders,never()).saveAndFlush(any());
    }
    @Test void productVariantMismatchNeverPersistsOrder() {
        when(catalog.getPricing(eq(variant),any(),any(),any())).thenReturn(new OrderCatalogService.VariantPricing(
                UUID.randomUUID(),variant,"Wrong","Large","SKU",BigDecimal.ZERO,BigDecimal.TEN,2,Map.of()));
        assertThrows(ValidationException.class,()->service.create(command(InitialPaymentChoice.FULL_PAYMENT,false,null)));
        verify(orders,never()).saveAndFlush(any());
    }
    void amount(String expected,BigDecimal actual) { assertEquals(0,new BigDecimal(expected).compareTo(actual)); }
}
