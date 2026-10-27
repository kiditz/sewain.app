package app.sewain.merchants;

import app.sewain.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
class StorefrontBrandingController {
    private final MerchantRepository repository;

    @GetMapping("/api/storefront/branding")
    @Transactional(readOnly = true)
    Branding get() {
        var merchant = repository.findBySchema(TenantContext.get())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var information = merchant.getStoreInformation();
        return new Branding(merchant.getName(),
                information == null ? null : information.getLogoUrl(),
                information == null ? null : information.getFaviconUrl());
    }

    record Branding(String name, String logoUrl, String faviconUrl) {}
}
