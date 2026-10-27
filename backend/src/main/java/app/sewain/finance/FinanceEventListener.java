package app.sewain.finance;

import app.sewain.shared.OrderPaymentReceivedEvent;
import app.sewain.shared.OrderPaymentRefundedEvent;
import app.sewain.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
class FinanceEventListener {
	private final FinanceTransactionRepository transactionRepository;
	private final FinanceMapper mapper;

	@ApplicationModuleListener
	void onPaymentReceived(OrderPaymentReceivedEvent event) {
		saveIfMissing("PAYMENT", event.paymentId(), FinanceCategory.RENTAL_PAYMENT,
				event.rentalAmount(), mapper.toRentalIncome(event));
		saveIfMissing("PAYMENT", event.paymentId(), FinanceCategory.DEPOSIT_RECEIVED,
				event.depositAmount(), mapper.toDepositReceipt(event));
	}

	@ApplicationModuleListener
	void onPaymentRefunded(OrderPaymentRefundedEvent event) {
		saveIfMissing("REFUND", event.refundId(), FinanceCategory.RENTAL_REFUND,
				event.rentalAmount(), mapper.toRentalRefund(event));
		saveIfMissing("REFUND", event.refundId(), FinanceCategory.DEPOSIT_REFUND,
				event.depositAmount(), mapper.toDepositRefund(event));
	}

	private void saveIfMissing(String sourceType, java.util.UUID sourceId, FinanceCategory category,
	                           java.math.BigDecimal amount, FinanceTransaction transaction) {
		if (amount.signum() > 0 && !transactionRepository.existsBySourceTypeAndSourceIdAndCategory(sourceType, sourceId, category))
			transactionRepository.save(transaction);
	}

}
