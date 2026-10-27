package app.sewain.catalogs;

import app.sewain.shared.DurationUnit;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "variant_rental_plans",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_rental_plans_duration",
                        columnNames = {
                                "variant_id",
                                "duration_unit",
                                "duration_value"
                        }
                )
        }
)
public class VariantRentalPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Enumerated(EnumType.STRING)
    @Column(name = "duration_unit", nullable = false, length = 20)
    private DurationUnit durationUnit;

    @Column(name = "duration_value", nullable = false)
    private int durationValue;

    @Column(name = "rental_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal rentalFee;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}