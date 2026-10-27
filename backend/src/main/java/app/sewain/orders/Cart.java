package app.sewain.orders;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "carts")
class Cart {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "customer_id")
	private UUID customerId;

	@Column(name = "guest_token")
	private UUID guestToken;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private CartStatus status = CartStatus.ACTIVE;

	@Column(name = "expires_at")
	private OffsetDateTime expiresAt;

	@OneToMany(
			mappedBy = "cart",
			cascade = CascadeType.ALL,
			orphanRemoval = true
	)
	private List<CartItem> items = new ArrayList<>();

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	void addItem(CartItem item) {
		items.add(item);
		item.setCart(this);
	}

	void removeItem(CartItem item) {
		items.remove(item);
		item.setCart(null);
	}
}