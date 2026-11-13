# Sewain dashboard style layer

The active style entry point is `web/app/scss/style.scss`. It loads:

| Module | Responsibility |
| --- | --- |
| `sewain/_foundation.scss` | Vuetify component geometry, shared borders, inputs, cards, buttons, lists and feedback styles |
| `sewain/_layout.scss` | Dashboard content spacing, responsive constraints, drawer/profile/footer geometry and compatibility utilities used by existing pages |
| `sewain/_features.scss` | Existing Sewain theme editor, chat/emoji, drag handles and auth decoration styles |
| `page/_landing.scss` | Existing application landing page |

The template `components/` and `layout/` stylesheet trees, `_variables.scss`, `_override.scss` and unused dashboard demo stylesheet were removed. The unused customizer configuration/store was replaced with `useDashboardShellStore`, which owns only drawer visibility and collapsed state. App bar and navigation use this store; no template theme/font customizer remains. The existing Nuxt/Vuetify palette is retained.

## Appearance contract

The refactor preserves the current visual targets: primary color and neutral palette, 256 px desktop drawer/62 px rail, mobile/tablet temporary drawer, 4 px form radius, 12 px card-heading spacing, 16 px card-action inset, current font sizes, content padding and responsive table containment. It retains `dashboard-navigation`, ARIA menu state and existing feature selectors used by E2E tests. Application menus, authentication, settings, storefront renderer and business behavior are not replaced.

Existing markup utility names remain where pages still use them. Unused template color-swatches, customizer animations, horizontal-layout demos and sidebar-demo selectors are removed. The global theme-editor button rules use normal descendant selectors instead of Vue-only `:deep`, which is not valid in a global stylesheet.

## Provenance

This removes the old template implementation from the active stylesheet/store structure. It does not establish that all repository content is free of Mantis-derived work or make the retained visual design an original clean-room design. The historical [third-party attribution](../web/THIRD_PARTY_NOTICES.md) and [MIT notice](../web/licenses/MANTIS-MIT.txt) remain. Deleting a credit would not substitute for replacing inherited work.

## Verification

Completed for this refactor:

- Compiled the full Sass entry point to a temporary CSS file.
- Parsed and compiled the changed app-bar/navigation Vue scripts.
- Checked for stale references to removed stylesheet modules and the old customizer store/config.
- Checked whitespace with `git diff --check`.

The Docker web image was rebuilt and only the web container recreated on November 13, 2026. The public Playwright suite passed all 12 scenarios across desktop (1440 px) and mobile (375 px), including login/register horizontal-overflow checks and protected-route redirects. Backend/database containers were not rebuilt or recreated.

This does not prove pixel equivalence for authenticated pages. Dashboard, product/collection/order forms, settings and storefront editor still require authenticated visual comparisons and navigation/write workflows at desktop, tablet and mobile widths.
