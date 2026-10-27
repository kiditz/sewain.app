package app.sewain.auth;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
		name = "users",
		schema = "public",
		indexes = {
				@Index(name = "idx_users_email", columnList = "email")
		}
)
@Getter
@Setter
@NoArgsConstructor
class User implements Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	@Column(nullable = false, unique = true)
	private String email;
	@Column(nullable = false, unique = true)
	private String phone;
	@Column
	private String password;
	@Column(name = "full_name", nullable = false)
	private String fullName;
	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;
	@Column(nullable = false)
	private boolean enabled = true;
	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false, updatable = false)
	private LocalDateTime updatedAt;

	@Transient
	void changePassword(String password) {
		this.password = password;
	}
}