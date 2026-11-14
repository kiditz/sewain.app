# Medium article screenshots

Six screenshots captured from the local Eiger Rent store on 2026-11-14.
Use the sequence below for an application walkthrough. Merchant account headers
are excluded from the administrative captures; customer details are fictional.

| Order | Image | Suggested English caption |
| --- | --- | --- |
| 1 | [Dashboard](02-dashboard.jpg) | Review rental operations and see which orders need attention. The new demo order is waiting for payment. |
| 2 | [Product catalog](03-product-catalog.jpg) | Find rental products by name and organize them by product type and tags. This view filters the demo catalog to carriers. |
| 3 | [Rental packages](04-rental-pricing.jpg) | Configure daily and multi-day packages independently of physical inventory: IDR 35,000 for one day, IDR 87,500 for three days and IDR 175,000 for seven days. |
| 4 | [Create an order](06-order-creation.jpg) | Select the rental dates, product and quantity, then choose pickup or delivery. This example books one carrier for December 21–24. |
| 5 | [Order detail](07-order-detail.jpg) | The saved order preserves its rental window and price breakdown, including the selected three-day package and refundable deposit. |
| 6 | [Storefront preview](10-storefront-preview.jpg) | Preview the draft storefront with the seeded product illustrations before publishing changes. |

## Demo transaction

- Order: `SW-130397C5`.
- Customer: Demo Camper (`camper@example.invalid`, a fictional address).
- Product: Carrier Hiking 40 Liter, Standar, SKU `DUMMY-OUT-006`.
- Quantity: one unit.
- Pickup: December 21, 2026 at 09:00 in the browser's displayed local time.
- Return: December 24, 2026 at 00:00 in the browser's displayed local time.
- Rental fee: IDR 87,500 for a three-day package.
- Refundable deposit: IDR 70,000.
- Total: IDR 157,500.
- Initial rental down payment: IDR 26,250, pending bank transfer.
- Remaining rental balance: due at pickup. No payment was recorded as received.
- Status: pending payment, not an active rental or a completed payment.

The initial Sunday date was rejected by the store's operating-day rule. The
saved example uses weekdays. The order was created through the real application
UI; it is not a mocked screen or an inserted order record.

## Capture context

The earlier seven E2E products and three associated dummy orders were deleted
before this order was created. The Python seed added 50 products, 50 variants,
150 rental plans and 250 inventory units. The original non-E2E tent was retained,
so the store contains 51 products. Illustrations are stored in the persistent
Docker backend-upload volume. These are demo illustrations, not product photos.

The catalog, package configuration and order screenshots use the same seeded
carrier. The storefront demonstrates the broader catalog and remains a draft
preview with a placeholder logo; it does not demonstrate publication or customer
checkout. The dashboard's displayed available-unit figure is not used as a
verified total of all seeded inventory in the article.

The creation image shows the form immediately before submission; the detail
image shows the persisted result. No theme was published and no external message
or payment was sent. Nothing has been published to Medium.

## Suggested article structure

Suggested title: **From Catalog to Booking: A Walkthrough of Sewain**.

Open with the daily workflow of a rental merchant: maintaining a catalog,
tracking physical stock and understanding what each booking costs. Introduce
Sewain as a portfolio application connecting those activities in one workspace.
Use the dashboard image to establish the merchant's starting point.

Next, use the catalog and rental-package images to explain the distinction
between a product, its variant, its rental packages and its physical units. The
carrier example has three units and three duration-based packages. The three-day
rate is a package price rather than three times the daily rate.

Then follow the order form and saved detail. Explain that the merchant selects a
customer, dates, quantity and pickup method, while the result keeps the rental
fee separate from the refundable deposit. Use the actual figures above, and make
clear that creating an order does not mean payment has been received.

Close with the storefront preview as the customer-facing side of the application.
Describe it as a draft preview, and reserve payment collection, pickup/return and
integration workflows for a later article rather than implying they occurred in
this demonstration.
