package app.sewain.orders;

import app.sewain.shared.MerchantOrderActivityEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class MerchantOrderActivityListener {
	private final MerchantOrderActivityRepository activityRepository;

	@ApplicationModuleListener
	void onActivity(MerchantOrderActivityEvent event) {
		MerchantOrderActivity activity = new MerchantOrderActivity();
		activity.setOrderId(event.orderId());
		activity.setOrderNumber(event.orderNumber());
		activity.setCustomerName(event.customerName());
		activity.setEventType(event.eventType());
		activity.setPreviousStatus(event.previousStatus());
		activity.setCurrentStatus(event.currentStatus());
		activity.setPaymentStage(event.paymentStage());
		activity.setAmount(event.amount());
		activityRepository.save(activity);
	}
}
