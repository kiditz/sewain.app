package app.sewain.catalogs;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
		name = "product_variants",
		uniqueConstraints = {
				@UniqueConstraint(
						name = "uk_product_variants_product_sku",
						columnNames = {"product_id", "sku"}
				),
				@UniqueConstraint(
						name = "uk_product_variants_product_id_id",
						columnNames = {"product_id", "id"}
				)
		}
)
class ProductVariant {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false, length = 100)
	private String sku;

	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(name = "media_list", nullable = false, columnDefinition = "text[]")
	private List<String> mediaList = new ArrayList<>();

	@Column(name = "buy_price", precision = 12, scale = 2)
	private BigDecimal buyPrice;

	@Column(
			name = "deposit_amount",
			nullable = false,
			precision = 12,
			scale = 2
	)
	private BigDecimal depositAmount = BigDecimal.ZERO;

	@Column(name = "is_active", nullable = false)
	private boolean active = true;

	@OneToMany(
			mappedBy = "variant",
			fetch = FetchType.LAZY,
			cascade = CascadeType.ALL,
			orphanRemoval = true
	)
	private List<ProductVariantValue> values = new ArrayList<>();

	@OneToMany(
			mappedBy = "variant",
			fetch = FetchType.LAZY,
			cascade = CascadeType.ALL,
			orphanRemoval = true
	)
	private List<VariantRentalPlan> rentalPlans = new ArrayList<>();

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@OneToMany(mappedBy = "variant", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
	private List<InventoryUnit> inventoryUnits = new ArrayList<>();

	public void addInventoryUnit(InventoryUnit inventoryUnit) {
		inventoryUnits.add(inventoryUnit);
		inventoryUnit.setVariant(this);
	}

	public void removeInventoryUnit(InventoryUnit inventoryUnit) {
		inventoryUnits.remove(inventoryUnit);
		inventoryUnit.setVariant(null);
	}
}