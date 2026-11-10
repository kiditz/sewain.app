# Booking & Rental Business Lifecycle

## 1. Booking Lifecycle

A booking represents the entire rental process, from the customer placing a booking until all items are returned and the rental is complete.

```text
PENDING
   │
   ├── cancel ───────────────► CANCELLED
   │
   ▼
CONFIRMED
   │
   │ prepare rental
   ▼
READY
   │
   │ handover / delivery
   ▼
RENTED
   │
   │ customer returns items
   ▼
RETURNING
   │
   │ inspection completed
   ▼
COMPLETED
```

`DAMAGED` is not a `BookingStatus`. Damage is an outcome of the return inspection.

### Booking Status

```java
public enum BookingStatus {
    PENDING,
    CONFIRMED,
    READY,
    RENTED,
    RETURNING,
    COMPLETED,
    CANCELLED
}
```

Valid cancellation transitions:

```text
PENDING    ──────► CANCELLED
CONFIRMED  ──────► CANCELLED
```

---

## 2. Booking Rental Period

A booking stores the rental period:

```text
Booking #BK-2026-001

Customer:
Budi

Product:
Sony A7 IV

Rental plan:
3 Days

Start:
10 Oct 2026

Expected return:
13 Oct 2026
```

Initial `bookings` structure:

```text
bookings
--------
id
booking_number
customer_id
status
start_at
expected_return_at
returned_at
subtotal
deposit_amount
discount_amount
late_fee
damage_fee
total_amount
notes
created_at
updated_at
```

### Rental Plan Snapshot

A booking stores more than just `rental_plan_id`.

Rental plans may change after a booking is created, so the values used at booking time must be stored as a snapshot.

```text
rental_fee
deposit_amount
duration_value
duration_unit
```

Example:

```text
Rental plan at booking time:
3 Days = Rp120.000
```

If the merchant later changes the rental plan:

```text
3 Days = Rp150.000
```

the existing booking continues to use:

```text
3 Days = Rp120.000
```

---

## 3. Inventory Unit

A booking must reference an **inventory unit**, rather than only a product.

A product represents a general type of item:

```text
Sony A7 IV
```

An inventory unit represents a specific physical item:

```text
Sony A7 IV
├── CAM-001
├── CAM-002
└── CAM-003
```

Existing inventory structure:

```text
inventory_units
---------------
id
product_id
sku_barcode
condition_notes
```

### Booking Item

```text
booking_items
-------------
id
booking_id
product_id
inventory_unit_id
rental_plan_id

quantity
rental_fee
deposit_amount

start_at
expected_return_at
returned_at
```

Example:

```text
BK001
└── Sony A7 IV
    └── CAM-002
```

This allows the system to identify the specific physical inventory unit rented by the customer.

---

## 4. Availability

Availability must be calculated per **inventory unit**, rather than only per product.

Example:

```text
10 Oct - 13 Oct

CAM-001 → booked
CAM-002 → available
CAM-003 → booked
```

When the customer places a booking:

```text
available units
      │
      ▼
   CAM-002
```

After the booking is confirmed:

```text
CAM-002
   │
   ▼
RESERVED
```

When the item is handed over to the customer:

```text
CAM-002
   │
   ▼
RENTED
```

When the customer returns the item:

```text
CAM-002
   │
   ▼
INSPECTION
```

After inspection:

```text
INSPECTION
   │
   ├── good ──────► AVAILABLE
   │
   └── damaged ───► MAINTENANCE
```

---

## 5. Inventory Unit Lifecycle

An inventory unit has its own lifecycle, separate from the booking.

```java
public enum InventoryUnitStatus {
    AVAILABLE,
    RESERVED,
    RENTED,
    INSPECTION,
    MAINTENANCE,
    LOST,
    RETIRED
}
```

Normal lifecycle:

```text
AVAILABLE
   │
   ▼
RESERVED
   │
   ▼
RENTED
   │
   ▼
INSPECTION
   │
   ▼
AVAILABLE
```

If damage is found:

```text
INSPECTION
   │
   ▼
MAINTENANCE
```

`BookingStatus` and `InventoryUnitStatus` must remain separate because they represent different states.

---

## 6. Return Process

Returning an item does not immediately change the booking to `COMPLETED`.

Flow:

