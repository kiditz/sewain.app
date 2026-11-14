package app.sewain.finance;

import app.sewain.shared.*;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class FinanceMapperTest {
    final FinanceMapper mapper=Mappers.getMapper(FinanceMapper.class);
    final OffsetDateTime at=OffsetDateTime.parse("2026-01-01T00:00:00Z");
    @Test void receivedEventSeparatesRentalAndDepositWithoutLosingSourceIdentity() {
        var event=new OrderPaymentReceivedEvent(UUID.randomUUID(),UUID.randomUUID(),"SW-123",new BigDecimal("200"),new BigDecimal("50"),"IDR","CASH",at,"tenant");
        var rental=mapper.toRentalIncome(event); var deposit=mapper.toDepositReceipt(event);
        assertEquals(FinanceCategory.RENTAL_PAYMENT,rental.getCategory()); assertEquals(FinanceCategory.DEPOSIT_RECEIVED,deposit.getCategory());
        assertEquals(FinanceDirection.INCOME,rental.getDirection()); assertEquals(event.paymentId(),rental.getSourceId());
        assertEquals(event.orderId(),deposit.getOrderId()); assertEquals(event.rentalAmount(),rental.getAmount());
        assertEquals(event.depositAmount(),deposit.getAmount()); assertEquals(at,deposit.getOccurredAt());
        assertEquals("IDR",mapper.toResponse(rental).currency());
    }
    @Test void refundedEventCreatesExpenseAllocationsBoundToRefundRatherThanPayment() {
        var event=new OrderPaymentRefundedEvent(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),"SW-123",new BigDecimal("80"),new BigDecimal("20"),"IDR","CASH",at,"tenant");
        var rental=mapper.toRentalRefund(event); var deposit=mapper.toDepositRefund(event);
        assertEquals(FinanceDirection.EXPENSE,rental.getDirection()); assertEquals(FinanceCategory.RENTAL_REFUND,rental.getCategory());
        assertEquals(FinanceCategory.DEPOSIT_REFUND,deposit.getCategory()); assertEquals(event.refundId(),deposit.getSourceId());
        assertEquals(event.rentalAmount(),rental.getAmount()); assertEquals(event.depositAmount(),deposit.getAmount());
        assertEquals(at,rental.getOccurredAt()); assertEquals("REFUND",mapper.toResponse(deposit).sourceType());
    }
}
