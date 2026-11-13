package app.sewain.testsupport;

import app.sewain.tenant.JpaIntegrationConfiguration;
import app.sewain.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.function.Supplier;

public abstract class JpaIntegrationSupport extends PostgresIntegrationSupport {
    protected AnnotationConfigApplicationContext context;
    protected String tenant;
    protected OffsetDateTime start;
    protected OffsetDateTime end;

    @Override protected int poolSize() { return 4; }

    @BeforeEach
    void openJpaContext() {
        tenant = migratedTenant();
        TenantContext.set(tenant);
        start = OffsetDateTime.now(ZoneOffset.UTC).plusDays(7).withHour(3).withMinute(0).withSecond(0).withNano(0);
        end = start.plusDays(2);
        context = new AnnotationConfigApplicationContext();
        context.registerBean(DataSource.class, () -> dataSource);
        context.register(JpaIntegrationConfiguration.class);
        customizeContext(context);
        context.refresh();
    }

    protected void customizeContext(AnnotationConfigApplicationContext context) { }

    @AfterEach
    void closeJpaContext() {
        try {
            if (context != null) context.close();
        } finally {
            TenantContext.clear();
        }
    }

    protected <T> T inTransaction(Supplier<T> action) {
        return new TransactionTemplate(context.getBean(PlatformTransactionManager.class)).execute(status -> action.get());
    }

    protected CatalogFixture catalogFixture(String name, String unitStatus) {
        var product = UUID.randomUUID();
        var variant = UUID.randomUUID();
        var unit = UUID.randomUUID();
        jdbc.update("INSERT INTO " + tenant + ".products(id,name,slug,status) VALUES (?,?,?,'ACTIVE')",
                product, name, "product-" + product);
        jdbc.update("INSERT INTO " + tenant + ".product_variants(id,product_id,name,sku,deposit_amount) VALUES (?,?,?,?,50000)",
                variant, product, name + " variant", "SKU-" + variant);
        jdbc.update("INSERT INTO " + tenant + ".variant_rental_plans(id,variant_id,duration_unit,duration_value,rental_fee) VALUES (?,?,'DAY',1,100000)",
                UUID.randomUUID(), variant);
        jdbc.update("INSERT INTO " + tenant + ".inventory_units(id,variant_id,sku_barcode,status) VALUES (?,?,?,?)",
                unit, variant, "UNIT-" + unit, unitStatus);
        return new CatalogFixture(product, variant, unit);
    }

    protected long count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tenant + "." + table, Long.class);
    }

    protected record CatalogFixture(UUID productId, UUID variantId, UUID unitId) { }
}
