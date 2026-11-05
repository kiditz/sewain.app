# Storefront section catalog

The Add Section dialog groups 45 section types into Banner, Collections, Forms, Layout, Products, Storytelling, Text and Footer, with localized search. Existing section identifiers remain valid. New variants store their own type and JSON defaults while reusing the existing renderer where appropriate.

`section-catalog.ts` defines grouping; `additional-sections.ts` defines the new presets. New editable blocks include Question, Icon with Text, Column, Divider, Marquee Text and Product Hotspot. Sections retain the shared page/full width and padding behavior. Image fields use the media library and products use the existing paginated product selector.

- Collection Bento, Spotlight and Text have distinct layouts, alongside Carousel, Editorial and Grid.
- Full-frame, Inset and Layered slideshow presets retain editable/reorderable Slide blocks. Preview pauses autoplay.
- Image Compare uses two Image blocks and a keyboard-accessible range slider.
- Product Hotspot uses an Image block and positioned Hotspot blocks. Each marker selects one product and opens its product card.
- Featured Product and Product Highlight select one product and link to its rental details, without changing the product context of other sections.
- Recommended Products uses active available products of the current product's type, excludes the current product and falls back to available products. Explicitly selected products override automatic suggestions. This does not implement a behavioral/personalized recommendation engine.
- FAQ answers are plain text. Marquee respects reduced-motion preferences and pauses in the editor.
- Contact and email-signup forms are disabled in preview. Successful requests create or reuse a tenant-scoped Customer by email. Contact messages appear in Customer details; email signup records consent on the Customer. `/merchant/storefront/submissions` redirects to the Customer list. They do not send email campaigns or forward messages externally.

## Backend deployment

Run tenant migrations **V32__storefront_form_submissions.sql** and **V33__storefront_customer_contacts.sql** using the existing merchant migration process. V32 creates form history; V33 links it to Customer, backfills existing form contacts without replacing existing customer profiles, and adds acquisition source and email consent fields. Deploy the frontend and updated backend schema allowlist together so new section/block identifiers can be saved.

The public `POST /api/storefront/submissions` validates kind, UUID request ID, name/email/message sizes and consent. The native insert ignores duplicate request IDs and duplicate email signups. A honeypot avoids storing obvious automated submissions. Signup consent is always required by the backend.

Authenticated `GET /api/storefront/submissions?customerId=<id>&page=0&size=10` returns bounded paginated Customer contact history; `DELETE /api/storefront/submissions/{id}` removes a submission. Commands and queries use separate services. Existing tenant routing and authentication scope these operations.

Customer resolution preserves existing names, notes, passwords and enabled status. New email-only contacts use the email as their display name until a merchant updates the profile. Only email-signup consent marks `emailSubscribed`; a contact-form message alone does not opt a Customer into marketing. Creating a Customer contact does not sign the visitor in or issue an account token.

### Bento and Editorial resource layouts

Collection Bento uses a twelve-track composition that adapts to the number of cards, including balanced final groups. Mobile uses two tracks with alternating paired and wide cards. Editorial collections and products each use an asymmetric eight-card cycle with intentional empty space; mobile uses alternating widths and alignment instead of shrinking the desktop grid. Both respond to their container width, including the builder preview.

`ResourceListLayout.vue` owns the shared geometry. Cards retain editable content, padding and styling; Bento/Editorial own image dimensions, and Editorial owns the card's vertical arrangement. The editor hides conflicting card direction/height and image aspect-ratio controls while these layouts are selected. Item spacing remains editable. Ordinary Grid and Carousel retain their column controls.

Design reference: [Shopify Horizon resource layouts](https://github.com/Shopify/horizon/tree/main/snippets), specifically the Bento grid and the separate Editorial collection/product compositions. These are built into the existing JSON section/block renderer and require no backend migration.
