package app.sewain.integrations;

import app.sewain.tenant.TenantContext;
import org.springframework.security.access.AccessDeniedException;

public final class IntegrationAccess {
    private IntegrationAccess() {}
    public static String tenant() {
        var tenant = TenantContext.get();
        if (tenant == null || tenant.isBlank() || "public".equals(tenant))
            throw new AccessDeniedException("integrations.tenantRequired");
        return tenant;
    }
}
