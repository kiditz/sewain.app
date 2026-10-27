
package app.sewain.tenant;

import app.sewain.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Component
@RequiredArgsConstructor
@Slf4j
class TenantConnectionProvider implements MultiTenantConnectionProvider<String> {

	private final DataSource dataSource;

	@Override
	public Connection getAnyConnection() throws SQLException {
		return dataSource.getConnection();
	}

	@Override
	public void releaseAnyConnection(Connection connection) throws SQLException {
		connection.close();
	}

	@Override
	public Connection getConnection(String tenantIdentifier) throws SQLException {
		Connection connection = getAnyConnection();

		try (Statement statement = connection.createStatement()) {
			statement.execute(
					"SET search_path TO " + quoteSchema(tenantIdentifier)
			);

			log.debug(
					"Acquired connection for tenant [{}], search_path [{}]",
					tenantIdentifier,
					getSearchPath(connection)
			);

			return connection;
		} catch (SQLException e) {
			connection.close();
			throw e;
		}
	}

	@Override
	public void releaseConnection(
			String tenantIdentifier,
			Connection connection
	) throws SQLException {
		try (connection) {
			try (Statement statement = connection.createStatement()) {
				statement.execute("SET search_path TO public");
			}
		}
	}

	private String quoteSchema(String schema) {
		if (schema == null || !schema.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
			throw new IllegalArgumentException(ErrorCode.MESSAGE_INVALID_TENANT_SCHEMA + schema);
		}

		return "\"" + schema + "\"";
	}

	private String getSearchPath(Connection connection) {
		try (Statement statement = connection.createStatement();
		     var result = statement.executeQuery("SHOW search_path")) {
			return result.next() ? result.getString(1) : "unknown";
		} catch (SQLException e) {
			return "unknown";
		}
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return false;
	}

	@Override
	public boolean isUnwrappableAs(Class<?> unwrapType) {
		return false;
	}

	@Override
	public <T> T unwrap(Class<T> unwrapType) {
		return null;
	}
}
