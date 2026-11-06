package app.sewain.integrations.email;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.UUID;

interface EmailMessageRepository extends JpaRepository<EmailMessage, UUID> {
    Optional<EmailMessage> findByExternalKey(String externalKey);
    boolean existsByExternalKey(String externalKey);
    Page<EmailMessage> findByDirection(String direction, Pageable pageable);
}
