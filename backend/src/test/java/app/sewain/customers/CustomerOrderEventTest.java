package app.sewain.customers;
import app.sewain.shared.*;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.context.ApplicationEventPublisher;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class CustomerOrderEventTest {
 @Test void eventCreatesCustomerAndPublishesResolvedIdentity(){var repository=mock(CustomerRepository.class);var events=mock(ApplicationEventPublisher.class);var service=new CustomerOrderCreatedService(repository,Mappers.getMapper(CustomerMapper.class),events);var id=UUID.randomUUID();when(repository.saveAndFlush(any())).thenAnswer(c->{Customer customer=c.getArgument(0);customer.setId(id);return customer;});var event=new CustomerOrderCreatedEvent(UUID.randomUUID(),"Owner","owner@example.invalid","08123");service.createOrUpdateForOrder(event);verify(events).publishEvent(new CustomerResolvedForOrderEvent(event.orderId(),id));}
 @Test void existingCustomerIdentityIsReused(){var repository=mock(CustomerRepository.class);var events=mock(ApplicationEventPublisher.class);var service=new CustomerOrderCreatedService(repository,Mappers.getMapper(CustomerMapper.class),events);var customer=new Customer();customer.setId(UUID.randomUUID());when(repository.findByEmailIgnoreCase("owner@example.invalid")).thenReturn(Optional.of(customer));when(repository.saveAndFlush(customer)).thenReturn(customer);var event=new CustomerOrderCreatedEvent(UUID.randomUUID(),"Updated","owner@example.invalid",null);service.createOrUpdateForOrder(event);assertEquals("Updated",customer.getFullName());verify(events).publishEvent(new CustomerResolvedForOrderEvent(event.orderId(),customer.getId()));}
}
