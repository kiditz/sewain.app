package app.sewain.merchants;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "merchants", schema = "public", indexes = {@Index(name = "idx_merchants_slug", columnList = "slug")})
@Getter
@Setter
@NoArgsConstructor
class Merchant {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false, unique = true)
	private String slug;
	@Column(nullable = false, unique = true, updatable = false)
	private String schema;

	@Column(name = "is_active", nullable = false)
	private boolean active;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@CreationTimestamp
	@Column(name = "updated_at", nullable = false, updatable = false)
	private LocalDateTime updatedAt;

	@Column(name = "custom_domain", unique = true)
	private String customDomain;

	@Column(name = "is_published", nullable = false)
	private boolean published;

	@Embedded
	private StoreInformation storeInformation = new StoreInformation();

	@Embedded
	@AttributeOverrides({ //NOSONAR
			@AttributeOverride(name = "name", column = @Column(name = "business_name")), @AttributeOverride(name = "address", column = @Column(name = "business_address", columnDefinition = "text")), @AttributeOverride(name = "email", column = @Column(name = "business_email")), @AttributeOverride(name = "phone", column = @Column(name = "business_phone")), @AttributeOverride(name = "taxId", column = @Column(name = "tax_id"))})
	private BusinessInformation businessInformation = new BusinessInformation();
}