# Backend PostgreSQL integration tests

The `integrationTest` Gradle task runs a separate source set under
`backend/src/integrationTest/java`. It uses Testcontainers with `postgres:16-alpine`,
the real Flyway migration files, JDBC, Spring transactions and the application's
tenant connection provider. Testcontainers dependency versions are managed by
Spring Boot; the modules use the Testcontainers 2 artifact names.

## Requirements and commands

Use JDK 21 and a running Docker-compatible container runtime. The application,
development database and Compose services do not need to be running.

From `backend/`:

```sh
# Existing unit and lightweight tests; does not start Testcontainers.
./gradlew test

# PostgreSQL integration tests only.
./gradlew integrationTest

# Run both suites explicitly.
./gradlew test integrationTest

# Run one integration test class.
./gradlew integrationTest --tests 'app.sewain.merchants.TenantMigrationIntegrationTest'
./gradlew integrationTest --tests 'app.sewain.tenant.TenantConnectionIntegrationTest'
```

The first integration run downloads the container image and any missing Gradle
dependencies. Missing Docker access fails the task instead of silently skipping
database coverage. The standard `test`, `check` and `build` tasks do not implicitly
run this integration suite. CI must invoke `integrationTest` explicitly.

The HTML report is `backend/build/reports/tests/integrationTest/index.html`.
JUnit XML reports are under `backend/build/test-results/integrationTest/`.

## Isolation and resource use

- Each test class starts a temporary PostgreSQL container, shared by its methods
  and stopped by the JUnit Testcontainers extension after the class finishes.
- Tests use generated host ports and container-only database credentials. They
  never read `application.yaml`, `.env` or development database credentials.
- Each method uses distinct tenant schema names. All test data disappears when
  its container is removed; no development volumes are mounted.
- Classes and methods run sequentially, with one Gradle test worker and a Hikari
  pool limited to one connection. The connection initialization sets UTC as in
  the production datasource configuration.
- Container reuse is not enabled. See the official [PostgreSQL module](https://java.testcontainers.org/modules/databases/postgres/)
  and [JUnit integration](https://java.testcontainers.org/test_framework_integration/junit_5/)
  documentation for runtime and lifecycle details.

## Initial coverage

| Class | Checks |
| --- | --- |
| `TenantMigrationIntegrationTest` (5 tests) | All public migrations on an empty database, shared tables and seeded country; new tenant defaults for every `SettingKey`; upgrade from tenant V34 with missing settings; preservation of existing merchant preferences; repeat migration without duplicated settings or reset values |
| `TenantConnectionIntegrationTest` (3 tests) | Identical IDs/slugs remain isolated across tenant switches on the same physical connection; released connections reset to the public schema; real Spring transaction rollback and commit against tenant tables, verified outside the transaction |

Tenant provisioning tests call the real `MerchantSchemaService` with the real
datasource and JDBC template. Only the unused merchant repository dependency is
mocked. Provider tests use real migrated tenant tables and the real
`TenantConnectionProvider`. No database results or transaction manager are mocked.

## Boundaries and verification status

This suite does not start the full Spring Boot application or Hibernate. The
transaction test exercises Spring's JDBC transaction manager with a connection
obtained from the tenant provider; it does not establish that every application's
`@Transactional` service boundary is correct. The schema service is instantiated
directly, so its Spring transaction proxy is outside these migration tests.

Follow-up coverage is still needed for JPA repository queries, concurrent stock
reservation/overbooking, application startup, actual Spring Batch import jobs,
STOMP recovery, and external email/WhatsApp behavior.

These tests were added without running Gradle, Docker or the test suite, at the
user's request. Compilation and runtime results must be confirmed using the
commands above.
