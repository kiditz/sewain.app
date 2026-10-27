package app.sewain.finance;

import app.sewain.exception.ValidationException;

import app.sewain.exception.ErrorCode;

import app.sewain.settings.SettingFacade;
import app.sewain.settings.SettingKey;
import app.sewain.shared.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class FinanceService {
	public static final String TIMEZONE = "timezone";
	public static final String ASIA_JAKARTA = "Asia/Jakarta";
	private final FinanceTransactionRepository transactionRepository;
	private final FinanceMapper mapper;
	private final SettingFacade settings;

	@Transactional(readOnly = true)
	FinanceDashboardResponse get(LocalDate from, LocalDate to, String direction, Integer page, Integer size) {
		var range = range(from, to);
		var fromAt = range.fromAt();
		var toAt = range.toAt();
		var cashIn = transactionRepository.sumAmount(FinanceDirection.INCOME, fromAt, toAt);
		var cashOut = transactionRepository.sumAmount(FinanceDirection.EXPENSE, fromAt, toAt);
		var rentalIncome = transactionRepository.sumCategory(FinanceCategory.RENTAL_PAYMENT, FinanceDirection.INCOME, fromAt, toAt)
				.subtract(transactionRepository.sumCategory(FinanceCategory.RENTAL_REFUND, FinanceDirection.EXPENSE, fromAt, toAt));
		var depositHeld = transactionRepository.sumLifetimeCategory(FinanceCategory.DEPOSIT_RECEIVED, FinanceDirection.INCOME)
				.subtract(transactionRepository.sumLifetimeCategory(FinanceCategory.DEPOSIT_REFUND, FinanceDirection.EXPENSE))
				.max(BigDecimal.ZERO);
		var summary = new FinanceSummaryResponse(cashIn, cashOut, rentalIncome.max(BigDecimal.ZERO), depositHeld);
		String timezone = settings.getValue(SettingKey.REGIONAL).path(TIMEZONE).asString(ASIA_JAKARTA);
		var cashFlow = transactionRepository.findCashFlow(fromAt, toAt, timezone).stream()
				.map(point -> new FinanceCashFlowPoint(point.getDate(), point.getIncome(), point.getExpense()))
				.toList();

		var pageable = PageRequest.of(Math.max(page == null ? 0 : page, 0),
				Math.clamp(size == null ? 20 : size, 1, 100), Sort.by(Sort.Direction.DESC, "occurredAt"));
		var transactionPage = direction == null || direction.isBlank() || "ALL".equalsIgnoreCase(direction)
				? transactionRepository.findAllByOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtDesc(fromAt, toAt, pageable)
				: transactionRepository.findAllByOccurredAtGreaterThanEqualAndOccurredAtLessThanAndDirectionOrderByOccurredAtDesc(
				fromAt, toAt, FinanceDirection.valueOf(direction.toUpperCase()), pageable);
		var transactions = new PageResponse<>(transactionPage.getContent().stream().map(mapper::toResponse).toList(), transactionPage.getTotalElements());
		return new FinanceDashboardResponse(summary, cashFlow, transactions);
	}

	@Transactional
	FinanceTransactionResponse recordExpense(RecordExpenseCommand command) {
		JsonNode regional = settings.getValue(SettingKey.REGIONAL);
		String timezone = regional.path(TIMEZONE).asString(ASIA_JAKARTA);
		String currency = regional.path("currency").asString("IDR");
		var transaction = mapper.toExpense(command, UUID.randomUUID(), currency, timezone);
		return mapper.toResponse(transactionRepository.save(transaction));
	}

	private DateRange range(LocalDate from, LocalDate to) {
		LocalDate end = to == null ? LocalDate.now(ZoneId.systemDefault()) : to;
		LocalDate start = from == null ? end.minusDays(29) : from;
		if (end.isBefore(start)) throw new ValidationException(ErrorCode.FINANCE_DATE_RANGE_INVALID);
		if (java.time.temporal.ChronoUnit.DAYS.between(start, end) >= 90)
			throw new ValidationException(ErrorCode.FINANCE_DATE_RANGE_TOO_LARGE);
		String timezone = settings.getValue(SettingKey.REGIONAL).path(TIMEZONE).asString(ASIA_JAKARTA);
		ZoneId zone = ZoneId.of(timezone);
		return new DateRange(start.atStartOfDay(zone).toOffsetDateTime(), end.plusDays(1).atStartOfDay(zone).toOffsetDateTime());
	}

	private record DateRange(OffsetDateTime fromAt, OffsetDateTime toAt) {
	}
}
