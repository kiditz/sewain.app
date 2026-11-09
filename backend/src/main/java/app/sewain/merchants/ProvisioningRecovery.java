package app.sewain.merchants;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ProvisioningRecovery {
    private final MerchantRepository merchants;
    private final TenantSchemaEventListener listener;
    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void resume() {
        for (var merchant : merchants.findByProvisioningStatus("PROVISIONING")) {
            listener.onMerchantCreated(new MerchantCreatedEvent(merchant.getName(), merchant.getSchema()));
        }
    }
}
