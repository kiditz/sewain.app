# Security review remediation — November 6, 2026

All seven main findings in the [initial review](/Users/rifkyaditya/Playground/sewain/docs/security/review-2026-11-06.md) received patches and focused regression coverage. No database migrations or store data changes were executed as part of this work. This does not claim that all application/infrastructure risks have been eliminated.

| Finding | Change | Regression evidence |
|---|---|---|
| SEC-01 | Merchant updates check the target owner before mapping and no longer replace `userId` | MerchantOwnershipTest |
| SEC-02 | Removed the HTTP migration controller and denied the old URL in the security chain; migrations require an opt-in maintenance runner | ApiSecurityBoundaryTest |
| SEC-03 | All cart operations reject caller-asserted customer IDs; GET also validates ownership; guest tokens remain supported | CartOwnershipTest |
| SEC-04 | Separate public catalog queries/DTOs enforce ACTIVE status; purchase costs, serials, barcodes, physical units, and inventory notes are not serialized | PublicCatalogSecurityTest + ApiSecurityBoundaryTest |
| SEC-05 | Parser-based rich-text sanitization covers writes/imports, legacy reads, and Vue SSR/browser rendering | ContentSecurityTest + ThemeLibraryServiceTest + storefront-content-security.test.mjs |
| SEC-06 | Uploads are decoded and re-encoded as raster images; filenames/extensions/MIME are server-controlled, dimensions are bounded, and legacy active files are blocked | SafeRasterUploadTest + MediaFileReplacementTest |
| SEC-07 | Block navigation URLs use a shared protocol resolver, including Button/Logo/Policy and imported/legacy settings | ContentSecurityTest + storefront-content-security.test.mjs |

## API contract changes

Merchant endpoints `/api/products/**`, `/api/collections/**`, and `/api/merchants` require authentication. Storefront clients use GET:

- `/api/storefront/products`, `/api/storefront/products/{id}`, `/api/storefront/products/slug/{slug}`;
- `/api/storefront/products/types`;
- `/api/storefront/collections`, `/api/storefront/collections/{slug}`, `/api/storefront/collections/{id}/products`.

DRAFT and UNLISTED products are not exposed through the public catalog, including ID/slug details. Public variants provide an aggregate `inventoryQuantity`; `inventoryUnits` and `buyPrice` are omitted. The frontend adapter preserves shared UI types without fetching internal data again.

The public cart continues to use `X-Guest-Token`. `X-Customer-Id` is rejected until customer identity comes from a verified session/token. Future customer login must use a server-side owner resolver rather than reopening the caller-supplied header.

**Deploy the backend and frontend together**, because older storefront bundles still call the previous catalog URLs. Reload the storefront after deployment.

## Maintenance operations

`POST /api/merchants/migrate` must no longer be used, including by merchant owners.

The migration runner is registered only when trusted deployment configuration sets `app.maintenance.migrate-tenants=true`, for example through `APP_MAINTENANCE_MIGRATE_TENANTS=true`. Run it as a maintenance instance that receives no traffic, using an appropriate database role, then disable the flag. Do not enable it during routine application startup. This flag was not enabled during testing.

## Upload formats and compatibility

- Raster input formats: successfully decoded PNG, JPEG, GIF, and WebP.
- Upload output: JPEG for JPEG input, PNG for other formats; metadata is removed and animated GIFs become their first frame.
- Limits: 5 MB input/file, at most 8192 pixels per side and 16 million pixels, 20 MB output/file, and 20 MB combined output per batch. The 20 MB multipart request limit still applies.
- Active SVG/HTML is not accepted. Legacy uploads with active extensions are not served publicly; legacy SVG images need raster replacements. Static application SVGs under `web/public` are unaffected.
- Background removal preserves the existing file ID and URL. The resolver detects PNG output even when the legacy URL ends in JPEG/SVG/HTML, serving `image/png` rather than an active document.
- Legacy HTML is sanitized on read without requiring a bulk database rewrite. Plain-text fields such as page titles are not converted to HTML entities.

## Validation

- Focused backend regressions cover actual security filters through MockMvc, ownership checks, public projections, sanitization, imported/legacy themes, raster validation, and media replacement/rollback.
- Three frontend tests check HTML/URL payloads, valid Quill formatting, and SSR of the actual BlockLabel component.
- Vue and TypeScript compilation passed for affected files.
- Production client, SSR, and Nitro builds passed with Node 24.22.1, including after the Nuxt 4.6.4 update.
- No browser testing, penetration testing against real stores, tenant migrations, or order creation was performed.

## Dependencies: risk reduction and remaining work

Sanitizer installation was followed by a registry audit. Compatible updates were applied without `--force` and without executing installation scripts. Nuxt was raised to baseline `^4.6.4`, with its lockfile updated. Background-removal model/library dependencies were not upgraded across major versions.

Results of `npm audit` before/after this work:

| Package severity | Before | After |
|---|---:|---:|
| Critical | 3 | 0 |
| High | 18 | 5 |
| Moderate | 19 | 12 |
| Low | 3 | 6 |
| Total | 43 | 23 |

These counts are npm audit package classifications, not demonstrated exploits or unique CVEs. Remaining high findings involve the `@huggingface/transformers` → `sharp` chain, `zod`/`@vee-validate/zod`, and some transitive `postcss` versions. Some require major migrations or upstream fixes; incompatible versions were not forced. The sanitizer disallows style attributes and disables style-attribute parsing.

Next steps: migrate validation/model dependencies with dedicated regressions; confirm advisory reachability in the runtime/deployment; complete the merchant role matrix, fail-closed tenant handling, rate limiting, and session/CSP/production hardening identified in the initial review. An audit showing Critical=0 is not a security certification.

## Parser policy references

- [jsoup: parser and safelist sanitization](https://jsoup.org/cookbook/cleaning-html/safelist-sanitizer).
- [sanitize-html: tag, attribute, and protocol configuration](https://github.com/apostrophecms/sanitize-html).
- [TwelveMonkeys ImageIO 3.15.3](https://github.com/haraldk/TwelveMonkeys/releases/tag/twelvemonkeys-3.15.3), used to decode WebP before re-encoding.
