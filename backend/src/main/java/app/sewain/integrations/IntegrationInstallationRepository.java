package app.sewain.integrations;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import java.util.UUID;

public interface IntegrationInstallationRepository extends JpaRepository<IntegrationInstallation, UUID> {
    Optional<IntegrationInstallation> findByCode(String code);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from IntegrationInstallation i where i.code = :code")
    Optional<IntegrationInstallation> findForUpdate(String code);
}
