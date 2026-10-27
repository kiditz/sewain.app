package app.sewain.customers;

import app.sewain.shared.CustomerOrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class CustomerOrderCreatedEventListener {
	private final CustomerOrderCreatedService customerService;

	@ApplicationModuleListener
	void onCustomerOrderCreated(CustomerOrderCreatedEvent event) {
		customerService.createOrUpdateForOrder(event);
	}
}
