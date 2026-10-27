package app.sewain.finance;

import app.sewain.shared.GlobalMapperConfig;
import app.sewain.shared.OrderPaymentReceivedEvent;
import app.sewain.shared.OrderPaymentRefundedEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.OffsetDateTime;
import java.util.UUID;

@Mapper(config = GlobalMapperConfig.class)
interface FinanceMapper {
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "sourceType", constant = "PAYMENT")
	@Mapping(target = "sourceId", source = "paymentId")
	@Mapping(target = "direction", constant = "INCOME")
	@Mapping(target = "category", constant = "RENTAL_PAYMENT")
	@Mapping(target = "description", expression = "java(\"Rental payment · \" + event.orderNumber())")
	@Mapping(target = "orderId", source = "orderId")
	@Mapping(target = "orderNumber", source = "orderNumber")
	@Mapping(target = "amount", source = "rentalAmount")
	@Mapping(target = "currency", source = "currency")
	@Mapping(target = "paymentMethod", source = "method")
	@Mapping(target = "expenseCategory", ignore = true)
	@Mapping(target = "notes", ignore = true)
	@Mapping(target = "occurredAt", source = "paidAt")
	@Mapping(target = "createdAt", ignore = true)
	FinanceTransaction toRentalIncome(OrderPaymentReceivedEvent event);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "sourceType", constant = "PAYMENT")
	@Mapping(target = "sourceId", source = "paymentId")
	@Mapping(target = "direction", constant = "INCOME")
	@Mapping(target = "category", constant = "DEPOSIT_RECEIVED")
	@Mapping(target = "description", expression = "java(\"Rental deposit · \" + event.orderNumber())")
	@Mapping(target = "orderId", source = "orderId")
	@Mapping(target = "orderNumber", source = "orderNumber")
	@Mapping(target = "amount", source = "depositAmount")
	@Mapping(target = "currency", source = "currency")
	@Mapping(target = "paymentMethod", source = "method")
	@Mapping(target = "expenseCategory", ignore = true)
	@Mapping(target = "notes", ignore = true)
	@Mapping(target = "occurredAt", source = "paidAt")
	@Mapping(target = "createdAt", ignore = true)
	FinanceTransaction toDepositReceipt(OrderPaymentReceivedEvent event);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "sourceType", constant = "REFUND")
	@Mapping(target = "sourceId", source = "refundId")
	@Mapping(target = "direction", constant = "EXPENSE")
	@Mapping(target = "category", constant = "RENTAL_REFUND")
	@Mapping(target = "description", expression = "java(\"Rental refund · \" + event.orderNumber())")
	@Mapping(target = "orderId", source = "orderId")
	@Mapping(target = "orderNumber", source = "orderNumber")
	@Mapping(target = "amount", source = "rentalAmount")
	@Mapping(target = "currency", source = "currency")
	@Mapping(target = "paymentMethod", source = "method")
	@Mapping(target = "expenseCategory", ignore = true)
	@Mapping(target = "notes", ignore = true)
	@Mapping(target = "occurredAt", source = "processedAt")
	@Mapping(target = "createdAt", ignore = true)
	FinanceTransaction toRentalRefund(OrderPaymentRefundedEvent event);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "sourceType", constant = "REFUND")
	@Mapping(target = "sourceId", source = "refundId")
	@Mapping(target = "direction", constant = "EXPENSE")
	@Mapping(target = "category", constant = "DEPOSIT_REFUND")
	@Mapping(target = "description", expression = "java(\"Deposit refund · \" + event.orderNumber())")
	@Mapping(target = "orderId", source = "orderId")
	@Mapping(target = "orderNumber", source = "orderNumber")
	@Mapping(target = "amount", source = "depositAmount")
	@Mapping(target = "currency", source = "currency")
	@Mapping(target = "paymentMethod", source = "method")
	@Mapping(target = "expenseCategory", ignore = true)
	@Mapping(target = "notes", ignore = true)
	@Mapping(target = "occurredAt", source = "processedAt")
	@Mapping(target = "createdAt", ignore = true)
	FinanceTransaction toDepositRefund(OrderPaymentRefundedEvent event);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "sourceType", constant = "EXPENSE")
	@Mapping(target = "sourceId", source = "sourceId")
	@Mapping(target = "direction", constant = "EXPENSE")
	@Mapping(target = "category", constant = "OPERATING_EXPENSE")
	@Mapping(target = "description", source = "command.description")
	@Mapping(target = "orderId", ignore = true)
	@Mapping(target = "orderNumber", ignore = true)
	@Mapping(target = "amount", source = "command.amount")
	@Mapping(target = "currency", source = "currency")
	@Mapping(target = "paymentMethod", expression = "java(command.paymentMethod().name())")
	@Mapping(target = "expenseCategory", source = "command.category")
	@Mapping(target = "notes", source = "command.notes")
	@Mapping(target = "occurredAt", expression = "java(command.date().atStartOfDay(java.time.ZoneId.of(timezone)).toOffsetDateTime())")
	@Mapping(target = "createdAt", ignore = true)
	FinanceTransaction toExpense(RecordExpenseCommand command, UUID sourceId, String currency, String timezone);

	FinanceTransactionResponse toResponse(FinanceTransaction transaction);
}
