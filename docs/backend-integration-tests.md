# Backend PostgreSQL integration tests

The `integrationTest` Gradle task runs a separate source set under
`backend/src/integrationTest/java`. It uses Testcontainers with `postgres:16-alpine`,
the real Flyway migration files, JDBC, Hibernate/JPA, Spring transactions and the
application's tenant connection provider. Testcontainers dependency versions are managed by
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

# Business transactions and concurrent stock reservation.
./gradlew integrationTest --tests '*OrderReservationIntegrationTest'

# JPA inventory queries and actual Spring Batch imports.
./gradlew integrationTest --tests '*InventoryRepositoryIntegrationTest'
./gradlew integrationTest --tests '*ProductImportJobIntegrationTest'

# Full application startup, signed-JWT HTTP authorization and real STOMP connections.
./gradlew integrationTest --tests '*BackendStartupIntegrationTest'

# Small regression test for HTTP tenant-access denial; no Docker needed.
./gradlew test --tests '*TenantFilterTest'
```

The first integration run downloads the container image and any missing Gradle
dependencies. Missing Docker access fails the task instead of silently skipping
database coverage. The standard `test`, `check` and `build` tasks do not implicitly
run this integration suite. CI must invoke `integrationTest` explicitly.

The HTML report is `backend/build/reports/tests/integrationTest/index.html`.
JUnit XML reports are under `backend/build/test-results/integrationTest/`.

JaCoCo coverage is generated automatically after the suite at
`backend/build/reports/jacoco/integrationTest/html/index.html`. Run
`./gradlew jacocoCombinedReport` to run both suites and merge their coverage.
See the [coverage guide](backend-coverage.md) for HTML/XML outputs and limitations.

## Isolation and resource use

- Each test class starts a temporary PostgreSQL container, shared by its methods
  and stopped by the JUnit Testcontainers extension after the class finishes.
- Tests use generated host ports and container-only database credentials. The
  lightweight suites do not load application configuration. The full Boot suite
  loads `application.yaml`, overrides database credentials/URLs and media storage,
  disables Compose startup, and replaces the JWT key-pair factory with an in-memory
  key. It never reads or writes development JWT keys. Its media path is under
  `backend/build/integration-test-media/`.
- Each method uses distinct tenant schema names. All test data disappears when
  its container is removed; no development volumes are mounted.
- Classes and methods run sequentially, with one Gradle test worker. JDBC-only
  connection-provider tests use a one-connection pool to force connection reuse.
  Public migration bootstrap and schema-service migration tests use two pooled
  connections: PostgreSQL Flyway holds its main connection while opening another
  for migration work. The tenant bootstrap helper uses Flyway-managed JDBC
  connections independently of the connection-provider test pool. JPA and full Boot
  suites permit four connections for concurrent requests, migration connections,
  and Batch metadata transactions. The connection initialization sets UTC as in
  the production datasource configuration.
- Container reuse is not enabled. See the official [PostgreSQL module](https://java.testcontainers.org/modules/databases/postgres/)
  and [JUnit integration](https://java.testcontainers.org/test_framework_integration/junit_5/)
  documentation for runtime and lifecycle details.

## Coverage (43 integration tests)

| Class | Checks |
| --- | --- |
| `TenantMigrationIntegrationTest` (8 tests) | All public migrations on an empty database, shared tables and seeded country; new tenant defaults for every `SettingKey`; upgrade from tenant V34 with missing settings; preservation of existing merchant preferences; repeat migration without duplicated settings or reset values; relocation of a legacy tenant-owned `btree_gist` extension without invalidating its existing index; maintenance migration across tenants; fail-fast behavior on invalid migration history |
| `TenantConnectionIntegrationTest` (3 tests) | Identical IDs/slugs remain isolated across tenant switches on the same physical connection; released connections reset to the public schema; real Spring transaction rollback and commit against tenant tables, verified outside the transaction |
| `InventoryRepositoryIntegrationTest` (4 tests) | Actual status and joined-product filters, entity graph usability after transaction closure, pagination, variant-aware lookup, JPA tenant switching, and insufficient stock rejection |
| `OrderReservationIntegrationTest` (8 tests) | Two overlapping requests for the last unit; rollback after a later item fails; rollback after all order/payment/allocation writes flush; adjacent rentals and buffer windows; initial payment confirmation/cash allocation; payment rollback; refund rollback and successful retry |
| `ProductImportJobIntegrationTest` (4 tests) | CSV multi-variant import into products/plans/physical inventory and Batch metadata; invalid rows and skipped existing slugs; image failure isolation with sanitized diagnostics; concurrent active-import prevention and recovery marking interrupted imports failed |
| `BackendStartupIntegrationTest` (6 tests) | Full Spring Boot startup on an empty database; persisted READY/FAILED provisioning outcomes; real signed-JWT authentication and database-backed catalog tenant isolation; HTTP 503 while provisioning; STOMP status retrieval across reconnects and rejection of another owner's merchant |
| `CatalogFilterIntegrationTest` (10 tests) | Real SQL collection title/tag/type/variant/attribute rules; ALL/ANY composition; empty rules and snapshot stability; inventory text/status composition; ignored search/sort fields; literal percent/underscore handling |

The migration suite calls the real `MerchantSchemaService` with the real
datasource and JDBC template. Only the unused merchant repository dependency is
mocked. Provider tests use real migrated tenant tables and the real
`TenantConnectionProvider`. No database results or transaction manager are mocked.

Business suites start a focused Spring context with production repositories,
generated mappers, selected production services and the real Hibernate tenant
configuration. Production `@Transactional` proxies perform the writes and
commit/rollback. A test-only synchronous event listener flushes the order's writes
before injecting failures; assertions then read committed database state outside
the failed transaction. The concurrency test uses latches to keep the winning
transaction open while a second request competes for the same unit; it does not
depend on arbitrary sleeps.

Mapper scanning includes catalog/settings implementations, the merchant order
mapper and the settings JSON helper. Unrelated cart and other module mappers are
excluded from this focused context so they do not introduce unused dependencies.
The focused context explicitly uses Hibernate's snake-case physical naming
strategy, matching Spring Boot's mapping of implicit column names such as
`createdAt` to `created_at`.

The import suite uses the actual import worker, service, row service, product
command service, JDBC job repository and job operator. Its test configuration uses
the same Batch transaction-manager and table-prefix settings as the production
configuration. It mocks download transport, STOMP notifications and membership
authorization, which are outside the import-persistence checks. A notification
latch waits for terminal status. An interrupted import is marked failed so the
merchant can upload again; the test does not claim that a failed Batch job resumes.

The full Boot suite starts an embedded server on a random port, loads production
configuration and verifies JWT signatures with a real in-memory RSA key. HTTP
requests use MockMvc with the actual filter chain, controllers, repositories and
database membership. STOMP uses real local WebSocket connections to that server;
the second connection requests the persisted status again, as a refreshed browser
would. This tests backend recovery behavior, not frontend reconnect logic.

## Related fixes

- Public migration `V13__install_shared_btree_gist.sql` installs the shared GiST
  extension in `public` and relocates earlier tenant-owned installations. Existing
  tenant migration checksums are unchanged. The migration tests cover a real
  public V12-to-V13 upgrade and existing index validity.
- `TenantFilter` explicitly returns HTTP 403 on membership denial because it runs
  before Spring Security's exception translation filter. The additional unit test
  `TenantFilterTest` checks the response, controller suppression and context cleanup.

## Boundaries and verification status

This is baseline integration coverage, not exhaustive coverage of every feature.
Follow-up scenarios include full pickup/return/late-fee lifecycle persistence,
authorization for every non-catalog resource, actual remote image HTTP transport,
SMTP/IMAP servers and WhatsApp gateway delivery/recovery. Existing unit tests cover
parts of those rules, but do not establish real transport behavior. Browser refresh
behavior still needs frontend E2E coverage.

Verified locally on **2026-11-14**: all **43 integration tests** and **422
default-suite tests** passed, with zero failures, errors or skips. The integration
suite was explicitly rerun using
`./gradlew integrationTest --rerun-tasks jacocoCombinedReport -x test` after the
complete default suite had passed against the same production source. JaCoCo
regenerated integration and combined reports. The fresh integration command took
2 minutes 54 seconds locally; runtime depends on container startup and resources.
For routine verification, use `./gradlew jacocoCombinedReport`; add
`--rerun-tasks` to force both suites to execute again. The `-x test` shortcut is
only appropriate when complete, compatible unit execution data already exists.
