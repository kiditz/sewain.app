# Storefront theme catalog

The ten built-in presets install an independent draft with templates for Home, Product, Collections, All products, Cart, and Checkout. They retain custom pages, page identifiers, and the store's navigation/footer configuration. Installing a preset does not publish it. Existing installed themes are not rewritten by catalog changes.

Preset copy is generated in the merchant UI's current English/Indonesian locale. Local original SVG illustrations in `web/public/themes/demo` provide editable banner/slide images. Product and collection cards remain connected to merchant resources; installing a preset creates no demo products or collections.

Catalog Preview opens `/merchant/storefront/catalog/:preset` in a separate iframe app. The route requires the merchant session and uses an injected in-memory catalog, demo branding and sample cart, without loading or saving storefront drafts. Page/device controls belong to the surrounding preview dialog; checkout submission and public form submission stay disabled in preview mode.

Catalog thumbnails are actual screenshots of rendered demo Home templates. They are selected by the installed theme's `source`, so renaming a preset-derived theme retains its thumbnail. Imported/original themes use the generic fallback. These screenshots describe the starting preset, not subsequent merchant edits.

## Refreshing screenshots

With the development app and backend running, sign in using the in-app browser. Open `/merchant/storefront/catalog/:preset?locale=id&capture=1` for each catalog ID. Capture mode renders a 1200px-wide desktop composition scaled to the browser viewport using CSS. Wait for `[data-theme-ready="true"]`, `.storefront`, and visible images to finish loading, then save a screenshot in `web/public/themes/previews/:preset.png`. Crop from the top to a 4:3 card; current assets are 768×576. Re-capture after changing a preset's layout, copy, or illustrations.

Visual checks covered all ten Home templates, the preview dialog's mobile mode, product details, and checkout. The focused theme catalog test covers template generation, independent copies, locale copy, assets, palettes, and preservation of custom pages.
