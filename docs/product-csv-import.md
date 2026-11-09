# Product CSV import

Merchant: Products → Import CSV (`/merchant/products/import`). Download the template there.

Required columns: `name,slug,sku,rental_fee`. Optional: `stock,buy_price,deposit_amount,status,description,image_urls`.

- UTF-8 CSV, comma delimiter, RFC4180 quoting (quote commas/newlines and escape quotes as `""`). UTF-8 BOM accepted. At most 5 MB and 1000 data records.
- One row creates one product, one Standard variant, one DAY/1 rental plan, and `stock` inventory units (0–1000). Generated barcodes use `sku-1`, `sku-2`, etc.; use a unique SKU per product. Categories, tags and multiple variants are not part of this first format.
- Default status DRAFT, stock 0, buy price and deposit 0. Status accepts DRAFT, ACTIVE, UNLISTED. Prices use a dot for decimals, no currency or thousands separators, max 9999999999.99.
- `image_urls`: zero to five direct public HTTP/HTTPS image URLs separated by `|`. First becomes the product cover; all become variant images and entries in Media. Existing raster/SVG upload validation applies. Download max 5 MB each, public DNS/IP only, standard ports, no redirects/proxy/cookies, bounded request time. If a CDN redirects, provide its final URL.
- Import is additive. Existing slug is SKIPPED, never updated. Failed rows can be corrected and uploaded again. Product + media DB writes commit per record; downloaded media files are removed on rollback, including checked download failures.
- One active import per tenant; global worker concurrency 1 with bounded queue 10 to limit laptop load. Each user sees their own latest report. Up to 100 row errors are shown. Record numbers count the header as 1; multiline quoted cells do not increase record numbering.

## Backend and storage

POST multipart `file` to `/api/products/imports` returns 202 with job status. GET `/api/products/imports/latest` or `/{id}` recovers the report. Authenticated tenant access is required; job ownership and tenant membership are checked on snapshots. Spring Batch JDBC metadata and reports live in public schema via Flyway V11. Products/media use the tenant context explicitly set and cleared by the worker.

Progress uses the existing `/ws/provisioning` endpoint with authenticated STOMP CONNECT and `/user/queue/product-imports`. On refresh/reconnection the frontend loads a snapshot once; no polling. The import continues when the page closes. A backend restart marks interrupted queued/running reports FAILED; re-upload safely skips committed slugs. This version does not auto-resume jobs after backend restart. In-flight Batch metadata is retained for diagnostics; report state is the UI source. The worker/recovery model supports a single backend instance (current Compose deployment), not multiple replicas.

Images use `APP_MEDIA_STORAGE_PATH=/app/upload`. Root Compose already mounts `backend-upload:/app/upload`; it survives container recreation. **Do not run `docker compose down -v`** if you want to retain media/database volumes. No external image URL is retained on products after a successful import.

## Validation (run manually)

No builds, tests, migrations or containers were run for this change.

```sh
cd backend
./gradlew test --tests app.sewain.catalogs.ProductImportTest --tests app.sewain.media.RemoteImageImporterTest
./gradlew compileJava
```

Build/deploy both changed services when ready:

```sh
docker compose build backend web
docker compose up -d --no-deps backend web
```

Manual checks:

1. Import template without images; check DRAFT product, daily rental fee and stock.
2. Import valid public JPG/PNG/SVG URLs; verify files appear in Media and survive backend recreate (without removing volumes).
3. Retry same file: existing slugs are skipped, no duplicate media/downloads.
4. Mix valid/invalid records and unreachable/oversized/private/redirect image URLs. Valid products should commit, failed rows should report errors with no partial product/media.
5. Refresh during import, close/reopen page, temporarily disconnect WebSocket: persisted counts recover without polling.
6. Restart backend during import: interrupted report becomes FAILED; re-upload skips committed products.
7. Try a different tenant/user's import UUID via REST and STOMP: deny access; second simultaneous tenant import returns 409.

Implementation follows Spring Batch 6 JDBC infrastructure and job builder APIs:
https://docs.spring.io/spring-batch/reference/api/org/springframework/batch/core/configuration/annotation/EnableJdbcJobRepository.html
