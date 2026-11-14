package app.sewain.orders;
import app.sewain.customers.Customer;
import app.sewain.shared.CustomerResolvedForOrderEvent;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class OrderCustomerLinkTest {
 @Test void resolvedIdentityLinksExistingOrderWithoutRecreatingCustomer(){var repository=mock(RentalOrderRepository.class);var em=mock(EntityManager.class);var service=new OrderCustomerLinkService(repository,Mappers.getMapper(MerchantOrderMapper.class),em);var event=new CustomerResolvedForOrderEvent(UUID.randomUUID(),UUID.randomUUID());var order=new RentalOrder();var customer=new Customer();when(repository.findById(event.orderId())).thenReturn(Optional.of(order));when(em.getReference(Customer.class,event.customerId())).thenReturn(customer);service.linkCustomer(event);assertSame(customer,order.getCustomer());verify(repository).save(order);assertThrows(IllegalStateException.class,()->service.linkCustomer(new CustomerResolvedForOrderEvent(UUID.randomUUID(),UUID.randomUUID())));}
}
