# Storefront blocks

Add Block groups the registry into Basic, Collection, Decorative, Footer, Form, Layout, Links and Product. Search and scroll work within the currently compatible section/parent. All existing saved block identifiers remain supported; the catalog includes both the requested blocks and existing rental/checkout controls.

New blocks include Icon, Page, Collection Card/Title, Jumbo Text, Comparison Slider, Copyright, Payment Icons, Policy Link, Contact Form, Email Signup, Accordion, Group, Spacer, Popup Link, Buy Buttons, Description, Recommended Products and SKU. Existing Price and Variant Picker implementations are reused.

- Group and Accordion contain editable child blocks. Popup Link shows editable content inline in the builder so the inspector remains usable, and opens a dialog on the live storefront.
- Page selects a custom page and embeds its content without Navigation/Footer. Reference cycles and excessive nesting are blocked; embedded content is edited in its original page.
- Collection Card has a selectable source outside collection lists. Inside a list it receives each collection as context; Image and Collection Title children follow that context. New grid/bento/carousel/editorial presets use the card template. Existing saved lists retain their previous rendering until a card template is added.
- Comparison Slider reuses the accessible two-image comparison renderer. Form blocks reuse the Customer-linked form backend and consent handling.
- Copyright replaces `{year}` and `{store}`, escaping the dynamic store name. Payment Icons are manually configured visual indicators of accepted methods, not payment processor integration.
- Policy Link uses the existing destination picker and saved references. Product buying controls require an appropriate product context; embedded products link to rental options rather than using another product's selected variant or dates. Preview cannot add items to cart.
- Recommended Products uses available active products, prioritizes the current product type and excludes the current product. SKU uses the selected variant when available.

The backend JSON allowlist was extended with the new identifiers. This change adds no database migration; form blocks continue to require the previously introduced V32/V33 Customer/contact migrations.
