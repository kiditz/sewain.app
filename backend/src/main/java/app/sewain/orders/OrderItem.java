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
import java.util.UUID;
import java.util.Map;
import java.util.LinkedHashMap;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "order_items")
class OrderItem {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false)
	private RentalOrder order;


	@Column(name = "product_id", nullable = false)
	private UUID productId;

	@Column(name = "variant_id", nullable = false)
	private UUID variantId;

	@Column(name = "product_name", nullable = false)
	private String productName;

	@Column(name = "variant_name", nullable = false)
	private String variantName;

	@Column(nullable = false, length = 100)
	private String sku;

	@Column(name = "rental_start_at", nullable = false)
	private OffsetDateTime rentalStartAt;

	@Column(name = "rental_end_at", nullable = false)
	private OffsetDateTime rentalEndAt;

	@Column(name = "rental_duration_value", nullable = false)
	private int rentalDurationValue;

	@Enumerated(EnumType.STRING)
	@Column(name = "rental_duration_unit", nullable = false, length = 20)
	private RentalDurationUnit rentalDurationUnit;

	@Column(nullable = false)
	private int quantity;

	@Column(name = "rental_unit_price", nullable = false, precision = 14, scale = 2)
	private BigDecimal rentalUnitPrice;

	@Column(name = "deposit_unit_amount", nullable = false, precision = 14, scale = 2)
	private BigDecimal depositUnitAmount = BigDecimal.ZERO;

	@Column(name = "discount_amount", nullable = false, precision = 14, scale = 2)
	private BigDecimal discountAmount = BigDecimal.ZERO;

	@Column(name = "tax_amount", nullable = false, precision = 14, scale = 2)
	private BigDecimal taxAmount = BigDecimal.ZERO;

	@Column(name = "line_total", nullable = false, precision = 14, scale = 2)
	private BigDecimal lineTotal;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "rental_price_snapshot", columnDefinition = "jsonb", nullable = false)
	private Map<String, Object> rentalPriceSnapshot = new LinkedHashMap<>();

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;
}
