package app.sewain.finance;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

record FinanceTransactionResponse(UUID id, String sourceType, UUID sourceId, FinanceDirection direction,
		FinanceCategory category, String description, UUID orderId, String orderNumber,
		BigDecimal amount, String currency, String paymentMethod, ExpenseCategory expenseCategory,
		String notes, OffsetDateTime occurredAt) {}
