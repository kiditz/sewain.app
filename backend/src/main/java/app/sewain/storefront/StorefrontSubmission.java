package app.sewain.storefront;

import jakarta.persistence.*;
import lombok.Getter;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "storefront_submissions")
@Getter
@lombok.NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
class StorefrontSubmission {
    @Id private UUID id;
    @Column(nullable = false) private String kind;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String email;
    @Column(nullable = false, length = 5000) private String message;
    @Column(nullable = false) private boolean consent;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;
}
