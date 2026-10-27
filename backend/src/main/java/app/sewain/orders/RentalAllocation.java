package app.sewain.orders;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "rental_allocations")
class RentalAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;


    @Column(name = "inventory_unit_id", nullable = false)
    private UUID inventoryUnitId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @Column(name = "rental_start_at", nullable = false)
    private OffsetDateTime rentalStartAt;

    @Column(name = "rental_end_at", nullable = false)
    private OffsetDateTime rentalEndAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RentalAllocationStatus status = RentalAllocationStatus.HELD;

    @Column(name = "hold_expires_at")
    private OffsetDateTime holdExpiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
