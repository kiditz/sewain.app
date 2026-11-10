package app.sewain.orders;

import app.sewain.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MerchantOrderPaymentServiceTest {
    private final RentalOrderRepository orders=mock(RentalOrderRepository.class);
    private final MerchantOrderMapper mapper=mock(MerchantOrderMapper.class);
    private final PaymentRepository payments=mock(PaymentRepository.class);
    private final PaymentRefundRepository refunds=mock(PaymentRefundRepository.class);
    private final RentalAllocationRepository allocations=mock(RentalAllocationRepository.class);
    private final ApplicationEventPublisher events=mock(ApplicationEventPublisher.class);
    private final MerchantOrderQueryService queries=mock(MerchantOrderQueryService.class);
    private final MerchantOrderPaymentService service=new MerchantOrderPaymentService(orders,mapper,payments,refunds,allocations,events,queries);
    private RentalOrder order;
    @BeforeEach void setup() {
        order=new RentalOrder(); order.setId(UUID.randomUUID()); order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setTotalAmount(new BigDecimal("150")); order.setDepositTotal(new BigDecimal("50"));
        order.getPaymentTermsSnapshot().put("initialAmount","100");
        when(orders.findById(order.getId())).thenReturn(Optional.of(order));
    }
    private Payment payment(PaymentStatus status,String amount) {
        var payment=new Payment(); payment.setId(UUID.randomUUID()); payment.setOrder(order); payment.setStatus(status); payment.setAmount(new BigDecimal(amount)); return payment;
    }
    private PaymentRefund refund(Payment payment,PaymentRefundStatus status,String amount) {
        var refund=new PaymentRefund(); refund.setPayment(payment); refund.setStatus(status); refund.setAmount(new BigDecimal(amount)); return refund;
    }
    @Test void receivedTotalCountsOnlySettledMoneyAndSubtractsSuccessfulRefunds() {
        var settled=payment(PaymentStatus.SUCCEEDED,"100"); var partial=payment(PaymentStatus.PARTIALLY_REFUNDED,"60");
        when(refunds.findAllByPayment_Order_Id(order.getId())).thenReturn(List.of(refund(partial,PaymentRefundStatus.SUCCEEDED,"20"),refund(settled,PaymentRefundStatus.PENDING,"50")));
        var total=service.totalReceived(order.getId(),List.of(settled,partial,payment(PaymentStatus.PENDING,"900"),payment(PaymentStatus.FAILED,"900"),payment(PaymentStatus.REFUNDED,"80")));
        assertEquals(0,new BigDecimal("140").compareTo(total));
    }
    @Test void refundDataCannotMakeReceivedAmountNegative() {
        var paid=payment(PaymentStatus.PARTIALLY_REFUNDED,"100");
        when(refunds.findAllByPayment_Order_Id(order.getId())).thenReturn(List.of(refund(paid,PaymentRefundStatus.SUCCEEDED,"110")));
        assertEquals(0,service.totalReceived(order.getId(),List.of(paid)).signum());
    }
    @Test void closedOrdersRejectNewReceiptsWithoutWritingOrPublishing() {
        for (var status:List.of(OrderStatus.CANCELLED,OrderStatus.EXPIRED,OrderStatus.COMPLETED)) {
            order.setStatus(status);
            assertThrows(ConflictException.class,()->service.recordPayment(order.getId(),new RecordOrderPaymentCommand(PaymentStage.INITIAL,new BigDecimal("100"),PaymentMethod.CASH,null)));
        }
        verifyNoInteractions(payments,refunds,mapper,events);
        verify(orders,never()).save(any());
    }
    @Test void refusesDuplicateInitialPaymentAndIncorrectInitialAmount() {
        var initial=payment(PaymentStatus.SUCCEEDED,"100");
        when(payments.findAllByOrder_Id(order.getId())).thenReturn(List.of(initial));
        assertThrows(ConflictException.class,()->service.recordPayment(order.getId(),new RecordOrderPaymentCommand(PaymentStage.INITIAL,new BigDecimal("100"),PaymentMethod.CASH,null)));
        when(payments.findAllByOrder_Id(order.getId())).thenReturn(List.of());
        assertThrows(ValidationException.class,()->service.recordPayment(order.getId(),new RecordOrderPaymentCommand(PaymentStage.INITIAL,new BigDecimal("99"),PaymentMethod.CASH,null)));
        verifyNoInteractions(mapper,events);
        verify(payments,never()).save(any());
    }
    @Test void confirmsOnlyAfterSuccessfulInitialPaymentAndConvertsOnlyHeldAllocations() {
        var held=new RentalAllocation(); held.setStatus(RentalAllocationStatus.HELD); held.setHoldExpiresAt(java.time.OffsetDateTime.now());
        var released=new RentalAllocation(); released.setStatus(RentalAllocationStatus.RELEASED);
        assertThrows(ValidationException.class,()->service.confirmOrder(order,List.of(payment(PaymentStatus.PENDING,"100"))));
        assertEquals(OrderStatus.PENDING_PAYMENT,order.getStatus());
        when(allocations.findAllByOrderItem_Order_Id(order.getId())).thenReturn(List.of(held,released));
        service.confirmOrder(order,List.of(payment(PaymentStatus.SUCCEEDED,"100")));
        assertEquals(OrderStatus.CONFIRMED,order.getStatus());
        assertEquals(RentalAllocationStatus.CONFIRMED,held.getStatus()); assertNull(held.getHoldExpiresAt());
        assertEquals(RentalAllocationStatus.RELEASED,released.getStatus());
    }
    @Test void depositRefundRequiresCompletedReturn() {
        order.setStatus(OrderStatus.ACTIVE_RENTAL);
        assertThrows(ValidationException.class,()->service.recordDepositRefund(order.getId(),new RecordDepositRefundCommand(BigDecimal.TEN,"Return")));
        verifyNoInteractions(payments,refunds,events);
    }
    @Test void cannotRefundPaymentBelongingToAnotherOrder() {
        var payment=payment(PaymentStatus.SUCCEEDED,"100"); var other=new RentalOrder(); other.setId(UUID.randomUUID()); payment.setOrder(other);
        when(payments.findById(payment.getId())).thenReturn(Optional.of(payment));
        assertThrows(ResourceNotFoundException.class,()->service.recordRefund(order.getId(),payment.getId(),new RecordPaymentRefundCommand(BigDecimal.TEN,"Refund")));
        verifyNoInteractions(refunds,mapper,events);
    }
    @Test void cannotRefundMoreThanRemainingAfterPriorSuccessfulRefunds() {
        var payment=payment(PaymentStatus.PARTIALLY_REFUNDED,"100");
        when(payments.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(refunds.findAllByPayment_Order_Id(order.getId())).thenReturn(List.of(refund(payment,PaymentRefundStatus.SUCCEEDED,"80")));
        assertThrows(ValidationException.class,()->service.recordRefund(order.getId(),payment.getId(),new RecordPaymentRefundCommand(new BigDecimal("21"),"Refund")));
        verifyNoInteractions(mapper,events);
        verify(refunds,never()).saveAndFlush(any());
    }
}
