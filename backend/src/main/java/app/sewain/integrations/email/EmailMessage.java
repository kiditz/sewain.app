package app.sewain.integrations.email;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "integration_email_messages")
@Getter @Setter
class EmailMessage {
    @Id private UUID id = UUID.randomUUID();
    @Column(nullable = false, unique = true) private String externalKey;
    private String direction;
    private String status;
    @Column(length = 512) private String sender;
    @Column(length = 512) private String recipient;
    private String subject;
    @Column(columnDefinition = "text") private String body;
    private LocalDateTime occurredAt = LocalDateTime.now(java.time.ZoneOffset.UTC);
    private LocalDateTime createdAt = LocalDateTime.now(java.time.ZoneOffset.UTC);
}
