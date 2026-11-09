package app.sewain.merchants;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MerchantProvisioningServiceTest {
    private final MerchantRepository merchants = mock(MerchantRepository.class);
    private final MerchantSchemaService schemas = mock(MerchantSchemaService.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final MerchantProvisioningService service = new MerchantProvisioningService(merchants, schemas, events, entityManager);

    private Merchant merchant() {
        var merchant = new Merchant(); merchant.setId(UUID.randomUUID()); merchant.setUserId(UUID.randomUUID());
        merchant.setName("Store"); merchant.setSchema("test_store"); merchant.setProvisioningStatus("PROVISIONING");
        when(merchants.findById(merchant.getId())).thenReturn(Optional.of(merchant));
        when(merchants.lockBySchema("test_store")).thenReturn(Optional.of(merchant));
        return merchant;
    }
    @Test void cannotReadAnotherOwnersStatus() {
        var merchant = merchant();
        assertThrows(AccessDeniedException.class, () -> service.snapshot(merchant.getId(), UUID.randomUUID()));
    }
    @Test void refreshSnapshotReflectsCompletedMigrationAndDuplicateJobDoesNotRerun() {
        var merchant = merchant();
        assertEquals("PROVISIONING", service.snapshot(merchant.getId(), merchant.getUserId()).status());
        service.process("test_store");
        assertEquals("READY", service.snapshot(merchant.getId(), merchant.getUserId()).status());
        service.process("test_store");
        verify(schemas, times(1)).createTenantSchema("test_store");
    }
    @Test void migrationFailureIsPersistedAndRetryIsOnlyEnqueuedOnce() {
        var merchant = merchant();
        doThrow(new IllegalStateException("migration failed")).when(schemas).createTenantSchema("test_store");
        assertEquals("FAILED", service.process("test_store").status().status());
        assertEquals("PROVISIONING", service.retry(merchant.getId(), merchant.getUserId()).status());
        service.retry(merchant.getId(), merchant.getUserId());
        verify(events, times(1)).publishEvent(any(MerchantCreatedEvent.class));
    }
}
