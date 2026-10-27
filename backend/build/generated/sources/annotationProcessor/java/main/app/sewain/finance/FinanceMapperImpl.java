package app.sewain.finance;

import app.sewain.shared.OrderPaymentReceivedEvent;
import app.sewain.shared.OrderPaymentRefundedEvent;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-20T12:27:41+0700",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-java-compiler-worker-9.7.1.jar, environment: Java 21.0.12 (Amazon.com Inc.)"
)
@Component
class FinanceMapperImpl implements FinanceMapper {

    @Override
    public FinanceTransaction toRentalIncome(OrderPaymentReceivedEvent event) {
        if ( event == null ) {
            return null;
        }

        FinanceTransaction.FinanceTransactionBuilder financeTransaction = FinanceTransaction.builder();

        financeTransaction.sourceId( event.paymentId() );
        financeTransaction.orderId( event.orderId() );
        financeTransaction.orderNumber( event.orderNumber() );
        financeTransaction.amount( event.rentalAmount() );
        financeTransaction.currency( event.currency() );
        financeTransaction.paymentMethod( event.method() );
        financeTransaction.occurredAt( event.paidAt() );

        financeTransaction.sourceType( "PAYMENT" );
        financeTransaction.direction( FinanceDirection.INCOME );
        financeTransaction.category( FinanceCategory.RENTAL_PAYMENT );
        financeTransaction.description( "Rental payment · " + event.orderNumber() );

        return financeTransaction.build();
    }

    @Override
    public FinanceTransaction toDepositReceipt(OrderPaymentReceivedEvent event) {
        if ( event == null ) {
            return null;
        }

        FinanceTransaction.FinanceTransactionBuilder financeTransaction = FinanceTransaction.builder();

        financeTransaction.sourceId( event.paymentId() );
        financeTransaction.orderId( event.orderId() );
        financeTransaction.orderNumber( event.orderNumber() );
        financeTransaction.amount( event.depositAmount() );
        financeTransaction.currency( event.currency() );
        financeTransaction.paymentMethod( event.method() );
        financeTransaction.occurredAt( event.paidAt() );

        financeTransaction.sourceType( "PAYMENT" );
        financeTransaction.direction( FinanceDirection.INCOME );
        financeTransaction.category( FinanceCategory.DEPOSIT_RECEIVED );
        financeTransaction.description( "Rental deposit · " + event.orderNumber() );

        return financeTransaction.build();
    }

    @Override
    public FinanceTransaction toRentalRefund(OrderPaymentRefundedEvent event) {
        if ( event == null ) {
            return null;
        }

        FinanceTransaction.FinanceTransactionBuilder financeTransaction = FinanceTransaction.builder();

        financeTransaction.sourceId( event.refundId() );
        financeTransaction.orderId( event.orderId() );
        financeTransaction.orderNumber( event.orderNumber() );
        financeTransaction.amount( event.rentalAmount() );
        financeTransaction.currency( event.currency() );
        financeTransaction.paymentMethod( event.method() );
        financeTransaction.occurredAt( event.processedAt() );

        financeTransaction.sourceType( "REFUND" );
        financeTransaction.direction( FinanceDirection.EXPENSE );
        financeTransaction.category( FinanceCategory.RENTAL_REFUND );
        financeTransaction.description( "Rental refund · " + event.orderNumber() );

        return financeTransaction.build();
    }

    @Override
    public FinanceTransaction toDepositRefund(OrderPaymentRefundedEvent event) {
        if ( event == null ) {
            return null;
        }

        FinanceTransaction.FinanceTransactionBuilder financeTransaction = FinanceTransaction.builder();

        financeTransaction.sourceId( event.refundId() );
        financeTransaction.orderId( event.orderId() );
        financeTransaction.orderNumber( event.orderNumber() );
        financeTransaction.amount( event.depositAmount() );
        financeTransaction.currency( event.currency() );
        financeTransaction.paymentMethod( event.method() );
        financeTransaction.occurredAt( event.processedAt() );

        financeTransaction.sourceType( "REFUND" );
        financeTransaction.direction( FinanceDirection.EXPENSE );
        financeTransaction.category( FinanceCategory.DEPOSIT_REFUND );
        financeTransaction.description( "Deposit refund · " + event.orderNumber() );

        return financeTransaction.build();
    }

    @Override
    public FinanceTransaction toExpense(RecordExpenseCommand command, UUID sourceId, String currency, String timezone) {
        if ( command == null && sourceId == null && currency == null && timezone == null ) {
            return null;
        }

        FinanceTransaction.FinanceTransactionBuilder financeTransaction = FinanceTransaction.builder();

        if ( command != null ) {
            financeTransaction.description( command.description() );
            financeTransaction.amount( command.amount() );
            financeTransaction.expenseCategory( command.category() );
            financeTransaction.notes( command.notes() );
        }
        financeTransaction.sourceId( sourceId );
        financeTransaction.currency( currency );
        financeTransaction.sourceType( "EXPENSE" );
        financeTransaction.direction( FinanceDirection.EXPENSE );
        financeTransaction.category( FinanceCategory.OPERATING_EXPENSE );
        financeTransaction.paymentMethod( command.paymentMethod().name() );
        financeTransaction.occurredAt( command.date().atStartOfDay(java.time.ZoneId.of(timezone)).toOffsetDateTime() );

        return financeTransaction.build();
    }

    @Override
    public FinanceTransactionResponse toResponse(FinanceTransaction transaction) {
        if ( transaction == null ) {
            return null;
        }

        UUID id = null;
        String sourceType = null;
        UUID sourceId = null;
        FinanceDirection direction = null;
        FinanceCategory category = null;
        String description = null;
        UUID orderId = null;
        String orderNumber = null;
        BigDecimal amount = null;
        String currency = null;
        String paymentMethod = null;
        ExpenseCategory expenseCategory = null;
        String notes = null;
        OffsetDateTime occurredAt = null;

        id = transaction.getId();
        sourceType = transaction.getSourceType();
        sourceId = transaction.getSourceId();
        direction = transaction.getDirection();
        category = transaction.getCategory();
        description = transaction.getDescription();
        orderId = transaction.getOrderId();
        orderNumber = transaction.getOrderNumber();
        amount = transaction.getAmount();
        currency = transaction.getCurrency();
        paymentMethod = transaction.getPaymentMethod();
        expenseCategory = transaction.getExpenseCategory();
        notes = transaction.getNotes();
        occurredAt = transaction.getOccurredAt();

        FinanceTransactionResponse financeTransactionResponse = new FinanceTransactionResponse( id, sourceType, sourceId, direction, category, description, orderId, orderNumber, amount, currency, paymentMethod, expenseCategory, notes, occurredAt );

        return financeTransactionResponse;
    }
}
