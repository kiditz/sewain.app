package app.sewain.customers;

import app.sewain.exception.ResourceNotFoundException;

import app.sewain.exception.ErrorCode;

import app.sewain.exception.ConflictException;
import app.sewain.shared.PageQuery;
import app.sewain.shared.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class CustomerService {
	private final CustomerRepository repository;
	private final CustomerMapper mapper;

	@Transactional(readOnly = true)
	PageResponse<CustomerResponse> search(PageQuery query) {
		var actualQuery = query == null ? PageQuery.defaultQuery() : query;
		var page = repository.findAll(
				CustomerSpecifications.search(actualQuery.getSearch()),
				PageRequest.of(actualQuery.pageIndex(), actualQuery.getSize(), CustomerSpecifications.toSort(actualQuery))
		);
		var summaries = getSummaries(page.getContent().stream().map(Customer::getId).toList());
		return new PageResponse<>(page.getContent().stream()
				.map(customer -> toResponse(customer, summaries.get(customer.getId())))
				.toList(), page.getTotalElements());
	}

	@Transactional(readOnly = true)
	CustomerResponse findById(UUID id) {
		var customer = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CUSTOMER_NOT_FOUND));
		return toResponse(customer, getSummaries(List.of(id)).get(id));
	}

	@Transactional
	CustomerResponse create(CustomerCommand command) {
		String email = normalizeEmail(command.email());
		if (repository.findByEmailIgnoreCase(email).isPresent()) throw new ConflictException(ErrorCode.CUSTOMER_EMAIL_EXISTS);
		var customer = mapper.toEntity(command);
		customer.setFullName(command.fullName().trim());
		customer.setEmail(email);
		customer.setPhone(normalizePhone(command.phone()));
		customer.setNotes(normalizeNotes(command.notes()));
		return toResponse(repository.saveAndFlush(customer), null);
	}

	@Transactional
	CustomerResponse setEnabled(UUID id, CustomerStatusCommand command) {
		var customer = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CUSTOMER_NOT_FOUND));
		customer.setEnabled(command.enabled());
		return toResponse(repository.save(customer), getSummaries(List.of(id)).get(id));
	}

	@Transactional(readOnly = true)
	CustomerStatisticsResponse statistics() {
		var monthStart = OffsetDateTime.now(ZoneOffset.UTC).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
		return new CustomerStatisticsResponse(
				repository.count(), repository.countByEnabledTrue(),
				repository.countByCreatedAtGreaterThanEqual(monthStart), repository.countRepeatCustomers()
		);
	}

	private CustomerResponse toResponse(Customer customer, CustomerOrderSummary summary) {
		return mapper.toResponse(customer,
				summary == null ? 0 : summary.orderCount(),
				summary == null ? BigDecimal.ZERO : summary.totalSpent(),
				summary == null ? null : summary.lastOrderAt());
	}

	private Map<UUID, CustomerOrderSummary> getSummaries(List<UUID> ids) {
		if (ids.isEmpty()) return Map.of();
		var summaries = new HashMap<UUID, CustomerOrderSummary>();
		for (Object[] row : repository.findOrderSummaries(ids)) {
			var id = (UUID) row[0];
			var count = ((Number) row[1]).longValue();
			var totalSpent = row[2] instanceof BigDecimal value ? value : new BigDecimal(row[2].toString());
			var lastOrderAt = toOffsetDateTime(row[3]);
			summaries.put(id, new CustomerOrderSummary(count, totalSpent, lastOrderAt));
		}
		return summaries;
	}

	private OffsetDateTime toOffsetDateTime(Object value) {
		if (value instanceof OffsetDateTime dateTime) return dateTime;
		if (value instanceof Timestamp timestamp) return timestamp.toInstant().atOffset(ZoneOffset.UTC);
		return null;
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(java.util.Locale.ROOT);
	}

	private String normalizePhone(String phone) {
		return phone == null || phone.isBlank() ? null : phone.trim();
	}

	private String normalizeNotes(String notes) {
		return notes == null || notes.isBlank() ? null : notes.trim();
	}

	private record CustomerOrderSummary(long orderCount, BigDecimal totalSpent, OffsetDateTime lastOrderAt) {}
}
