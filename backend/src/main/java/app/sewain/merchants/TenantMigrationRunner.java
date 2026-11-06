package app.sewain.merchants;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Explicit deployment-time operation; never exposed through a merchant HTTP route. */
@Component
@ConditionalOnProperty(name = "app.maintenance.migrate-tenants", havingValue = "true")
@RequiredArgsConstructor
class TenantMigrationRunner implements ApplicationRunner {
    private final MerchantSchemaService schemas;
    @Override public void run(ApplicationArguments args) { schemas.migrateAllTenantSchemas(); }
}
