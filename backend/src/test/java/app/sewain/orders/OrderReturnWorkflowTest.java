package app.sewain.orders;

import app.sewain.exception.*;
import app.sewain.shared.InventoryUnitStatusChangedEvent;
import org.junit.jupiter.api.*;
import org.mapstruct.factory.Mappers;
import org.springframework.context.ApplicationEventPublisher;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class OrderReturnWorkflowTest {
    final RentalOrderRepository orders=mock(RentalOrderRepository.class);
    final OrderItemRepository items=mock(OrderItemRepository.class);
    final PaymentRepository payments=mock(PaymentRepository.class);
    final MerchantOrderLateFeeService fees=mock(MerchantOrderLateFeeService.class);
    final RentalAllocationRepository allocations=mock(RentalAllocationRepository.class);
    final ApplicationEventPublisher events=mock(ApplicationEventPublisher.class);
    final MerchantOrderQueryService queries=mock(MerchantOrderQueryService.class);
    final MerchantOrderPaymentService receipts=mock(MerchantOrderPaymentService.class);
    final MerchantOrderLifecycleService service=new MerchantOrderLifecycleService(orders,Mappers.getMapper(MerchantOrderMapper.class),items,payments,fees,allocations,events,queries,receipts);
    final RentalOrder order=new RentalOrder();
    final OffsetDateTime returned=OffsetDateTime.now().minusHours(1);
    @BeforeEach void setup() {
        order.setId(UUID.randomUUID());order.setStatus(OrderStatus.ACTIVE_RENTAL);order.setTotalAmount(new BigDecimal("150"));
        when(orders.findById(order.getId())).thenReturn(Optional.of(order));
        when(receipts.totalReceived(eq(order.getId()),anyList())).thenReturn(BigDecimal.ZERO);
        quote(true,"20");
    }
    void quote(boolean enabled,String amount) {
        when(fees.calculate(eq(order),anyList(),any())).thenReturn(new LateFeeCalculator.Quote(enabled,120,60,LateFeeCalculator.Unit.DAY,new BigDecimal(amount),1,new BigDecimal(amount)));
    }
    void record(boolean waived,String reason) {service.recordReturn(order.getId(),new RecordOrderReturnCommand(returned,waived,reason));}
    void amount(String expected,BigDecimal actual){assertEquals(0,new BigDecimal(expected).compareTo(actual));}

    @Test void recordingReturnPersistsQuoteAndDoesNotCompleteOrderAutomatically() {
        record(false,null);assertEquals(returned,order.getReturnedAt());assertEquals(OrderStatus.ACTIVE_RENTAL,order.getStatus());
        amount("170",order.getTotalAmount());amount("20",order.getLateFeeTotal());amount("20",order.getLateFeeAssessed());
        assertEquals(120,order.getLateMinutes());assertEquals("DAY",order.getLateFeeUnit());assertEquals(1,order.getLateFeeUnits());
        assertFalse(order.isLateFeeWaived());assertNull(order.getLateFeeWaiverReason());verify(orders).save(order);verify(queries).get(order.getId());
    }
    @Test void recalculationReplacesPreviousLateFeeInsteadOfAddingTwice() {
        order.setLateFeeTotal(BigDecimal.TEN);record(false,null);amount("160",order.getTotalAmount());amount("20",order.getLateFeeTotal());
    }
    @Test void waiverRecordsAssessedAmountButChargesZeroAndTrimsReason() {
        order.setLateFeeTotal(BigDecimal.TEN);record(true," Loyal customer ");
        amount("140",order.getTotalAmount());amount("0",order.getLateFeeTotal());amount("20",order.getLateFeeAssessed());
        assertTrue(order.isLateFeeWaived());assertEquals("Loyal customer",order.getLateFeeWaiverReason());
    }
    @Test void waiverRequiresReasonAndActualFee() {
        for(String reason:Arrays.asList(null,""," ")) assertThrows(ValidationException.class,()->record(true,reason));
        assertThrows(ValidationException.class,()->record(false,"Unrequested waiver"));
        quote(true,"0");assertThrows(ValidationException.class,()->record(true,"Nothing to waive"));verify(orders,never()).save(any());
    }
    @Test void reducedTotalCannotFallBelowMoneyAlreadyReceived() {
        order.setLateFeeTotal(BigDecimal.TEN);when(receipts.totalReceived(eq(order.getId()),anyList())).thenReturn(new BigDecimal("141"));
        assertThrows(ValidationException.class,()->record(true,"Waiver"));amount("150",order.getTotalAmount());assertNull(order.getReturnedAt());
    }
    @Test void disabledLateFeePolicyDoesNotRecordBillingUnit() {
        quote(false,"0");record(false,null);assertNull(order.getLateFeeUnit());amount("150",order.getTotalAmount());
    }
    @Test void returnTimestampCannotBeBeforeRentalOrTooFarInFuture() {
        var item=new OrderItem();item.setRentalStartAt(returned);when(items.findAllByOrder_IdOrderByCreatedAtAsc(order.getId())).thenReturn(List.of(item));
        assertThrows(ValidationException.class,()->service.recordReturn(order.getId(),new RecordOrderReturnCommand(returned.minusSeconds(1),false,null)));
        assertThrows(ValidationException.class,()->service.recordReturn(order.getId(),new RecordOrderReturnCommand(OffsetDateTime.now().plusMinutes(10),false,null)));
        verifyNoInteractions(fees);
    }
    @Test void omittedReturnTimeDefaultsToNow() {
        var before=OffsetDateTime.now();service.recordReturn(order.getId(),new RecordOrderReturnCommand(null,false,null));
        assertFalse(order.getReturnedAt().isBefore(before));assertFalse(order.getReturnedAt().isAfter(OffsetDateTime.now()));
    }
    @Test void onlyActiveRentalCanRecordReturn() {
        for(var status:OrderStatus.values())if(status!=OrderStatus.ACTIVE_RENTAL){order.setStatus(status);assertThrows(ConflictException.class,()->record(false,null));}
        verify(orders,never()).save(any());
    }
    @Test void completingWithoutRecordedReturnIsAllowedOnlyWhenNoFeeIsDue() {
        assertThrows(ValidationException.class,()->service.updateStatus(order.getId(),OrderStatus.COMPLETED));
        quote(true,"0");service.updateStatus(order.getId(),OrderStatus.COMPLETED);
        assertEquals(OrderStatus.COMPLETED,order.getStatus());assertNotNull(order.getReturnedAt());assertNotNull(order.getCompletedAt());
    }
    @Test void completionWithLateFeeRequiresPaymentValidation() {
        order.setReturnedAt(returned);order.setLateFeeTotal(BigDecimal.TEN);
        doThrow(new ValidationException("Unpaid fee")).when(receipts).validateRentalAmountPaid(eq(order),anyList());
        assertThrows(ValidationException.class,()->service.updateStatus(order.getId(),OrderStatus.COMPLETED));
        assertEquals(OrderStatus.ACTIVE_RENTAL,order.getStatus());verify(allocations,never()).saveAll(any());
    }
    @Test void returnDueBalanceIsCheckedBeforeCompleting() {
        order.setReturnedAt(returned);order.setBalanceDueAt(BalanceDueAt.RETURN);
        service.updateStatus(order.getId(),OrderStatus.COMPLETED);verify(receipts).validateRentalAmountPaid(eq(order),anyList());
    }
    @Test void processingAndReadyTransitionsDoNotChangeInventoryAvailability() {
        order.setStatus(OrderStatus.CONFIRMED);service.updateStatus(order.getId(),OrderStatus.PROCESSING);
        service.updateStatus(order.getId(),OrderStatus.READY);assertEquals(OrderStatus.READY,order.getStatus());
        verify(events,never()).publishEvent(any(InventoryUnitStatusChangedEvent.class));
    }
    @Test void terminalOrdersRejectEveryFurtherStatusChange() {
        for(var terminal:List.of(OrderStatus.COMPLETED,OrderStatus.CANCELLED,OrderStatus.EXPIRED)) {
            order.setStatus(terminal);for(var next:OrderStatus.values())assertThrows(ConflictException.class,()->service.updateStatus(order.getId(),next));
        }
        verify(orders,never()).save(any());
    }
    @Test void unknownOrderCannotRecordReturnOrChangeStatus() {
        var id=UUID.randomUUID();assertThrows(ResourceNotFoundException.class,()->service.recordReturn(id,new RecordOrderReturnCommand(null,false,null)));
        assertThrows(ResourceNotFoundException.class,()->service.updateStatus(id,OrderStatus.PROCESSING));
    }
}
