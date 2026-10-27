package app.sewain.tenant;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

@Component
class TenantIdentifierResolver
        implements CurrentTenantIdentifierResolver<String> {

    @Override
    public String resolveCurrentTenantIdentifier() {
        String tenant = TenantContext.get();

        return StringUtils.isBlank(tenant)
                ? "public"
                : tenant;
    }


    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
