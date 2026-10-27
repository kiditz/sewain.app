package app.sewain.merchants;

import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class TenantSchemaEventListener {

    private final MerchantSchemaService tenantSchemaService;

    @ApplicationModuleListener
    public void onMerchantCreated(MerchantCreatedEvent event) {
        tenantSchemaService.createTenantSchema(event.slug());
    }
}
