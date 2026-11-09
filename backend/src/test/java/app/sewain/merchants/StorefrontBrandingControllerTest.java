package app.sewain.merchants;

import app.sewain.tenant.TenantContext;
import app.sewain.settings.SettingFacade;
import app.sewain.settings.SettingKey;
import app.sewain.exception.ResourceNotFoundException;
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
        var settings = mock(SettingFacade.class);
        when(settings.getValue(SettingKey.REGIONAL)).thenThrow(new ResourceNotFoundException("setting.notFound"));
        var mvc = MockMvcBuilders.standaloneSetup(new StorefrontBrandingController(repository, settings)).build();
        mvc.perform(get("/api/storefront/branding"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"name":"Adventure Rental","logoUrl":"https://example.com/logo.png","faviconUrl":"https://example.com/icon.png","regional":{"timezone":"Asia/Jakarta","locale":"id","dateFormat":"DD/MM/YYYY"}}
                        """, org.springframework.test.json.JsonCompareMode.STRICT));
        verify(repository).findBySchema("adventure");
        verify(settings).getValue(SettingKey.REGIONAL);
    }

    @Test
    void returnsNotFoundForUnknownTenant() throws Exception {
        var repository = mock(MerchantRepository.class);
        TenantContext.set("unknown");
        when(repository.findBySchema("unknown")).thenReturn(Optional.empty());
        var settings = mock(SettingFacade.class);
        when(settings.getValue(SettingKey.REGIONAL)).thenThrow(new ResourceNotFoundException("setting.notFound"));
        var mvc = MockMvcBuilders.standaloneSetup(new StorefrontBrandingController(repository, settings)).build();
        mvc.perform(get("/api/storefront/branding")).andExpect(status().isNotFound());
        verifyNoInteractions(settings);
    }
    @Test
    void exposesConfiguredRegionalSettings() throws Exception {
        var repository = mock(MerchantRepository.class);
        var settings = mock(SettingFacade.class);
        var merchant = new Merchant();
        merchant.setName("Adventure Rental");
        TenantContext.set("adventure");
        when(repository.findBySchema("adventure")).thenReturn(Optional.of(merchant));
        when(settings.getValue(SettingKey.REGIONAL)).thenReturn(
                tools.jackson.databind.json.JsonMapper.builder().build().readTree("""
                        {"timezone":"Asia/Makassar","locale":"en","dateFormat":"YYYY-MM-DD"}
                        """));
        var mvc = MockMvcBuilders.standaloneSetup(new StorefrontBrandingController(repository, settings)).build();
        mvc.perform(get("/api/storefront/branding"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regional.timezone").value("Asia/Makassar"))
                .andExpect(jsonPath("$.regional.locale").value("en"))
                .andExpect(jsonPath("$.regional.dateFormat").value("YYYY-MM-DD"));
    }
}
