# Storefront storage

Each tenant keeps its storefront resources in `storefront_configurations` as
separate records. Sections and blocks remain JSON inside their owning page.

| `config_key` | Contents |
| --- | --- |
| `PAGE:<page-id>` | One page, including metadata, status, sections and blocks |
| `THEME` | Colors, typography, layout and theme preset |
| `MENU:<menu-id>` | Menu name and ordered `{ id, pageId }` references |
| `NAVIGATION` | `{ headerMenuId }`, which may be null |

Menu labels and URLs are derived from pages. Deleting a custom page removes its
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
