package app.sewain.merchants;

import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Component
@RequiredArgsConstructor
class TenantSchemaEventListener {
    private final MerchantProvisioningService provisioning;
    private final SimpMessagingTemplate messaging;

    @ApplicationModuleListener
    public void onMerchantCreated(MerchantCreatedEvent event) {
        var result = provisioning.process(event.slug());
        messaging.convertAndSendToUser(result.ownerId().toString(), "/queue/provisioning", result.status());
    }
}
