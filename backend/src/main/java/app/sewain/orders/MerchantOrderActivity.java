package app.sewain.orders;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "merchant_order_activities")
@Getter
@Setter
@NoArgsConstructor
class MerchantOrderActivity {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "order_id", nullable = false)
	private UUID orderId;

	@Column(name = "order_number", nullable = false, length = 50)
	private String orderNumber;

	@Column(name = "customer_name", nullable = false, length = 150)
	private String customerName;

	@Column(name = "event_type", nullable = false, length = 30)
	private String eventType;

	@Column(name = "previous_status", length = 30)
	private String previousStatus;

	@Column(name = "current_status", length = 30)
	private String currentStatus;

	@Column(name = "payment_stage", length = 20)
	private String paymentStage;

	@Column(precision = 14, scale = 2)
	private BigDecimal amount;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	MerchantOrderActivity(UUID orderId, String orderNumber, String customerName, String eventType,
			String previousStatus, String currentStatus, String paymentStage, BigDecimal amount) {
		this.orderId = orderId;
		this.orderNumber = orderNumber;
		this.customerName = customerName;
		this.eventType = eventType;
		this.previousStatus = previousStatus;
		this.currentStatus = currentStatus;
		this.paymentStage = paymentStage;
		this.amount = amount;
	}
}
