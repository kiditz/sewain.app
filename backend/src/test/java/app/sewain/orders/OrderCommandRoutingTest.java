package app.sewain.orders;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class OrderCommandRoutingTest {
 @Test void facadeRoutesEachCommandWithOriginalOrderAndPaymentIdentity(){var creation=mock(MerchantOrderCreationService.class);var payment=mock(MerchantOrderPaymentService.class);var lifecycle=mock(MerchantOrderLifecycleService.class);var service=new MerchantOrderCommandServiceImpl(creation,payment,lifecycle);var order=UUID.randomUUID();var receipt=UUID.randomUUID();var create=mock(MerchantOrderCommand.class);var pay=new RecordOrderPaymentCommand(PaymentStage.INITIAL,BigDecimal.TEN,PaymentMethod.CASH,"ref");var refund=new RecordPaymentRefundCommand(BigDecimal.ONE,"refund");var deposit=new RecordDepositRefundCommand(BigDecimal.ONE,"deposit");var returned=new RecordOrderReturnCommand(null,false,null);var response=mock(MerchantOrderResponse.class);when(creation.create(create)).thenReturn(response);assertSame(response,service.create(create));service.recordPayment(order,pay);service.recordRefund(order,receipt,refund);service.recordDepositRefund(order,deposit);service.recordReturn(order,returned);service.updateStatus(order,OrderStatus.READY);verify(payment).recordPayment(order,pay);verify(payment).recordRefund(order,receipt,refund);verify(payment).recordDepositRefund(order,deposit);verify(lifecycle).recordReturn(order,returned);verify(lifecycle).updateStatus(order,OrderStatus.READY);}
}
