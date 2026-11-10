# Backend business unit tests

Added 40 JUnit tests across six classes. These instantiate the calculators/services directly and mock external dependencies; no Spring application context, database, Docker, network requests, or real messages are required. No production business rules were changed.

| Test class | Tests | Business behavior |
| --- | ---: | --- |
| RentalPriceCalculatorTest | 10 | Cheapest package combination, over-coverage, tied prices, week/month conversion, free plans, merchant dates, DST, invalid periods and unusable plans |
| LateFeeCalculatorTest | 7 | Exact grace boundary, whole-delay charging, ceiling per day, quantity and package proration, rounding, disabled fees, offset timestamps and legacy configuration |
| MerchantOrderPaymentServiceTest | 8 | Net cash after successful refunds, pending/failed payments excluded, negative net protection, closed-order receipts, duplicate/incorrect initial payment, reservation confirmation, deposit-return gate and refund ownership/remaining amount |
| MerchantOrderLifecycleServiceTest | 5 | Forbidden transitions, cancellation stock release, pending-payment cancellation, tenant-aware inventory events, unpaid pickup guard and completed-return availability |
| InventoryCommandServiceTest | 6 | Normalized duplicate barcodes, generated serials, variant resolution, deletion restrictions for assigned/allocated inventory and deletion of unused inventory |
| TenantAccessServiceTest | 4 | Exact tenant/user membership, shared public context, provisioning readiness and unknown-store resolution |

From the `backend` directory:

```sh
./gradlew test --tests '*RentalPriceCalculatorTest' --tests '*LateFeeCalculatorTest' --tests '*MerchantOrderPaymentServiceTest' --tests '*MerchantOrderLifecycleServiceTest' --tests '*InventoryCommandServiceTest' --tests '*TenantAccessServiceTest'
```

Gradle still compiles all test sources before applying `--tests` filters. Compilation failures in another test class must be fixed before these tests can run. The normal HTML report is `backend/build/reports/tests/test/index.html`.

The implementation agent did not run Gradle or the tests, following the user's instruction. Only source review and `git diff --check` were performed. These tests do not verify actual database locks/concurrent overbooking, transaction rollback against PostgreSQL, HTTP authorization filters or migration compatibility; those need separate integration checks.
