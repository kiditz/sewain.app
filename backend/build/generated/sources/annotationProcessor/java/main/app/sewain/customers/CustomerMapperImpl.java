package app.sewain.customers;

import app.sewain.shared.CustomerOrderCreatedEvent;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-20T15:51:39+0700",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-java-compiler-worker-9.7.1.jar, environment: Java 21.0.12 (Amazon.com Inc.)"
)
@Component
class CustomerMapperImpl implements CustomerMapper {

    @Override
    public Customer toEntity(CustomerCommand command) {
        if ( command == null ) {
            return null;
        }

        Customer customer = new Customer();

        customer.setFullName( command.fullName() );
        customer.setEmail( command.email() );
        customer.setPhone( command.phone() );
        customer.setNotes( command.notes() );

        customer.setEnabled( true );

        return customer;
    }

    @Override
    public Customer toEntity(CustomerOrderCreatedEvent event) {
        if ( event == null ) {
            return null;
        }

        Customer customer = new Customer();

        customer.setFullName( event.fullName() );
        customer.setEmail( event.email() );
        customer.setPhone( event.phone() );

        customer.setEnabled( true );

        return customer;
    }

    @Override
    public void update(CustomerCommand command, Customer customer) {
        if ( command == null ) {
            return;
        }

        if ( command.fullName() != null ) {
            customer.setFullName( command.fullName() );
        }
        if ( command.email() != null ) {
            customer.setEmail( command.email() );
        }
        if ( command.phone() != null ) {
            customer.setPhone( command.phone() );
        }
        if ( command.notes() != null ) {
            customer.setNotes( command.notes() );
        }
    }

    @Override
    public void update(CustomerOrderCreatedEvent event, Customer customer) {
        if ( event == null ) {
            return;
        }

        if ( event.fullName() != null ) {
            customer.setFullName( event.fullName() );
        }
        if ( event.email() != null ) {
            customer.setEmail( event.email() );
        }
        if ( event.phone() != null ) {
            customer.setPhone( event.phone() );
        }
    }

    @Override
    public CustomerResponse toResponse(Customer customer, long orderCount, BigDecimal totalSpent, OffsetDateTime lastOrderAt) {
        if ( customer == null && totalSpent == null && lastOrderAt == null ) {
            return null;
        }

        UUID id = null;
        String fullName = null;
        String email = null;
        String phone = null;
        String notes = null;
        OffsetDateTime createdAt = null;
        boolean enabled = false;
        if ( customer != null ) {
            id = customer.getId();
            fullName = customer.getFullName();
            email = customer.getEmail();
            phone = customer.getPhone();
            notes = customer.getNotes();
            createdAt = customer.getCreatedAt();
            enabled = customer.isEnabled();
        }
        long orderCount1 = 0L;
        orderCount1 = orderCount;
        BigDecimal totalSpent1 = null;
        totalSpent1 = totalSpent;
        OffsetDateTime lastOrderAt1 = null;
        lastOrderAt1 = lastOrderAt;

        CustomerResponse customerResponse = new CustomerResponse( id, fullName, email, phone, notes, orderCount1, totalSpent1, lastOrderAt1, createdAt, enabled );

        return customerResponse;
    }
}
