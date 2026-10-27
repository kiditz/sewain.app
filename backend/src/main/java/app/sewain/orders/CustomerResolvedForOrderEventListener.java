package app.sewain.orders;

import app.sewain.shared.CustomerResolvedForOrderEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class CustomerResolvedForOrderEventListener {
	private final OrderCustomerLinkService linkService;

	@ApplicationModuleListener
	void onCustomerResolvedForOrder(CustomerResolvedForOrderEvent event) {
		linkService.linkCustomer(event);
	}
}
