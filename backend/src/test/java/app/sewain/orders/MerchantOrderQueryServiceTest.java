package app.sewain.orders;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MerchantOrderQueryServiceTest {
    private final RentalOrderRepository orders = mock(RentalOrderRepository.class);
    private final PaymentRepository payments = mock(PaymentRepository.class);
    private final PaymentRefundRepository refunds = mock(PaymentRefundRepository.class);
    private final OrderItemRepository items = mock(OrderItemRepository.class);
    private final MerchantOrderQueryServiceImpl service = new MerchantOrderQueryServiceImpl(
            orders, Mappers.getMapper(MerchantOrderMapper.class), items, payments, refunds,
            mock(MerchantOrderLateFeeService.class), mock(MerchantOrderActivityRepository.class));

    @Test
    void partialRefundReducesTotalRentalAndDepositPaidIgnoringFailedRefunds() {
        var order = order();
        var payment = payment(order, PaymentStatus.PARTIALLY_REFUNDED);
        given(order, List.of(payment), List.of(
                refund(payment, PaymentRefundStatus.SUCCEEDED, "50", "40", "10"),
                refund(payment, PaymentRefundStatus.FAILED, "100", "80", "20")));

        var detail = service.get(order.getId());

        amount("200", detail.totalPaidAmount());
        amount("50", detail.remainingAmount());
        amount("40", detail.remainingRentalAmount());
        amount("10", detail.remainingDepositAmount());
        amount("200", detail.payments().getFirst().receivedAmount());
    }

    @Test
    void fullyRefundedPaymentContributesNoReceivedMoney() {
        var order = order();
        var payment = payment(order, PaymentStatus.REFUNDED);
        given(order, List.of(payment), List.of(refund(payment, PaymentRefundStatus.SUCCEEDED, "250", "200", "50")));

        var detail = service.get(order.getId());

        amount("0", detail.totalPaidAmount());
        amount("250", detail.remainingAmount());
        amount("200", detail.remainingRentalAmount());
        amount("50", detail.remainingDepositAmount());
    }

    @Test
    void pendingAndFailedPaymentsDoNotCountAsReceivedMoney() {
        var order = order();
        given(order, List.of(payment(order, PaymentStatus.PENDING), payment(order, PaymentStatus.FAILED)), List.of());

        var detail = service.get(order.getId());

        amount("0", detail.totalPaidAmount());
        amount("250", detail.remainingAmount());
    }

    private RentalOrder order() {
        var order = new RentalOrder();
        order.setId(UUID.randomUUID());
        order.setTotalAmount(new BigDecimal("250"));
        order.setDepositTotal(new BigDecimal("50"));
        return order;
    }

    private Payment payment(RentalOrder order, PaymentStatus status) {
        var payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setOrder(order);
        payment.setAmount(new BigDecimal("250"));
        payment.setRentalAmount(new BigDecimal("200"));
        payment.setDepositAmount(new BigDecimal("50"));
        payment.setStatus(status);
        payment.setCreatedAt(OffsetDateTime.parse("2026-01-01T00:00:00Z"));
        return payment;
    }

    private PaymentRefund refund(Payment payment, PaymentRefundStatus status, String amount, String rental, String deposit) {
        var refund = new PaymentRefund();
        refund.setId(UUID.randomUUID());
        refund.setPayment(payment);
        refund.setStatus(status);
        refund.setAmount(new BigDecimal(amount));
        refund.setRentalAmount(new BigDecimal(rental));
        refund.setDepositAmount(new BigDecimal(deposit));
        return refund;
    }

    private void given(RentalOrder order, List<Payment> receipts, List<PaymentRefund> returns) {
        when(orders.findById(order.getId())).thenReturn(Optional.of(order));
        when(payments.findAllByOrder_Id(order.getId())).thenReturn(receipts);
        when(refunds.findAllByPayment_Order_Id(order.getId())).thenReturn(returns);
        when(items.findAllByOrder_IdOrderByCreatedAtAsc(order.getId())).thenReturn(List.of());
    }

    private void amount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
