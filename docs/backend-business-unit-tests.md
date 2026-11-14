# Backend default tests

Business unit tests run under the standard Gradle `test` task in
`backend/src/test/java`. There is no separate business test task or source set.
The suite also includes lightweight HTTP, filesystem and module-boundary checks.
It does not require Docker or a running application/database.

## Commands

Use JDK 21. From `backend/`:

```sh
# Complete default suite and its JaCoCo report.
./gradlew test

# One test class.
./gradlew test --tests '*CountryQueryServiceTest'

# Selected related classes.
./gradlew test --tests '*RentalPriceCalculatorTest' --tests '*LateFeeCalculatorTest'

# Fresh execution of the complete suite.
./gradlew test --rerun-tasks

# Optional 100% line and branch gate for this suite's measured scope.
./gradlew jacocoTestCoverageVerification
```

Gradle compiles all default test sources before applying `--tests` filters.
Compilation errors in another class must therefore be fixed before a selected test
can run. Filtered runs produce coverage for that selection only; rerun the full
suite before assessing its overall coverage.

Open `build/reports/tests/test/index.html` for pass/fail results and
`build/reports/jacoco/test/html/index.html` for coverage. JUnit XML is under
`build/test-results/test/`.

## Behaviors covered

The following groups summarize the suite rather than enumerate every test class.
Tests instantiate production services with mocked external dependencies and use
real generated mappers where practical. Transport tests use controlled test doubles
or in-memory HTTP responses rather than contacting merchant accounts.

| Area | Representative checks |
| --- | --- |
| Authentication and profiles | Registration, duplicate email, password encoding/change rules, token claims, principal lookup and missing accounts |
| Tenant and merchant access | Membership, provisioning readiness, unknown stores, HTTP access denial and tenant-context cleanup |
| Countries and settings | Empty country lists, ordered repository lookup, response/timezone mapping, setting JSON conversion, duplicate keys and missing settings |
| Catalog and inventory | Slugs, variant/child ownership, pricing snapshots, barcode normalization, physical inventory mutation/deletion guards and public product lookups |
| Collections and search | Manual ordering, automatic rule composition, wildcard escaping, scalar conversion, nested groups, approved sorting and Criteria query construction |
| Pricing and late fees | Cheapest rental packages, rounding, timezones/DST, grace boundaries, quantity, snapshot versus current policy and legacy fallbacks |
| Cart, checkout and orders | Ownership, empty carts, pickup/delivery windows, payment options, deposit separation, transitions, late-fee quotes and stock availability |
| Payments and finance | Pending/failed receipts, refund allocations, net rental/deposit totals, refund ownership, duplicate events and ledger categories |
| Reports | Calendar buckets, growth/zero denominators, date validation, timezone boundaries and JDBC row projections |
| CSV product import | Grouped variants, validation, partial failures, safe diagnostics, job status, interrupted-import recovery and tenant cleanup |
| Media | Upload normalization, bounded remote downloads, URL/address validation, file storage/replacement, traversal guards and transaction cleanup |
| Email | Settings/credential preservation, tenant access, draft validation, idempotency, SMTP/IMAP configuration, MIME parsing, charset fallback and body limits |
| WhatsApp | Gateway request/response handling, connection/disconnection state, authorization, failures and sanitized diagnostics |
| Storefront | Public product/collection lookups, theme rename/publish/history behavior and configuration snapshots |

## Choosing the right test layer

Use unit tests for service decisions, calculations and observable failure paths.
Mocking an ordered repository query verifies that the service requests that query
and preserves its result order; it does not establish PostgreSQL ordering.
Likewise, mocked Criteria APIs do not prove actual SQL filtering, and mocked
repositories do not prove transaction rollback or locking.

The separate [integration suite](backend-integration-tests.md) tests real
migrations, SQL filters, tenant switching, transactions, concurrent reservations,
Batch persistence, Boot startup and STOMP connections. Real external SMTP/IMAP and
WhatsApp delivery are outside the current automated suites.

Hibernate-generated entity methods do not need their own business unit tests.
All JaCoCo reports share an explicit application-code scope that excludes entity
classes. See the [coverage guide](backend-coverage.md) for the exact approach,
report commands and the optional gate. Generated mappers and entities can still
participate in behavioral tests even though their classes are outside that scope.

## Verification snapshot

On **2026-11-14**, the complete default suite passed **422 tests**, with zero
failures, errors or skips. The same verification cycle passed 43 PostgreSQL
integration tests. Default-suite coverage was **98.01% lines / 85.09% branches**;
combined coverage was **98.74% / 85.48%** within the configured scope. The 100%
coverage gate is not yet satisfied. See the coverage guide for denominators and
remaining gaps; regenerate reports after changes rather than treating this
snapshot as a current guarantee.
