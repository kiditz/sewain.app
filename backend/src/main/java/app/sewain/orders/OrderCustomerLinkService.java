package app.sewain.orders;

import app.sewain.exception.ErrorCode;

import app.sewain.customers.Customer;
import app.sewain.shared.CustomerResolvedForOrderEvent;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class OrderCustomerLinkService {
	private final RentalOrderRepository orderRepository;
	private final MerchantOrderMapper mapper;
	private final EntityManager entityManager;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void linkCustomer(CustomerResolvedForOrderEvent event) {
		var order = orderRepository.findById(event.orderId())
				.orElseThrow(() -> new IllegalStateException(ErrorCode.ORDER_NOT_FOUND));
		var customer = entityManager.getReference(Customer.class, event.customerId());
		mapper.linkCustomer(customer, order);
		orderRepository.save(order);
	}
}
