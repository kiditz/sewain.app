package app.sewain.testsupport;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

/** Only container-generated credentials are used; application.yaml is never loaded. */
@Testcontainers
public abstract class PostgresIntegrationSupport {
    @Container
    protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("sewain_integration")
            .withUsername("sewain_test")
            .withPassword("integration_test_only")
            .withEnv("TZ", "UTC");

    protected HikariDataSource dataSource;
    protected JdbcTemplate jdbc;

    @BeforeAll
    static void migratePublicSchema() {
        try (var source = newDataSource(1)) {
            Flyway.configure().dataSource(source).schemas("public")
                    .locations("classpath:db/migration").load().migrate();
        }
    }

    @BeforeEach
    void openPool() {
        dataSource = newDataSource(poolSize());
        jdbc = new JdbcTemplate(dataSource);
    }

    @AfterEach
    void closePool() {
        if (dataSource != null) dataSource.close();
    }

    protected int poolSize() { return 1; }

    private static HikariDataSource newDataSource(int poolSize) {
        var config = new HikariConfig();
        config.setJdbcUrl(POSTGRES.getJdbcUrl());
        config.setUsername(POSTGRES.getUsername());
        config.setPassword(POSTGRES.getPassword());
        // Force the same physical connection to be reused across tenant switches.
        config.setMaximumPoolSize(poolSize);
        config.setMinimumIdle(0);
        config.setConnectionTimeout(5_000);
        config.setConnectionInitSql("SET TIME ZONE 'UTC'");
        return new HikariDataSource(config);
    }

    protected String schemaName() {
        return "it_" + UUID.randomUUID().toString().replace("-", "");
    }

    protected String migratedTenant() {
        var schema = schemaName();
        tenantFlyway(schema, null).migrate();
        return schema;
    }

    protected Flyway tenantFlyway(String schema, String target) {
        var configuration = Flyway.configure().dataSource(dataSource)
                .schemas(schema).createSchemas(true).locations("classpath:db/tenant");
        if (target != null) configuration.target(target);
        return configuration.load();
    }
}
