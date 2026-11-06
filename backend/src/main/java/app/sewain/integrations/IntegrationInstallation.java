package app.sewain.integrations;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "integration_installations")
@Getter @Setter
public class IntegrationInstallation {
    @Id private UUID id = UUID.randomUUID();
    @Column(nullable = false, unique = true, length = 80) private String code;
    @Column(nullable = false) private boolean enabled;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb") private JsonNode configuration;
    @Column(columnDefinition = "text") private String encryptedSecrets;
    @CreationTimestamp @Column(updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp private LocalDateTime updatedAt;
}
