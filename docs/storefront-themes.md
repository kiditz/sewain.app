# Storefront theme library

The existing JSON component registry and PageBuilder render installed themes. No executable theme code is uploaded.

## Storage and migration

Tenant migration `V31__storefront_theme_library.sql` preserves all previous split configuration rows and creates:

- `THEME_LIBRARY`: the active installed-theme ID. Mutations lock this row to serialize publish, edit, duplicate and delete.
- `THM:<id>`: installed-theme metadata, draft, published snapshot and the latest ten published revisions.
- `CONTENT_PAGE:<id>`: shared custom-page draft content, independent of installed themes.

The existing storefront becomes `original`, with its published pages captured immediately. Run the normal tenant Flyway migration process when deploying the backend; frontend and backend changes should be deployed together.

Navigation and Footer are global within an installed theme. Custom-page content is shared by the store and resolved into the selected theme's draft. A theme published after a new custom page was created therefore includes that page when its status is ready. Products, collections, media and menus remain store resources.

Public storefront reads use only the active theme's complete published snapshot, plus current store menus. Saving settings/pages does not publish. Page status controls inclusion in the next theme publication. Publishing creates an immutable snapshot and changes the active ID in one transaction. Rollback republishes a prior snapshot as a new revision and leaves the editing draft intact.

## Merchant experience

`/merchant/storefront` contains the active theme, installed theme library and Everyday/Studio/Sunrise catalog. Theme cards use illustrative thumbnails. Catalog entries change the home layout, typography, palette and corners while retaining store content references.

Editor, theme settings and authenticated preview links carry `themeId`. Nuxt state and autosave writers are scoped to the selected theme. Default editor requests receive a resolved theme ID from the backend and pin subsequent writes to that ID. The library flushes pending store autosaves before mutations, including publish.

`/merchant/storefront/preview?themeId=<id>` previews draft pages. It uses the existing preview renderer, so checkout/cart actions are disabled and product-specific pages may need a product context to show real product details. Preview links are authenticated and are not public share links.

## API

All theme-management endpoints require the existing merchant authentication and tenant context.

- `GET /api/storefront/themes`: metadata and revision summaries.
- `POST /api/storefront/themes`: `{ name, source, configuration: { theme, pages } }` installs a draft.
- `GET /api/storefront/themes/{id}`: effective draft.
- `PATCH /api/storefront/themes/{id}`: `{ name }`.
- `DELETE /api/storefront/themes/{id}`: removes a non-active theme.
- `POST /api/storefront/themes/{id}/duplicate`.
- `POST /api/storefront/themes/{id}/publish`.
- `POST /api/storefront/themes/{id}/rollback/{revision}`.

Existing page/theme endpoints accept `?themeId=...`; omission addresses the active theme's draft. Their responses include the resolved `themeId`. `/api/storefront/published` remains the public published document.

## Deliberately separate follow-up features

This is the built-in theme library, not a third-party marketplace. ZIP asset packaging, developer accounts, paid licensing, schema upgrades, merging vendor updates and multiple alternative templates per resource are not implemented. JSON import/export includes design configuration and existing asset URLs; it does not copy image/font binaries or export store-owned custom-page content. Metadata currently uses schema version 1 and package version 1.0.0. Installed media references remain store media URLs.
