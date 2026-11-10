package app.sewain.tenant;

import java.util.UUID;

/** Tenant resolution/access contract; merchant membership supplies the implementation. */
public interface TenantAccess {
    String getSchema(String slug);
    void validateAccess(UUID userId, String tenant);
}
