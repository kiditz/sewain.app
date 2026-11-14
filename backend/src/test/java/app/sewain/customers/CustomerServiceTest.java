package app.sewain.customers;

import app.sewain.exception.*;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class CustomerServiceTest {
    final CustomerRepository repository=mock(CustomerRepository.class);
    final CustomerService service=new CustomerService(repository,Mappers.getMapper(CustomerMapper.class));
    @Test void createNormalizesIdentityAndBlankOptionalFields() {
        when(repository.saveAndFlush(any())).thenAnswer(c->c.getArgument(0));
        var result=service.create(new CustomerCommand(" Owner "," Owner@Example.Invalid "," ",null));
        assertEquals("Owner",result.fullName()); assertEquals("owner@example.invalid",result.email());
        assertNull(result.phone()); assertNull(result.notes()); assertEquals(0,result.orderCount()); assertEquals(0,result.totalSpent().signum());
        result=service.create(new CustomerCommand("Owner","owner2@example.invalid"," 08123 "," VIP "));
        assertEquals("08123",result.phone()); assertEquals("VIP",result.notes());
    }
    @Test void duplicateNormalizedEmailIsRejected() {
        when(repository.findByEmailIgnoreCase("owner@example.invalid")).thenReturn(Optional.of(new Customer()));
        assertThrows(ConflictException.class,()->service.create(new CustomerCommand("Owner"," OWNER@example.invalid ",null,null)));
        verify(repository,never()).saveAndFlush(any());
    }
    @Test void detailMapsAggregateAmountsAndTimestampToUtc() {
        var customer=new Customer(); var id=UUID.randomUUID(); customer.setId(id);
        when(repository.findById(id)).thenReturn(Optional.of(customer));
        var instant=Instant.parse("2026-01-01T00:00:00Z");
        when(repository.findOrderSummaries(List.of(id))).thenReturn(Collections.singletonList(
                new Object[]{id,3L,"125000.50",java.sql.Timestamp.from(instant)}));
        var result=service.findById(id);
        assertEquals(3,result.orderCount()); assertEquals(0,new BigDecimal("125000.50").compareTo(result.totalSpent()));
        assertEquals(instant.atOffset(ZoneOffset.UTC),result.lastOrderAt());
    }
    @Test void enabledStatusChangesWithoutResettingOrderSummary() {
        var customer=new Customer(); var id=UUID.randomUUID(); customer.setId(id); customer.setEnabled(true);
        when(repository.findById(id)).thenReturn(Optional.of(customer)); when(repository.save(customer)).thenReturn(customer);
        assertFalse(service.setEnabled(id,new CustomerStatusCommand(false)).enabled());
        assertThrows(ResourceNotFoundException.class,()->service.findById(UUID.randomUUID()));
    }
    @Test void emptySearchSkipsAggregateQuery() {
        when(repository.findAll(any(Specification.class),any(Pageable.class))).thenReturn(Page.empty());
        service.search(null); verify(repository,never()).findOrderSummaries(any());
    }
    @Test void statisticsCountsNewCustomersFromUtcMonthStart() {
        service.statistics();
        var boundary=org.mockito.ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(repository).countByCreatedAtGreaterThanEqual(boundary.capture());
        assertEquals(ZoneOffset.UTC,boundary.getValue().getOffset()); assertEquals(1,boundary.getValue().getDayOfMonth());
        assertEquals(LocalTime.MIDNIGHT,boundary.getValue().toLocalTime());
    }
}
