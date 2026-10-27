package app.sewain.finance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "finance_transactions")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
class FinanceTransaction {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "source_type", nullable = false, length = 20)
	private String sourceType;

	@Column(name = "source_id", nullable = false)
	private UUID sourceId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private FinanceDirection direction;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private FinanceCategory category;

	@Column(nullable = false, length = 255)
	private String description;

	@Column(name = "order_id")
	private UUID orderId;

	@Column(name = "order_number", length = 50)
	private String orderNumber;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal amount;

	@Column(nullable = false, length = 3)
	private String currency;

	@Column(name = "payment_method", length = 30)
	private String paymentMethod;

	@Enumerated(EnumType.STRING)
	@Column(name = "expense_category", length = 30)
	private ExpenseCategory expenseCategory;

	@Column(columnDefinition = "text")
	private String notes;

	@Column(name = "occurred_at", nullable = false)
	private OffsetDateTime occurredAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;
}
