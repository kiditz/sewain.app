package app.sewain.orders;

import app.sewain.customers.Customer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
		name = "orders",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_orders_order_number",
				columnNames = "order_number"
		)
)
class RentalOrder {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "order_number", nullable = false, unique = true, length = 50)
	private String orderNumber;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "customer_id")
	private Customer customer;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "cart_id")
	private Cart cart;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private OrderStatus status = OrderStatus.PENDING_PAYMENT;

	@Column(name = "customer_name", nullable = false, length = 150)
	private String customerName;

	@Column(name = "customer_email", nullable = false, length = 255)
	private String customerEmail;

	@Column(name = "customer_phone", length = 30)
	private String customerPhone;

	@Enumerated(EnumType.STRING)
	@Column(name = "fulfillment_type", nullable = false, length = 20)
	private FulfillmentType fulfillmentType = FulfillmentType.PICKUP;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "fulfillment_address", columnDefinition = "jsonb")
	private Map<String, Object> fulfillmentAddress;

	@Column(name = "pickup_at")
	private OffsetDateTime pickupAt;

	@Column(name = "delivery_at")
	private OffsetDateTime deliveryAt;

	@Column(name = "return_at")
	private OffsetDateTime returnAt;

	/** Waktu pengembalian aktual (berbeda dari returnAt yang merupakan jadwal). */
	@Column(name = "returned_at")
	private OffsetDateTime returnedAt;

	@Column(name = "late_minutes", nullable = false)
	private long lateMinutes;

	@Column(name = "late_fee_unit", length = 10)
	private String lateFeeUnit;

	@Column(name = "late_fee_unit_amount", nullable = false, precision = 14, scale = 2)
	private BigDecimal lateFeeUnitAmount = BigDecimal.ZERO;

	@Column(name = "late_fee_units", nullable = false)
	private int lateFeeUnits;

	/** Denda hasil perhitungan, sebelum waiver. */
	@Column(name = "late_fee_assessed", nullable = false, precision = 14, scale = 2)
	private BigDecimal lateFeeAssessed = BigDecimal.ZERO;

	/** Denda yang benar-benar ditagihkan (sudah termasuk di totalAmount). */
	@Column(name = "late_fee_total", nullable = false, precision = 14, scale = 2)
	private BigDecimal lateFeeTotal = BigDecimal.ZERO;

	@Column(name = "late_fee_waived", nullable = false)
	private boolean lateFeeWaived;

	@Column(name = "late_fee_waiver_reason", length = 200)
	private String lateFeeWaiverReason;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_method", length = 30)
	private PaymentMethod paymentMethod;

	@Enumerated(EnumType.STRING)
	@Column(name = "balance_due_at", length = 20)
	private BalanceDueAt balanceDueAt;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "payment_terms_snapshot", columnDefinition = "jsonb", nullable = false)
	private Map<String, Object> paymentTermsSnapshot = new HashMap<>();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "shipping_address", columnDefinition = "jsonb")
	private Map<String, Object> shippingAddress = new HashMap<>();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "billing_address", columnDefinition = "jsonb")
	private Map<String, Object> billingAddress = new HashMap<>();

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal subtotal = BigDecimal.ZERO;

	@Column(name = "deposit_total", nullable = false, precision = 14, scale = 2)
	private BigDecimal depositTotal = BigDecimal.ZERO;

	@Column(name = "deposit_waived", nullable = false)
	private boolean depositWaived;

	@Column(name = "deposit_waiver_reason", length = 200)
	private String depositWaiverReason;

	@Column(name = "discount_total", nullable = false, precision = 14, scale = 2)
	private BigDecimal discountTotal = BigDecimal.ZERO;

	@Column(name = "tax_total", nullable = false, precision = 14, scale = 2)
	private BigDecimal taxTotal = BigDecimal.ZERO;

	@Column(name = "delivery_fee", nullable = false, precision = 14, scale = 2)
	private BigDecimal deliveryFee = BigDecimal.ZERO;

	@Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
	private BigDecimal totalAmount = BigDecimal.ZERO;

	@Column(nullable = false, length = 3)
	private String currency = "IDR";

	@Column(columnDefinition = "text")
	private String notes;

	@Column(name = "placed_at", nullable = false)
	private OffsetDateTime placedAt;

	@Column(name = "cancelled_at")
	private OffsetDateTime cancelledAt;

	@Column(name = "completed_at")
	private OffsetDateTime completedAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	@PrePersist
	void onCreate() {
		if (placedAt == null) {
			placedAt = OffsetDateTime.now(ZoneId.of("UTC"));
		}
	}
}
