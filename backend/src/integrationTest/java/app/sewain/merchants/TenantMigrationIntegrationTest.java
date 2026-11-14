package app.sewain.merchants;

import app.sewain.settings.SettingKey;
import app.sewain.testsupport.PostgresIntegrationSupport;
import org.junit.jupiter.api.Test;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class TenantMigrationIntegrationTest extends PostgresIntegrationSupport {
    // The actual schema service passes this datasource to Flyway, which needs two connections.
    @Override protected int poolSize() { return 2; }

    @Test
    void publicMigrationsCreateSharedTablesAndSeedIndonesia() {
        for (var table : Set.of("users", "merchants", "countries", "product_imports",
                "batch_job_instance", "whatsapp_sessions")) {
            assertNotNull(jdbc.queryForObject("SELECT to_regclass(?)::text", String.class, "public." + table), table);
        }
        assertEquals(1L, jdbc.queryForObject("SELECT count(*) FROM public.countries WHERE iso2 = 'ID'", Long.class));
        assertEquals("public", jdbc.queryForObject("SELECT n.nspname FROM pg_extension e"
                + " JOIN pg_namespace n ON n.oid=e.extnamespace WHERE e.extname='btree_gist'", String.class));
        assertEquals(0L, jdbc.queryForObject(
                "SELECT count(*) FROM public.flyway_schema_history WHERE NOT success", Long.class));
    }

    @Test
    void sharedExtensionUpgradeRelocatesLegacyTenantExtensionAndAllowsAnotherTenant() {
        // Use a separate empty database so the real public V12 -> V13 upgrade is exercised.
        var database = schemaName();
        jdbc.execute("CREATE DATABASE " + database);
        var source = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl().replace("/" + POSTGRES.getDatabaseName(), "/" + database),
                POSTGRES.getUsername(), POSTGRES.getPassword());
        var migrated = new JdbcTemplate(source);
        Flyway.configure().dataSource(source).schemas("public")
                .locations("classpath:db/migration").target("12").load().migrate();
        var legacyTenant = schemaName();
        Flyway.configure().dataSource(source).schemas(legacyTenant).createSchemas(true)
                .locations("classpath:db/tenant").target("12").load().migrate();
        assertEquals(legacyTenant, migrated.queryForObject("SELECT n.nspname FROM pg_extension e"
                + " JOIN pg_namespace n ON n.oid=e.extnamespace WHERE e.extname='btree_gist'", String.class));

        Flyway.configure().dataSource(source).schemas("public").locations("classpath:db/migration").load().migrate();

        assertEquals("public", migrated.queryForObject("SELECT n.nspname FROM pg_extension e"
                + " JOIN pg_namespace n ON n.oid=e.extnamespace WHERE e.extname='btree_gist'", String.class));
        assertTrue(migrated.queryForObject("SELECT i.indisvalid FROM pg_index i"
                + " WHERE i.indexrelid = ?::regclass", Boolean.class, legacyTenant + ".ex_rental_allocations_no_overlap"));
        Flyway.configure().dataSource(source).schemas(schemaName()).createSchemas(true)
                .locations("classpath:db/tenant").target("12").load().migrate();
    }

    @Test
    void newTenantRegistersEverySettingWithUsableRegionalAndPaymentDefaults() {
        var schema = schemaName();
        service().createTenantSchema(schema);

        assertAllSettingsPresent(schema);
        assertEquals("Asia/Jakarta", setting(schema, "REGIONAL", "timezone"));
        assertEquals("IDR", setting(schema, "REGIONAL", "currency"));
        assertEquals("true", setting(schema, "PAYMENT", "cashAtPickup"));
        assertEquals("false", setting(schema, "PAYMENT", "bankTransfer"));
        assertEquals("[]", setting(schema, "PAYMENT_BANK_ACCOUNTS", "accounts"));
        assertEquals(jdbc.queryForObject("SELECT id::text FROM public.countries WHERE iso2 = 'ID'", String.class),
                setting(schema, "REGIONAL", "countryId"));
        assertEquals("UTC", jdbc.queryForObject("SHOW TIME ZONE", String.class));
        assertTrue(tenantFlyway(schema, null).validateWithResult().validationSuccessful);
    }

    @Test
    void upgradingOlderTenantBackfillsMissingRegionalAndPaymentSettings() {
        var schema = schemaName();
        tenantFlyway(schema, "34").migrate();
        assertEquals(Set.of("RENTAL_PLAN"), new HashSet<>(
                jdbc.queryForList("SELECT setting_key FROM " + schema + ".settings", String.class)));

        service().createTenantSchema(schema);

        assertAllSettingsPresent(schema);
        assertEquals("Asia/Jakarta", setting(schema, "REGIONAL", "timezone"));
        assertEquals("IDR", setting(schema, "REGIONAL", "currency"));
        assertEquals("true", setting(schema, "PAYMENT", "cashAtPickup"));
    }

    @Test
    void upgradingPreRegionalTenantFillsMissingSettingsWithoutReplacingPreferences() {
        var schema = schemaName();
        tenantFlyway(schema, "34").migrate();
        jdbc.update("INSERT INTO " + schema + ".settings (setting_key, setting_value) VALUES (?, ?::jsonb)",
                "REGIONAL", "{\"currency\":\"JPY\",\"timezone\":\"Asia/Tokyo\",\"locale\":\"ja\"}");
        jdbc.update("INSERT INTO " + schema + ".settings (setting_key, setting_value) VALUES (?, ?::jsonb)",
                "PAYMENT", "{\"cashAtPickup\":false,\"bankTransfer\":true,\"instructions\":\"Keep this\"}");
        jdbc.update("UPDATE " + schema + ".settings SET setting_value = ?::jsonb WHERE setting_key = 'RENTAL_PLAN'",
                "[{\"durationUnit\":\"WEEK\",\"durationValue\":1,\"percentage\":12}]");
        var previous = jdbc.queryForList("SELECT setting_key, setting_value::text FROM " + schema
                + ".settings ORDER BY setting_key");

        service().createTenantSchema(schema);

        assertAllSettingsPresent(schema);
        for (var row : previous) {
            assertEquals(row.get("setting_value"), jdbc.queryForObject("SELECT setting_value::text FROM "
                    + schema + ".settings WHERE setting_key = ?", String.class, row.get("setting_key")));
        }
        assertEquals("2", setting(schema, "RENTAL", "minimumNoticeHours"));
    }

    @Test
    void migratingAgainDoesNotDuplicateSettingsOrResetMerchantChanges() {
        var schema = schemaName();
        var service = service();
        service.createTenantSchema(schema);
        jdbc.update("UPDATE " + schema + ".settings SET setting_value = jsonb_set(setting_value, '{currency}', '\"EUR\"')"
                + " WHERE setting_key = 'REGIONAL'");
        var migrationCount = jdbc.queryForObject("SELECT count(*) FROM " + schema + ".flyway_schema_history", Long.class);

        service.createTenantSchema(schema);

        assertAllSettingsPresent(schema);
        assertEquals("EUR", setting(schema, "REGIONAL", "currency"));
        assertEquals(migrationCount, jdbc.queryForObject("SELECT count(*) FROM " + schema + ".flyway_schema_history", Long.class));
    }

    private MerchantSchemaService service() {
        // Migration SQL and JDBC are real; the merchant listing is unused in these provisioning calls.
        return new MerchantSchemaService(jdbc, dataSource, mock(MerchantRepository.class));
    }

    private void assertAllSettingsPresent(String schema) {
        var keys = jdbc.queryForList("SELECT setting_key FROM " + schema + ".settings", String.class);
        assertEquals(Arrays.stream(SettingKey.values()).map(Enum::name).collect(Collectors.toSet()), new HashSet<>(keys));
        assertEquals(SettingKey.values().length, keys.size());
        assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM " + schema
                + ".settings WHERE setting_value IS NULL", Long.class));
    }

    private String setting(String schema, String key, String property) {
        return jdbc.queryForObject("SELECT setting_value ->> ? FROM " + schema + ".settings WHERE setting_key = ?",
                String.class, property, key);
    }
}
