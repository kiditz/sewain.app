package app.sewain.orders;

import app.sewain.exception.*;
import app.sewain.shared.OrderPaymentReceivedEvent;
import org.junit.jupiter.api.*;
import org.mapstruct.factory.Mappers;
import org.springframework.context.ApplicationEventPublisher;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class PaymentWorkflowTest {
    final RentalOrderRepository orders=mock(RentalOrderRepository.class);
    final PaymentRepository payments=mock(PaymentRepository.class);
    final PaymentRefundRepository refunds=mock(PaymentRefundRepository.class);
    final RentalAllocationRepository allocations=mock(RentalAllocationRepository.class);
    final ApplicationEventPublisher events=mock(ApplicationEventPublisher.class);
    final MerchantOrderQueryService queries=mock(MerchantOrderQueryService.class);
    final MerchantOrderPaymentService service=new MerchantOrderPaymentService(orders,Mappers.getMapper(MerchantOrderMapper.class),payments,refunds,allocations,events,queries);
    final RentalOrder order=new RentalOrder();
    final List<Payment> receipts=new ArrayList<>();
    @BeforeEach void setup() {
        order.setId(UUID.randomUUID()); order.setOrderNumber("SW-TEST"); order.setStatus(OrderStatus.CONFIRMED);
        order.setTotalAmount(new BigDecimal("150")); order.setDepositTotal(new BigDecimal("50"));
        order.getPaymentTermsSnapshot().put("initialAmount",100);
        when(orders.findById(order.getId())).thenReturn(Optional.of(order));
        when(payments.findAllByOrder_Id(order.getId())).thenReturn(receipts);
        when(payments.saveAndFlush(any())).thenAnswer(c->{Payment p=c.getArgument(0);if(p.getId()==null)p.setId(UUID.randomUUID());return p;});
        when(refunds.saveAndFlush(any())).thenAnswer(c->{PaymentRefund r=c.getArgument(0);r.setId(UUID.randomUUID());return r;});
    }
    Payment receipt(PaymentStage stage,PaymentStatus status,String total,String rental,String deposit) {
        var p=new Payment();p.setId(UUID.randomUUID());p.setOrder(order);p.setPaymentStage(stage);p.setStatus(status);
        p.setAmount(new BigDecimal(total));p.setRentalAmount(new BigDecimal(rental));p.setDepositAmount(new BigDecimal(deposit));
        p.setPaidAt(OffsetDateTime.now()); receipts.add(p); when(payments.findById(p.getId())).thenReturn(Optional.of(p)); return p;
    }
    PaymentRefund refund(Payment p,String total,String rental,String deposit,PaymentRefundStatus status) {
        var r=new PaymentRefund();r.setPayment(p);r.setStatus(status);r.setAmount(new BigDecimal(total));
        r.setRentalAmount(new BigDecimal(rental));r.setDepositAmount(new BigDecimal(deposit));return r;
    }
    void pay(PaymentStage stage,String amount,String reference) { service.recordPayment(order.getId(),new RecordOrderPaymentCommand(stage,new BigDecimal(amount),PaymentMethod.CASH,reference)); }
    Payment saved() {var c=org.mockito.ArgumentCaptor.forClass(Payment.class);verify(payments).saveAndFlush(c.capture());return c.getValue();}
    void amount(String expected,BigDecimal actual) {assertEquals(0,new BigDecimal(expected).compareTo(actual));}

    @Test void initialReceiptConfirmsOrderAndHeldAllocationsAndNormalizesReference() {
        order.setStatus(OrderStatus.PENDING_PAYMENT); var held=new RentalAllocation();held.setStatus(RentalAllocationStatus.HELD);held.setHoldExpiresAt(OffsetDateTime.now());
        when(allocations.findAllByOrderItem_Order_Id(order.getId())).thenReturn(List.of(held));
        pay(PaymentStage.INITIAL,"100"," RECEIPT ");var p=saved();
        assertEquals(PaymentType.ORDER,p.getPaymentType());assertEquals("RECEIPT",p.getProviderPaymentId());
        amount("100",p.getRentalAmount());amount("0",p.getDepositAmount());assertEquals(OrderStatus.CONFIRMED,order.getStatus());
        assertEquals(RentalAllocationStatus.CONFIRMED,held.getStatus());assertNull(held.getHoldExpiresAt());
        verify(events).publishEvent(any(OrderPaymentReceivedEvent.class));verify(queries).get(order.getId());
    }
    @Test void existingPendingReceiptIsConfirmedInsteadOfCreatingAnotherReceipt() {
        var pending=receipt(PaymentStage.INITIAL,PaymentStatus.PENDING,"100","0","0");
        when(payments.findFirstByOrder_IdAndPaymentStageAndStatusOrderByCreatedAtAsc(order.getId(),PaymentStage.INITIAL,PaymentStatus.PENDING)).thenReturn(Optional.of(pending));
        pay(PaymentStage.INITIAL,"100"," ");assertSame(pending,saved());assertEquals(PaymentStatus.SUCCEEDED,pending.getStatus());
        assertNotNull(pending.getPaidAt());assertNull(pending.getProviderPaymentId());
    }
    @Test void depositReceiptAllocatesOnlyDepositAndUsesDepositType() {
        pay(PaymentStage.DEPOSIT,"50",null);var p=saved();amount("0",p.getRentalAmount());amount("50",p.getDepositAmount());assertEquals(PaymentType.DEPOSIT,p.getPaymentType());
    }
    @Test void separateDepositKeepsInitialReceiptInRentalAllocation() {
        order.getPaymentTermsSnapshot().put("payment",Map.of("separateDeposit",true));
        pay(PaymentStage.INITIAL,"100",null);amount("100",saved().getRentalAmount());amount("0",saved().getDepositAmount());
    }
    @Test void combinedReceiptAllocatesRentalFirstThenDeposit() {
        order.getPaymentTermsSnapshot().put("initialAmount",150);pay(PaymentStage.INITIAL,"150",null);
        amount("100",saved().getRentalAmount());amount("50",saved().getDepositAmount());
    }
    @Test void balanceUsesNetRentalAfterSuccessfulRefunds() {
        var prior=receipt(PaymentStage.INITIAL,PaymentStatus.PARTIALLY_REFUNDED,"80","80","0");
        when(refunds.findAllByPayment_Order_Id(order.getId())).thenReturn(List.of(refund(prior,"20","20","0",PaymentRefundStatus.SUCCEEDED)));
        pay(PaymentStage.BALANCE,"40",null);var p=saved();amount("40",p.getRentalAmount());assertEquals(PaymentType.RENTAL,p.getPaymentType());
    }
    @Test void overpaidBalanceAndIncorrectDepositAreRejected() {
        receipt(PaymentStage.INITIAL,PaymentStatus.SUCCEEDED,"80","80","0");
        assertThrows(ValidationException.class,()->pay(PaymentStage.BALANCE,"21",null));
        assertThrows(ValidationException.class,()->pay(PaymentStage.DEPOSIT,"49",null));verify(payments,never()).saveAndFlush(any());
    }
    @Test void lateFeePaymentSubtractsSuccessfulRefundsAndIgnoresFailedReceipts() {
        order.setLateFeeTotal(new BigDecimal("30"));
        var late=receipt(PaymentStage.LATE_FEE,PaymentStatus.PARTIALLY_REFUNDED,"20","20","0");
        receipt(PaymentStage.LATE_FEE,PaymentStatus.FAILED,"900","0","0");
        when(refunds.findAllByPayment_Order_Id(order.getId())).thenReturn(List.of(refund(late,"5","5","0",PaymentRefundStatus.SUCCEEDED)));
        assertThrows(ValidationException.class,()->pay(PaymentStage.LATE_FEE,"16",null));
        pay(PaymentStage.LATE_FEE,"15",null);assertEquals(PaymentType.RENTAL,saved().getPaymentType());
    }
    @Test void initialWithoutSnapshotAndReceiptBeyondOrderTotalAreRejected() {
        order.getPaymentTermsSnapshot().remove("initialAmount");assertThrows(ValidationException.class,()->pay(PaymentStage.INITIAL,"100",null));
        receipt(PaymentStage.INITIAL,PaymentStatus.SUCCEEDED,"150","100","50");
        assertThrows(ValidationException.class,()->pay(PaymentStage.DEPOSIT,"50",null));
    }
    @Test void decreasingPendingReceiptAmountIsRejected() {
        var pending=receipt(PaymentStage.INITIAL,PaymentStatus.PENDING,"110","0","0");
        when(payments.findFirstByOrder_IdAndPaymentStageAndStatusOrderByCreatedAtAsc(order.getId(),PaymentStage.INITIAL,PaymentStatus.PENDING)).thenReturn(Optional.of(pending));
        assertThrows(ValidationException.class,()->pay(PaymentStage.INITIAL,"100",null));assertEquals(PaymentStatus.PENDING,pending.getStatus());
    }
    @Test void partialRefundSplitsMoneyProportionallyAndFullRemainderClosesPayment() {
        var paid=receipt(PaymentStage.INITIAL,PaymentStatus.SUCCEEDED,"150","100","50");
        service.recordRefund(order.getId(),paid.getId(),new RecordPaymentRefundCommand(new BigDecimal("30"),"Partial"));
        var captured=org.mockito.ArgumentCaptor.forClass(PaymentRefund.class);verify(refunds).saveAndFlush(captured.capture());
        amount("20",captured.getValue().getRentalAmount());amount("10",captured.getValue().getDepositAmount());assertEquals(PaymentStatus.PARTIALLY_REFUNDED,paid.getStatus());
        when(refunds.findAllByPayment_Order_Id(order.getId())).thenReturn(List.of(captured.getValue()));
        service.recordRefund(order.getId(),paid.getId(),new RecordPaymentRefundCommand(new BigDecimal("120"),"Remainder"));
        assertEquals(PaymentStatus.REFUNDED,paid.getStatus());verify(refunds,times(2)).saveAndFlush(captured.capture());
        amount("80",captured.getValue().getRentalAmount());amount("40",captured.getValue().getDepositAmount());
    }
    @Test void refundRejectsNullZeroNegativeAndUnsettledPayment() {
        var paid=receipt(PaymentStage.INITIAL,PaymentStatus.SUCCEEDED,"150","100","50");
        for(BigDecimal value:Arrays.asList(null,BigDecimal.ZERO,BigDecimal.ONE.negate()))
            assertThrows(ValidationException.class,()->service.recordRefund(order.getId(),paid.getId(),new RecordPaymentRefundCommand(value,"Invalid")));
        paid.setStatus(PaymentStatus.PENDING);assertThrows(ConflictException.class,()->service.recordRefund(order.getId(),paid.getId(),new RecordPaymentRefundCommand(BigDecimal.ONE,"Invalid")));
    }
    @Test void depositRefundUsesOldestReceiptFirstAndDoesNotRefundRentalMoney() {
        order.setStatus(OrderStatus.COMPLETED);
        var first=receipt(PaymentStage.DEPOSIT,PaymentStatus.SUCCEEDED,"20","0","20");first.setPaidAt(OffsetDateTime.now().minusDays(1));
        var second=receipt(PaymentStage.INITIAL,PaymentStatus.SUCCEEDED,"130","100","30");
        receipt(PaymentStage.DEPOSIT,PaymentStatus.PENDING,"500","0","500");
        service.recordDepositRefund(order.getId(),new RecordDepositRefundCommand(new BigDecimal("35"),"Returned"));
        var captured=org.mockito.ArgumentCaptor.forClass(PaymentRefund.class);verify(refunds,times(2)).saveAndFlush(captured.capture());
        var generated=captured.getAllValues();assertSame(first,generated.get(0).getPayment());assertSame(second,generated.get(1).getPayment());
        amount("20",generated.get(0).getDepositAmount());amount("15",generated.get(1).getDepositAmount());
        generated.forEach(r->amount("0",r.getRentalAmount()));assertEquals(PaymentStatus.REFUNDED,first.getStatus());assertEquals(PaymentStatus.PARTIALLY_REFUNDED,second.getStatus());
    }
    @Test void priorDepositRefundsReduceAvailableFundsAndInvalidRequestsAreRejected() {
        order.setStatus(OrderStatus.COMPLETED);var paid=receipt(PaymentStage.DEPOSIT,PaymentStatus.PARTIALLY_REFUNDED,"50","0","50");
        when(refunds.findAllByPayment_Order_Id(order.getId())).thenReturn(List.of(refund(paid,"40","0","40",PaymentRefundStatus.SUCCEEDED),refund(paid,"5","0","5",PaymentRefundStatus.FAILED)));
        for(BigDecimal value:Arrays.asList(null,BigDecimal.ZERO,BigDecimal.ONE.negate(),new BigDecimal("11")))
            assertThrows(ValidationException.class,()->service.recordDepositRefund(order.getId(),new RecordDepositRefundCommand(value,"Invalid")));
        service.recordDepositRefund(order.getId(),new RecordDepositRefundCommand(BigDecimal.TEN,"Remaining"));assertEquals(PaymentStatus.REFUNDED,paid.getStatus());
    }
    @Test void rentalBalanceValidationSubtractsRefunds() {
        var paid=receipt(PaymentStage.INITIAL,PaymentStatus.SUCCEEDED,"100","100","0");service.validateRentalAmountPaid(order,receipts);
        when(refunds.findAllByPayment_Order_Id(order.getId())).thenReturn(List.of(refund(paid,"10","10","0",PaymentRefundStatus.SUCCEEDED)));
        assertThrows(ValidationException.class,()->service.validateRentalAmountPaid(order,receipts));
    }
    @Test void unknownOrderAndPaymentReturnNotFound() {
        assertThrows(ResourceNotFoundException.class,()->service.recordPayment(UUID.randomUUID(),new RecordOrderPaymentCommand(PaymentStage.INITIAL,BigDecimal.ONE,PaymentMethod.CASH,null)));
        assertThrows(ResourceNotFoundException.class,()->service.recordRefund(order.getId(),UUID.randomUUID(),new RecordPaymentRefundCommand(BigDecimal.ONE,"Unknown")));
    }
}
