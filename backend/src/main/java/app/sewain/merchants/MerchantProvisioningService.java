package app.sewain.merchants;

import app.sewain.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
class MerchantProvisioningService {
    private final MerchantRepository merchants;
    private final MerchantSchemaService schemas;
    private final ApplicationEventPublisher events;
    private final jakarta.persistence.EntityManager entityManager;

    record Status(UUID merchantId, String status, long version) {}
    record Result(UUID ownerId, Status status) {}
    private Status status(Merchant merchant) {
        return new Status(merchant.getId(), merchant.getProvisioningStatus(), merchant.getProvisioningVersion());
    }
    @Transactional(readOnly = true)
    public Status snapshot(UUID merchantId, UUID ownerId) {
        var merchant = owned(merchantId, ownerId);
        return status(merchant);
    }
    @Transactional
    public Status retry(UUID merchantId, UUID ownerId) {
        var initial = owned(merchantId, ownerId);
        var merchant = merchants.lockBySchema(initial.getSchema()).orElseThrow();
        if ("FAILED".equals(merchant.getProvisioningStatus())) {
            merchant.setProvisioningStatus("PROVISIONING");
            merchants.save(merchant);
            entityManager.flush();
            events.publishEvent(new MerchantCreatedEvent(merchant.getName(), merchant.getSchema()));
        }
        return status(merchant);
    }
    // Commit the persisted result before the listener publishes its WebSocket notification.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Result process(String schema) {
        var merchant = merchants.lockBySchema(schema).orElseThrow();
        if (!"PROVISIONING".equals(merchant.getProvisioningStatus())) return new Result(merchant.getUserId(), status(merchant));
        try {
            schemas.createTenantSchema(schema);
            merchant.setProvisioningStatus("READY");
        } catch (Exception error) {
            log.error("Tenant provisioning failed for {}", merchant.getId(), error);
            merchant.setProvisioningStatus("FAILED");
        }
        // Flush assigns the optimistic version before constructing the status notification.
        merchants.save(merchant);
        entityManager.flush();
        return new Result(merchant.getUserId(), status(merchant));
    }
    private Merchant owned(UUID merchantId, UUID ownerId) {
        var merchant = merchants.findById(merchantId).orElseThrow(() -> new ResourceNotFoundException("merchant.notFound"));
        if (!merchant.getUserId().equals(ownerId)) throw new AccessDeniedException("merchant.accessDenied");
        return merchant;
    }
}
