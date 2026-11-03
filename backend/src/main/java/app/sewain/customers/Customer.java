package app.sewain.customers;

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
@Table(
		name = "customers",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_customers_email",
				columnNames = "email"
		)
)
public class Customer {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "full_name", nullable = false, length = 150)
	private String fullName;

	@Column(nullable = false, length = 255)
	private String email;

	@Column(length = 30)
	private String phone;

	@Column(columnDefinition = "text")
	private String notes;

	@Column(name = "password_hash")
	private String passwordHash;

	@Column(nullable = false)
	private boolean enabled = true;

    @Column(nullable = false, length = 30)
    private String source = "MANUAL";

    @Column(name = "email_subscribed", nullable = false)
    private boolean emailSubscribed;

    @Column(name = "email_consent_at")
    private OffsetDateTime emailConsentAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;
}
