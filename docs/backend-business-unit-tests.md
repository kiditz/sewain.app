# Backend business unit tests

## Additional coverage tests (not yet executed)

An additional 81 tests across 13 classes exercise service decisions with mocked
repositories/transports and real generated mappers where practical. They require
neither Docker nor a running application. These additions have only been reviewed
statically; they have not been compiled or run, at the user's request.

| Class | Tests | Main scenarios |
| --- | ---: | --- |
| AuthServiceTest | 6 | Registration, duplicate email, password encoding, JWT claims/expiry, authentication failures, principal identity |
| ProfileCommandServiceTest | 5 | Current password, confirmation, password reuse, missing accounts, profile mapping |
| AttributeCommandServiceTest | 8 | Slug conflicts, child ownership, update/add/remove values, missing IDs, deletion |
| CollectionCommandServiceTest | 11 | Parent placement, manual item identity/order, automatic rules, missing products, sibling reorder, deletion |
| MerchantOrderCreationServiceTest | 9 | Totals, percentage/fixed down payment, waiver, buffered allocations, disabled payment choices, rental windows and mismatched variants |
| CheckoutServiceTest | 5 | Cart ownership/emptiness, pickup/delivery dates, address, failure retention, payment options |
| CustomerServiceTest | 6 | Normalization, duplicates, aggregate amounts/timestamps, enable/disable, empty search, UTC statistics |
| SettingCommandServiceTest | 5 | JSON mapping, duplicate key, identity-preserving update, missing settings, deletion |
| FinanceServiceTest | 5 | Net rental/deposit totals, timezone boundaries, pagination limits, invalid date ranges, expenses |
| FinanceMapperTest | 2 | Rental/deposit payment and refund allocations with correct source IDs |
| FinanceEventListenerTest | 4 | Separate ledger categories, duplicate events, nonpositive amounts, refund expenses |
| MerchantReportServiceTest | 5 | Empty periods, calendar buckets, growth rates, zero denominators, invalid periods, timezone boundaries |
| EmailSettingsServiceTest | 10 | Tenant requirement, credentials/configuration, direction checks, drafts, injection, password preservation, disconnect, redaction |

From `backend/`, run `./gradlew test` for the complete default suite. Then run
`./gradlew jacocoCombinedReport` with Docker active to regenerate the combined
report. The previous measured coverage (48.02% lines, 32.33% branches) predates
these additions. No new percentage or achievement of the 80% target is claimed.

## Original business test baseline

Added 40 JUnit tests across six classes. These instantiate the calculators/services directly and mock external dependencies; no Spring application context, database, Docker, network requests, or real messages are required. No production business rules were changed.

| Test class | Tests | Business behavior |
| --- | ---: | --- |
| RentalPriceCalculatorTest | 10 | Cheapest package combination, over-coverage, tied prices, week/month conversion, free plans, merchant dates, DST, invalid periods and unusable plans |
| LateFeeCalculatorTest | 7 | Exact grace boundary, whole-delay charging, ceiling per day, quantity and package proration, rounding, disabled fees, offset timestamps and legacy configuration |
| MerchantOrderPaymentServiceTest | 8 | Net cash after successful refunds, pending/failed payments excluded, negative net protection, closed-order receipts, duplicate/incorrect initial payment, reservation confirmation, deposit-return gate and refund ownership/remaining amount |
| MerchantOrderLifecycleServiceTest | 5 | Forbidden transitions, cancellation stock release, pending-payment cancellation, tenant-aware inventory events, unpaid pickup guard and completed-return availability |
| InventoryCommandServiceTest | 6 | Normalized duplicate barcodes, generated serials, variant resolution, deletion restrictions for assigned/allocated inventory and deletion of unused inventory |
| TenantAccessServiceTest | 4 | Exact tenant/user membership, shared public context, provisioning readiness and unknown-store resolution |

`MerchantOrderQueryServiceTest` adds three payment-summary regression tests:
partial refunds reduce the received total and rental/deposit allocations, failed
refunds are ignored, fully refunded payments contribute zero, and pending/failed
receipts do not count as paid. Run them separately with:

```sh
./gradlew test --tests '*MerchantOrderQueryServiceTest'
```

From the `backend` directory:

```sh
./gradlew test --tests '*RentalPriceCalculatorTest' --tests '*LateFeeCalculatorTest' --tests '*MerchantOrderPaymentServiceTest' --tests '*MerchantOrderLifecycleServiceTest' --tests '*InventoryCommandServiceTest' --tests '*TenantAccessServiceTest'
```

Gradle still compiles all test sources before applying `--tests` filters. Compilation failures in another test class must be fixed before these tests can run. The normal HTML report is `backend/build/reports/tests/test/index.html`.

JaCoCo coverage is generated automatically after `./gradlew test`. Open
`backend/build/reports/jacoco/test/html/index.html` to inspect lines and branches.
Filtered test runs produce coverage for the selection only. See the
[coverage guide](backend-coverage.md) for full-suite and combined report commands.

Verified on 2026-11-13 with `./gradlew jacocoCombinedReport`: all 140 tests in the
default suite passed, with zero failures, errors or skips. All 31 PostgreSQL
integration tests also passed. `git diff --check` passed. The unit tests above do
not by themselves verify database locks, concurrent overbooking, real transaction
rollback or migration compatibility; see the separate
[integration suite](backend-integration-tests.md) for database coverage.
