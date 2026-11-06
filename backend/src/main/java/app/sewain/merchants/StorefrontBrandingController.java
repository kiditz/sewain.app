package app.sewain.merchants;

import app.sewain.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
class StorefrontBrandingController {
    private final MerchantRepository repository;
    private final app.sewain.settings.SettingFacade settings;

    @GetMapping("/api/storefront/branding")
    Branding get() {
        var merchant = repository.findBySchema(TenantContext.get())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var information = merchant.getStoreInformation();
        return new Branding(merchant.getName(),
                information == null ? null : information.getLogoUrl(),
                information == null ? null : information.getFaviconUrl(), regional());
    }

    private Regional regional() {
        try {
            var value = settings.getValue(app.sewain.settings.SettingKey.REGIONAL);
            return new Regional(value.path("timezone").asString("Asia/Jakarta"),
                    value.path("locale").asString("id"), value.path("dateFormat").asString("DD/MM/YYYY"));
        } catch (app.sewain.exception.ResourceNotFoundException missing) {
            return new Regional("Asia/Jakarta", "id", "DD/MM/YYYY");
        }
    }
    record Regional(String timezone, String locale, String dateFormat) {}
    record Branding(String name, String logoUrl, String faviconUrl, Regional regional) {}
}
