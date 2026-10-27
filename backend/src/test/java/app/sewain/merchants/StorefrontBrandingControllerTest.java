package app.sewain.merchants;

import app.sewain.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class StorefrontBrandingControllerTest {
    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void exposesOnlyBrandingForTheResolvedTenantWithoutAuthentication() throws Exception {
        var repository = mock(MerchantRepository.class);
        var merchant = new Merchant();
        merchant.setName("Adventure Rental");
        merchant.setSchema("adventure");
        merchant.getStoreInformation().setLogoUrl("https://example.com/logo.png");
        merchant.getStoreInformation().setFaviconUrl("https://example.com/icon.png");
        merchant.getBusinessInformation().setTaxId("private-tax-id");
        TenantContext.set("adventure");
        when(repository.findBySchema("adventure")).thenReturn(Optional.of(merchant));
        var mvc = MockMvcBuilders.standaloneSetup(new StorefrontBrandingController(repository)).build();
        mvc.perform(get("/api/storefront/branding"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"name":"Adventure Rental","logoUrl":"https://example.com/logo.png","faviconUrl":"https://example.com/icon.png"}
                        """, org.springframework.test.json.JsonCompareMode.STRICT));
        verify(repository).findBySchema("adventure");
    }

    @Test
    void returnsNotFoundForUnknownTenant() throws Exception {
        var repository = mock(MerchantRepository.class);
        TenantContext.set("unknown");
        when(repository.findBySchema("unknown")).thenReturn(Optional.empty());
        var mvc = MockMvcBuilders.standaloneSetup(new StorefrontBrandingController(repository)).build();
        mvc.perform(get("/api/storefront/branding")).andExpect(status().isNotFound());
    }
}
