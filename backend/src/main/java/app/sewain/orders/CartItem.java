package app.sewain.orders;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "cart_items")
class CartItem {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cart_id", nullable = false)
	private Cart cart;

	@Column(name = "product_id", nullable = false)
	private UUID productId;

	@Column(name = "variant_id", nullable = false)
	private UUID variantId;

	@Column(nullable = false)
	private int quantity = 1;

	@Column(name = "rental_start_at", nullable = false)
	private OffsetDateTime rentalStartAt;

	@Column(name = "rental_end_at", nullable = false)
	private OffsetDateTime rentalEndAt;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "rental_plans", columnDefinition = "jsonb", nullable = false)
	private List<Map<String, Object>> rentalPlans = new ArrayList<>();

	@Column(name = "rental_subtotal", nullable = false, precision = 19, scale = 2)
	private BigDecimal rentalSubtotal = BigDecimal.ZERO;

	@Column(name = "deposit_subtotal", nullable = false, precision = 19, scale = 2)
	private BigDecimal depositSubtotal = BigDecimal.ZERO;

	@Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal totalAmount = BigDecimal.ZERO;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;
}