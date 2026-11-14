package app.sewain.finance;

import app.sewain.shared.*;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class FinanceEventListenerTest {
    final FinanceTransactionRepository repository=mock(FinanceTransactionRepository.class);
    final FinanceEventListener listener=new FinanceEventListener(repository,Mappers.getMapper(FinanceMapper.class));
    OrderPaymentReceivedEvent received(BigDecimal rental,BigDecimal deposit) {
        return new OrderPaymentReceivedEvent(UUID.randomUUID(),UUID.randomUUID(),"SW-TEST",rental,deposit,"IDR","CASH",OffsetDateTime.now(),"tenant");
    }
    @Test void receiptCreatesSeparateRentalAndDepositLedgerEntries() {
        var event=received(new BigDecimal("200"),new BigDecimal("50")); listener.onPaymentReceived(event);
        var capture=org.mockito.ArgumentCaptor.forClass(FinanceTransaction.class); verify(repository,times(2)).save(capture.capture());
        assertEquals(java.util.Set.of(FinanceCategory.RENTAL_PAYMENT,FinanceCategory.DEPOSIT_RECEIVED),
                capture.getAllValues().stream().map(FinanceTransaction::getCategory).collect(java.util.stream.Collectors.toSet()));
        capture.getAllValues().forEach(t->assertEquals(event.paymentId(),t.getSourceId()));
    }
    @Test void repeatedReceiptDoesNotDuplicateExistingCategories() {
        when(repository.existsBySourceTypeAndSourceIdAndCategory(anyString(),any(),any())).thenReturn(true);
        listener.onPaymentReceived(received(BigDecimal.TEN,BigDecimal.TEN)); verify(repository,never()).save(any());
    }
    @Test void zeroAndNegativeAmountsAreNotRecorded() {
        listener.onPaymentReceived(received(BigDecimal.ZERO,BigDecimal.ONE.negate())); verifyNoInteractions(repository);
    }
    @Test void refundCreatesExpenseWithRefundIdAndSkipsZeroDeposit() {
        var event=new OrderPaymentRefundedEvent(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),"SW-TEST",
                BigDecimal.TEN,BigDecimal.ZERO,"IDR","CASH",OffsetDateTime.now(),"tenant");
        listener.onPaymentRefunded(event);
        var capture=org.mockito.ArgumentCaptor.forClass(FinanceTransaction.class); verify(repository).save(capture.capture());
        assertEquals(FinanceDirection.EXPENSE,capture.getValue().getDirection());
        assertEquals(event.refundId(),capture.getValue().getSourceId());
    }
}
