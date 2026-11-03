package app.sewain.customers;

import app.sewain.shared.CustomerOrderCreatedEvent;
import app.sewain.shared.GlobalMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Mapper(config = GlobalMapperConfig.class)
interface CustomerMapper {
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "source", ignore = true)
    @Mapping(target = "emailSubscribed", ignore = true)
    @Mapping(target = "emailConsentAt", ignore = true)
	@Mapping(target = "enabled", constant = "true")
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	Customer toEntity(CustomerCommand command);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "emailSubscribed", ignore = true)
    @Mapping(target = "emailConsentAt", ignore = true)
	@Mapping(target = "enabled", constant = "true")
	@Mapping(target = "notes", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "source", constant = "ORDER")
	Customer toEntity(CustomerOrderCreatedEvent event);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "source", ignore = true)
    @Mapping(target = "emailSubscribed", ignore = true)
    @Mapping(target = "emailConsentAt", ignore = true)
	@Mapping(target = "enabled", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	void update(CustomerCommand command, @MappingTarget Customer customer);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "source", ignore = true)
    @Mapping(target = "emailSubscribed", ignore = true)
    @Mapping(target = "emailConsentAt", ignore = true)
	@Mapping(target = "notes", ignore = true)
	@Mapping(target = "enabled", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	void update(CustomerOrderCreatedEvent event, @MappingTarget Customer customer);

	CustomerResponse toResponse(Customer customer, long orderCount, BigDecimal totalSpent, OffsetDateTime lastOrderAt);
}
