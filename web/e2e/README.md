# Sewain frontend E2E

Standalone Playwright test files. Application sources, frontend package.json/lockfile, Docker config and backend code were not changed. Dependencies are isolated in this directory. Nothing was installed or executed by the implementation agent; only JS syntax and whitespace checks were performed.

Tests use the existing running application/backend (Docker or local). There are no mocked API responses or browser storage shortcuts. Authentication happens through the real login form in a fresh browser context per test. There is no `webServer`, automatic build, Docker startup, database reset, account registration, actual CSV submission, phone pairing/disconnection or WhatsApp send. The separate, opt-in write suite creates products, collections and orders as documented below.

## Setup (run yourself)

From repository root:

```sh
cd web/e2e
npm install
npx playwright install chromium
```

Existing web/backend/database must be running. Choose an existing dedicated test account whose selected/first merchant is READY. Merchant switching is not implemented in the current navbar, so the tests verify its displayed name to avoid silently testing the wrong store. Set URL to a tenant host if needed for the intended store. Credentials and the merchant name are required for authenticated tests.

```sh
export E2E_BASE_URL='http://sewain.app.localhost:3000'
export E2E_EMAIL='YOUR_TEST_ACCOUNT_EMAIL'
export E2E_PASSWORD='YOUR_TEST_ACCOUNT_PASSWORD'
export E2E_MERCHANT_NAME='YOUR_TEST_MERCHANT_NAME'
export E2E_REQUIRE_AUTH=true
npm test
```

`E2E_REQUIRE_AUTH=true` makes missing credentials/name a configuration error. Without it, public tests run and authenticated tests are visibly skipped when those variables are absent. The application's login form requires a password satisfying its current complexity rule.

For public checks only:

```sh
npx playwright test specs/public.spec.mjs
```

For one target:

```sh
npm test -- --project=desktop
npm test -- --project=mobile
npm test -- --project=tablet
```

For a visible browser:

```sh
npm run test:headed -- --project=desktop
```

## Scope

- Public: login UI/validation, anonymous redirects for protected merchant routes, registration link without submitting account creation.
- Desktop with auth: login, correct merchant selection, 15 navigation routes and page titles, import template content/download, invalid-file client validation without upload, WhatsApp disconnect-button visibility against real API state, empty-draft send guard, logout cookie cleanup and protected redirect afterward.
- Mobile 375 px and tablet 768 px with auth: drawer toggle, navigation closes drawer, reload preserves accessible header, import/file-input viewport fit.
- Every test fails on uncaught frontend `pageerror`. No document-wide horizontal overflow is allowed on checked pages; tables may scroll internally.

These are real frontend/backend E2E smoke checks, not full transactional coverage. The default smoke checks do not prove successful multi-variant CSV import, checkout/payment/refund, provisioning, live STOMP reconnect, actual WhatsApp pairing/send/inbound or cross-tenant data isolation. Product, collection and merchant order creation are covered by the opt-in write suite below. Those need dedicated fixtures and an explicitly writable test environment. WhatsApp status should remain stable while its visibility test runs; do not scan/unlink the phone simultaneously. Import validation expects no existing active import on the test merchant.

One worker and zero retries keep the run sequential. Playwright's auto-waiting assertions are used instead of fixed sleeps. Existing CSS/ARIA selectors are used where the application lacks associated input labels; no data-testid attributes were added to app code.

## Results

Default artifacts are outside the checkout:

- HTML report: OS temp directory `/sewain-e2e-report` (e.g. `/tmp/sewain-e2e-report` or macOS `$TMPDIR/sewain-e2e-report`).
- Failure screenshots/test output: OS temp directory `/sewain-e2e-results` (override with `E2E_OUTPUT_DIR`).
- Traces, videos and persistent authentication files are disabled.

Tests are not yet run or confirmed passing. A failed test should be inspected as a finding; this change does not modify app code to make tests pass.

Reference: https://playwright.dev/docs/test-configuration

## Opt-in write workflow: product → collection → order

`specs/workflow.spec.mjs` runs in the `write-desktop` project. It contains three serial tests using the real UI and real backend:

1. Create an ACTIVE single-variant product with a unique SKU, one physical inventory unit, zero deposit, and a one-day rental rate of 100000. Reopen its edit page to verify persisted name/SKU.
2. Create a MANUAL collection containing that product through the variant picker. Reopen the collection and check that the product remains selected.
3. Create a three-day pickup order for that product, using the tenant's existing payment options. Verify the selected product/variant/quantity, rental subtotal of 300000, and persisted detail data after navigation from the order list.

Enable only against a dedicated writable test merchant. The earlier login fixture checks `E2E_MERCHANT_NAME` before using the merchant UI. These tests do not change payment/rental settings: configure an allowed manual payment method, initial payment choice, and a rental window permitting a three-day rental starting 30 days ahead. Override the lead time with `E2E_RENTAL_LEAD_DAYS` (integer 1–365) when needed. A product/collection/order created by an earlier step is required by later steps; if creation fails, later steps are skipped by serial execution.

From `web/e2e`, with the normal account environment variables already exported:

```sh
export E2E_REQUIRE_AUTH=true
export E2E_ALLOW_WRITES=true
unset PWDEBUG
npm run test:headed -- --project=write-desktop
```

Headless:

```sh
npm test -- --project=write-desktop
```

Disable write tests afterward:

```sh
unset E2E_ALLOW_WRITES
```

With the flag absent, these tests are skipped. Enabling the flag requires all account environment variables, otherwise configuration fails immediately.

Created names/SKUs use an `E2E` prefix and a unique suffix. IDs and names are attached to the report after each successful creation. Data is deliberately retained for inspection, including partial results if a later step fails. No automatic deletion or payment/return/refund processing is performed. Use a disposable test tenant and manage its data explicitly. Do not run this suite against an operational merchant.

No API response is mocked, and no API request creates fixtures behind the UI. HTTP responses are observed to assert saved IDs and payloads. The new tests have not been executed; syntax, translation-key availability, and whitespace were checked only.
