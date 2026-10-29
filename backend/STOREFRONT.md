# Storefront storage

Each tenant keeps its storefront resources in `storefront_configurations` as
separate records. Sections and blocks remain JSON inside their owning page.

| `config_key` | Contents |
| --- | --- |
| `PAGE:<page-id>` | One page, including metadata, status, sections and blocks |
| `THEME` | Colors, typography, layout and theme preset |
| `MENU:<menu-id>` | Menu name and ordered items: `{ id, pageId, label? }` or `{ id, target: { type, id, title, slug }, label? }` |
| `NAVIGATION` | `{ headerMenuId }`, which may be null |

Menu labels default to page or catalog titles and can be overridden per item. The grouped link picker offers Home, Products, Collections, custom Pages, and Orders (checkout). Product results load on demand in pages of 20; collections load on entering their group. Product/collection targets store their catalog ID and the title/slug at selection time (`type` is `PRODUCT` or `COLLECTION`); select the destination again after changing its catalog slug. URLs are derived internally and are not displayed in the editor. Deleting a custom page removes its
menu references. Deleting a selected menu clears `headerMenuId`. Built-in pages
cannot be deleted.

## API

All write endpoints and editor reads require authentication under the existing
security configuration. Each PUT accepts `{ "configuration": ... }`.

- `GET /api/storefront/pages`; `PUT` or `DELETE /api/storefront/pages/{id}`
- `GET` or `PUT /api/storefront/theme`
- `GET /api/storefront/menus`; `PUT` or `DELETE /api/storefront/menus/{id}`
- `GET` or `PUT /api/storefront/navigation`

`GET /api/storefront/published` remains a public aggregate read for rendering and
includes only published pages. `GET /api/storefront` remains an authenticated
aggregate read. The old whole-document `PUT /api/storefront` is disabled.
Branding remains owned by merchant settings and `/api/storefront/branding`.

The `NAVIGATION` section contains independently visible `LOGO`, `HEADING`, and
`MENU` blocks. Blank logo/heading values use the merchant logo/name. Each menu
block selects its own menu or inherits `headerMenuId`; hiding it also removes its
links from the mobile drawer. Cart and account icons are fixed section actions,
outside the editable blocks. The cart badge appears only for a nonempty cart;
the account action is disabled until email login is implemented.

The editor upgrades legacy `HEADER` sections to `NAVIGATION` on load and saves
changed pages, preserving section IDs, visibility, spacing and selected menu.
Public rendering also normalizes old sections without writing data. Existing
`NAVIGATION` sections retain intentionally removed or hidden blocks on reload.

## Migration

Deploy backend and frontend together. Run tenant Flyway migrations including
`V29__split_storefront_resources.sql` before opening the updated editors. For
existing tenants, the project's migration entry point is
`POST /api/merchants/migrate`; restarting alone does not migrate tenant schemas.

V29 preserves page/menu IDs and item order, migrates old URL-based menu references,
and retains the legacy `STOREFRONT` row as a pre-migration backup. Runtime code no
longer updates that row. A tenant with unmigrated legacy data rejects edits rather
than replacing its content with defaults.

Frontend persistence is split between `useStorefrontPages`, `useStorefrontTheme`,
`useStorefrontMenus`, and `useStorefrontCatalog`. `useStorefront` is a compatibility
facade. Save queues are scoped per resource and Nuxt app; editor state is scoped
per merchant.

## Verification

- Frontend: `npm run test:storefront` and `npm run build` in `web/`.
- Backend: `./gradlew test --tests app.sewain.storefront.StorefrontServiceTest`.

Cart sections and their items, summary, and checkout blocks store independent
`layout` and `padding` settings. Legacy cart documents receive missing defaults
without replacing configured values. The section layout controls direction,
spacing and content width; summary position remains selectable. Container
queries stack the main/summary areas and simplify cart item cards in narrow
storefronts and builder previews.

Cart titles are grouped inside a `HEADER` block with `EYEBROW`, `HEADING`, and
`TEXT` children. Older top-level title blocks are moved into that group while
retaining their IDs, settings, order and individual visibility.

Checkout now stores a `HEADER` group plus `CHECKOUT_CONTACT`,
`CHECKOUT_FULFILLMENT`, `CHECKOUT_NOTES`, `CHECKOUT_PAYMENT`,
`CHECKOUT_SUMMARY`, and `CHECKOUT_SUBMIT` blocks. Section and block layout,
padding, labels, ordering and visibility are configured in the builder.
All checkout blocks share the same form state and validation through
`CheckoutFlow`; the builder preview uses sample cart/payment data and cannot
submit an order or load checkout APIs. Legacy sections gain default blocks once,
while preserving existing header content and visibility.
