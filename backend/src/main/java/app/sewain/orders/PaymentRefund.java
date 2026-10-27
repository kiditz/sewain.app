package app.sewain.orders;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payment_refunds")
class PaymentRefund {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "payment_id", nullable = false)
	private Payment payment;

	@Column(name = "provider_refund_id")
	private String providerRefundId;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal amount;

	@Column(name = "rental_amount", nullable = false, precision = 14, scale = 2)
	private BigDecimal rentalAmount = BigDecimal.ZERO;

	@Column(name = "deposit_amount", nullable = false, precision = 14, scale = 2)
	private BigDecimal depositAmount = BigDecimal.ZERO;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PaymentRefundStatus status = PaymentRefundStatus.PENDING;

	@Column(columnDefinition = "text")
	private String reason;

	@Column(name = "processed_at")
	private OffsetDateTime processedAt;

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
