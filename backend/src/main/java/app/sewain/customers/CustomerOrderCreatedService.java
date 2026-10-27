package app.sewain.customers;

import app.sewain.shared.CustomerOrderCreatedEvent;
import app.sewain.shared.CustomerResolvedForOrderEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class CustomerOrderCreatedService {
	private final CustomerRepository customerRepository;
	private final CustomerMapper mapper;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void createOrUpdateForOrder(CustomerOrderCreatedEvent event) {
		var customer = customerRepository.findByEmailIgnoreCase(event.email())
				.map(existing -> {
					mapper.update(event, existing);
					return existing;
				})
				.orElseGet(() -> mapper.toEntity(event));
		customer = customerRepository.saveAndFlush(customer);
		eventPublisher.publishEvent(new CustomerResolvedForOrderEvent(
				event.orderId(), customer.getId()
		));
	}
}
