package app.sewain.orders;

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
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payments")
class Payment {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false)
	private RentalOrder order;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_type", nullable = false, length = 20)
	private PaymentType paymentType = PaymentType.ORDER;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_stage", nullable = false, length = 20)
	private PaymentStage paymentStage = PaymentStage.INITIAL;

	@Enumerated(EnumType.STRING)
	@Column(name = "method", length = 30)
	private PaymentMethod method;

	@Column(nullable = false, length = 50)
	private String provider;

	@Column(name = "provider_payment_id")
	private String providerPaymentId;

	@Column(name = "idempotency_key", length = 100)
	private String idempotencyKey;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal amount;

	@Column(name = "rental_amount", nullable = false, precision = 14, scale = 2)
	private BigDecimal rentalAmount = BigDecimal.ZERO;

	@Column(name = "deposit_amount", nullable = false, precision = 14, scale = 2)
	private BigDecimal depositAmount = BigDecimal.ZERO;

	@Column(nullable = false, length = 3)
	private String currency = "IDR";

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private PaymentStatus status = PaymentStatus.PENDING;

	@Column(name = "payment_url", columnDefinition = "text")
	private String paymentUrl;

	@Column(name = "expires_at")
	private OffsetDateTime expiresAt;

	@Column(name = "paid_at")
	private OffsetDateTime paidAt;

	@Column(name = "failed_at")
	private OffsetDateTime failedAt;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "provider_response", columnDefinition = "jsonb")
	private Map<String, Object> providerResponse;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	void assignCashAllocation(BigDecimal rentalAmount, BigDecimal depositAmount) {
		this.rentalAmount = rentalAmount;
		this.depositAmount = depositAmount;
	}
}
