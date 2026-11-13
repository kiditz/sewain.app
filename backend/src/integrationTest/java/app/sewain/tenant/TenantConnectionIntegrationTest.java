package app.sewain.tenant;

import app.sewain.testsupport.PostgresIntegrationSupport;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Connection;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TenantConnectionIntegrationTest extends PostgresIntegrationSupport {
    @Test
    void sameIdentifiersAndSlugsStayIsolatedWhenOnePhysicalConnectionSwitchesTenants() throws Exception {
        var first = migratedTenant();
        var second = migratedTenant();
        var provider = new TenantConnectionProvider(dataSource);
        var id = UUID.randomUUID();

        int firstPid;
        var connection = provider.getConnection(first);
        try {
            firstPid = backendPid(connection);
            insertCollection(connection, id, "First store");
        } finally {
            provider.releaseConnection(first, connection);
        }
        connection = provider.getConnection(second);
        try {
            assertEquals(firstPid, backendPid(connection), "Must reuse the same physical connection");
            assertEquals(0, collectionCount(connection));
            insertCollection(connection, id, "Second store");
        } finally {
            provider.releaseConnection(second, connection);
        }
        for (var schema : new String[]{first, second, first}) {
            connection = provider.getConnection(schema);
            try (var statement = connection.prepareStatement("SELECT name FROM collections WHERE id = ?")) {
                statement.setObject(1, id);
                try (var result = statement.executeQuery()) {
                    assertTrue(result.next());
                    assertEquals(schema.equals(first) ? "First store" : "Second store", result.getString(1));
                    assertFalse(result.next());
                }
                assertEquals(1, collectionCount(connection));
            } finally {
                provider.releaseConnection(schema, connection);
            }
        }
    }

    @Test
    void releasedTenantConnectionReturnsToPublicBeforeSharedQueries() throws Exception {
        var schema = migratedTenant();
        var provider = new TenantConnectionProvider(dataSource);
        var connection = provider.getConnection(schema);
        int pid;
        try {
            pid = backendPid(connection);
            assertEquals(schema, scalar(connection, "SELECT current_schema()"));
        } finally {
            provider.releaseConnection(schema, connection);
        }

        connection = provider.getAnyConnection();
        try {
            assertEquals(pid, backendPid(connection));
            assertEquals("public", scalar(connection, "SHOW search_path"));
            assertEquals("public", scalar(connection, "SELECT current_schema()"));
            assertNull(scalar(connection, "SELECT to_regclass('settings')::text"));
            assertEquals("merchants", scalar(connection, "SELECT to_regclass('merchants')::text"));
        } finally {
            provider.releaseAnyConnection(connection);
        }
    }

    @Test
    void failedSpringTransactionRollsBackWritesAndKeepsOtherTenantUntouched() throws Exception {
        var schema = migratedTenant();
        var other = migratedTenant();
        var provider = new TenantConnectionProvider(dataSource);
        var connection = provider.getConnection(schema);
        try {
            var source = new SingleConnectionDataSource(connection, true);
            var tenantJdbc = new JdbcTemplate(source);
            var transaction = new TransactionTemplate(new DataSourceTransactionManager(source));
            var id = UUID.randomUUID();
            assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status -> {
                tenantJdbc.update("INSERT INTO collections (id, name, slug) VALUES (?, ?, ?)", id, "Rolled back", "rollback");
                assertEquals(1, tenantJdbc.queryForObject("SELECT count(*) FROM collections", Integer.class));
                throw new IllegalStateException("Simulated failure after write");
            }));
            assertEquals(0, collectionCount(connection));

            transaction.executeWithoutResult(status -> tenantJdbc.update(
                    "INSERT INTO collections (id, name, slug) VALUES (?, ?, ?)", id, "Committed", "committed"));
        } finally {
            provider.releaseConnection(schema, connection);
        }
        // Verify after releasing/reacquiring, outside the transaction that performed the write.
        assertEquals(1L, jdbc.queryForObject("SELECT count(*) FROM " + schema + ".collections", Long.class));
        assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM " + other + ".collections", Long.class));
    }

    private void insertCollection(Connection connection, UUID id, String name) throws Exception {
        try (var statement = connection.prepareStatement("INSERT INTO collections (id, name, slug) VALUES (?, ?, ?)")) {
            statement.setObject(1, id);
            statement.setString(2, name);
            statement.setString(3, "same-slug");
            statement.executeUpdate();
        }
    }

    private int collectionCount(Connection connection) throws Exception {
        return Integer.parseInt(scalar(connection, "SELECT count(*) FROM collections"));
    }

    private int backendPid(Connection connection) throws Exception {
        return Integer.parseInt(scalar(connection, "SELECT pg_backend_pid()"));
    }

    private String scalar(Connection connection, String sql) throws Exception {
        try (var statement = connection.createStatement(); var result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getString(1);
        }
    }
}
