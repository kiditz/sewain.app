package app.sewain.merchants;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantAccessServiceTest {
    private final MerchantUserRepository members=mock(MerchantUserRepository.class);
    private final MerchantRepository merchants=mock(MerchantRepository.class);
    private final TenantAccessService service=new TenantAccessService(members,merchants);
    @Test void membershipIsCheckedForExactUserAndTenantSchema() {
        var user=UUID.randomUUID(); when(members.existsBySchemaAndUserId("outdoor",user)).thenReturn(true);
        assertDoesNotThrow(()->service.validateAccess(user,"outdoor"));
        assertThrows(AccessDeniedException.class,()->service.validateAccess(user,"another_shop"));
        assertThrows(AccessDeniedException.class,()->service.validateAccess(UUID.randomUUID(),"outdoor"));
    }
    @Test void sharedPublicContextDoesNotRequireMerchantMembership() {
        service.validateAccess(UUID.randomUUID(),"public"); verifyNoInteractions(members);
    }
    @Test void onlyReadyMerchantResolvesToItsPrivateSchema() {
        var merchant=new Merchant(); merchant.setSchema("outdoor_schema"); merchant.setProvisioningStatus("READY");
        when(merchants.findBySlug("outdoor")).thenReturn(Optional.of(merchant));
        assertEquals("outdoor_schema",service.getSchema("outdoor"));
        for (var state:List.of("PROVISIONING","FAILED")) {
            merchant.setProvisioningStatus(state);
            var error=assertThrows(ResponseStatusException.class,()->service.getSchema("outdoor"));
            assertEquals(503,error.getStatusCode().value());
        }
    }
    @Test void unknownStoreUsesSharedPublicContextInsteadOfAnotherTenant() {
        when(merchants.findBySlug("unknown")).thenReturn(Optional.empty());
        assertEquals("public",service.getSchema("unknown"));
    }
}
