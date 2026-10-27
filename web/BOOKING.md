# Booking & Rental Business Lifecycle

## 1. Booking Lifecycle

Booking merepresentasikan seluruh proses penyewaan, mulai dari customer melakukan booking sampai seluruh barang dikembalikan dan proses rental selesai.

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

`DAMAGED` bukan merupakan `BookingStatus`. Kondisi rusak merupakan hasil dari proses return inspection.

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

Booking menyimpan periode rental:

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

Booking tidak hanya menyimpan `rental_plan_id`.

Rental plan dapat berubah setelah booking dibuat, sehingga nilai yang digunakan ketika booking dibuat harus disimpan sebagai snapshot.

```text
rental_fee
deposit_amount
duration_value
duration_unit
```

Contoh:

```text
Rental plan saat booking:
3 Days = Rp120.000
```

Jika merchant kemudian mengubah rental plan:

```text
3 Days = Rp150.000
```

booking lama tetap menggunakan:

```text
3 Days = Rp120.000
```

---

## 3. Inventory Unit

Booking harus mengacu pada **inventory unit**, bukan hanya product.

Product merupakan representasi barang secara umum:

```text
Sony A7 IV
```

Sedangkan inventory unit merepresentasikan barang fisik:

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

Contoh:

```text
BK001
└── Sony A7 IV
    └── CAM-002
```

Dengan demikian sistem dapat mengetahui inventory unit fisik mana yang sedang disewa oleh customer.

---

## 4. Availability

Availability harus dihitung berdasarkan **inventory unit**, bukan hanya product.

Contoh:

```text
10 Oct - 13 Oct

CAM-001 → booked
CAM-002 → available
CAM-003 → booked
```

Ketika customer melakukan booking:

```text
available units
      │
      ▼
   CAM-002
```

Setelah booking dikonfirmasi:

```text
CAM-002
   │
   ▼
RESERVED
```

Ketika barang diserahkan kepada customer:

```text
CAM-002
   │
   ▼
RENTED
```

Ketika customer mengembalikan barang:

```text
CAM-002
   │
   ▼
INSPECTION
```

Setelah inspection:

```text
INSPECTION
   │
   ├── good ──────► AVAILABLE
   │
   └── damaged ───► MAINTENANCE
```

---

## 5. Inventory Unit Lifecycle

Inventory unit memiliki lifecycle sendiri yang terpisah dari booking.

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

Jika ditemukan kerusakan:

```text
INSPECTION
   │
   ▼
MAINTENANCE
```

`BookingStatus` dan `InventoryUnitStatus` harus tetap dipisahkan karena keduanya merepresentasikan state yang berbeda.

---

## 6. Return Process

Barang yang dikembalikan tidak langsung membuat booking menjadi `COMPLETED`.

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

Return inspection dapat mencakup:

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

Jika ditemukan damage:

```text
Damage fee:
Rp500.000
```

Booking tetap dapat menjadi:

```text
COMPLETED
```

dengan:

```text
damage_fee = 500000
```

Damage bukan booking status.

---

## 7. Return Inspection

Return inspection sebaiknya merupakan entity terpisah dari `Booking`.

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

Jika satu booking memiliki beberapa inventory unit, inspection dilakukan per item.

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

Contoh:

```text
Booking
├── Camera
├── Lens
└── Tripod
```

Hasil inspection:

```text
Camera  → GOOD
Lens    → DAMAGED
Tripod  → GOOD
```

Dengan model ini, damage dapat dicatat secara spesifik pada inventory unit yang bermasalah.

---

## 8. Deposit

Deposit bukan bagian dari rental fee.

Contoh:

```text
Rental fee       Rp300.000
Deposit          Rp1.000.000
---------------------------
Customer pays    Rp1.300.000
```

Ketika barang dikembalikan:

```text
Deposit           Rp1.000.000
Damage deduction  Rp300.000
---------------------------
Refund             Rp700.000
```

Financial transactions yang terkait dengan booking dapat mencakup:

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

Expected return dan actual return harus disimpan secara terpisah.

Contoh:

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

Late duration dihitung berdasarkan kedua timestamp tersebut.

Hasil perhitungan dapat menghasilkan:

```text
late_days
late_fee
```

`expected_return_at` tidak boleh diubah ketika customer terlambat mengembalikan barang.

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