```text
RENTED
   │
   │ customer returns
   ▼
RETURNING
   │
   │ inspection
   ▼
COMPLETED
```

A return inspection can include:

```text
Condition:
GOOD

Accessories:
- Charger
- Battery
- Strap

Damage:
None

Late:
0 days
```

If damage is found:

```text
Damage fee:
Rp500.000
```

The booking can still become:

```text
COMPLETED
```

with:

```text
damage_fee = 500000
```

Damage is not a booking status.

---

## 7. Return Inspection

A return inspection should be a separate entity from `Booking`.

### Booking Return

```text
booking_returns
---------------
id
booking_id
returned_at
received_by
condition
notes
late_days
late_fee
damage_fee
created_at
```

### Booking Return Item

If a booking contains multiple inventory units, inspect each item separately.

```text
booking_return_items
--------------------
id
booking_return_id
booking_item_id
inventory_unit_id

condition
damage_notes
damage_fee
```

Example:

```text
Booking
├── Camera
├── Lens
└── Tripod
```

Inspection results:

```text
Camera  → GOOD
Lens    → DAMAGED
Tripod  → GOOD
```

This model records damage against the specific affected inventory unit.

---

## 8. Deposit

The deposit is separate from the rental fee.

Example:

```text
Rental fee       Rp300.000
Deposit          Rp1.000.000
---------------------------
Customer pays    Rp1.300.000
```

When the item is returned:

```text
Deposit           Rp1.000.000
Damage deduction  Rp300.000
---------------------------
Refund             Rp700.000
```

Financial transactions associated with a booking can include:

```text
Booking
├── Rental charge
├── Deposit charge
├── Late fee
├── Damage fee
└── Deposit refund
```

---

## 9. Late Return

Expected and actual return times must be stored separately.

Example:

```text
Expected:
13 Oct 15:00

Actual:
14 Oct 10:00
```

Data:

```text
expected_return_at = 13 Oct 15:00
returned_at        = 14 Oct 10:00
```

The late duration is calculated from these two timestamps.

The calculation can produce:

```text
late_days
late_fee
```

`expected_return_at` must not change when the customer returns an item late.

---

## 10. Domain Model

Initial domain relationship:

```text
Customer
   │
   ▼
Booking
   │
   ├── BookingItem
   │      │
   │      ├── Product
   │      ├── RentalPlan
   │      └── InventoryUnit
   │
   ├── BookingPayment
   │
   └── BookingReturn
          │
          └── BookingReturnItem
                 │
                 └── InventoryUnit
```

---

## 11. Complete Business Flow

```text
                    ┌───────────┐
                    │  PENDING  │
                    └─────┬─────┘
                          │ confirm
                          ▼
                    ┌───────────┐
                    │ CONFIRMED │
                    └─────┬─────┘
                          │ prepare
                          ▼
                    ┌───────────┐
                    │   READY   │
                    └─────┬─────┘
                          │ handover
                          ▼
                    ┌───────────┐
                    │  RENTED   │
                    └─────┬─────┘
                          │ return
                          ▼
                    ┌───────────┐
                    │ RETURNING │
                    └─────┬─────┘
                          │ inspect
                          ▼
                    ┌───────────┐
                    │ COMPLETED │
                    └───────────┘
```

Cancellation:

```text
PENDING
   │
   ▼
CANCELLED
```

```text
CONFIRMED
   │
   ▼
CANCELLED
```

---

## 12. Implementation Order

Implement the rental domain in the following order:

1. **Booking + BookingItem schema**
2. **Availability calculation**
3. **Booking creation**
4. **Inventory reservation**
5. **Booking confirmation**
6. **Handover → `RENTED`**
7. **Return → `RETURNING`**
8. **Return inspection**
9. **Late fee calculation**
10. **Damage fee calculation**
11. **Payment and deposit settlement**
12. **Complete booking**

### Critical Business Rule

The most important part of the rental system is **availability and inventory reservation**.

The system must prevent two bookings from reserving the same physical inventory unit for overlapping rental periods.

```text
Booking A
10 Oct ───────── 13 Oct
             │
             ▼
          CAM-002

Booking B
11 Oct ───────── 15 Oct
             │
             ▼
          CAM-002  ← NOT AVAILABLE
```

Availability must therefore be checked against existing reservations and rental periods before an inventory unit can be reserved.
