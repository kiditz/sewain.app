-- V12__create_order_tables.sql
-- Assumes products, product_variants and inventory_units exist
-- in the current merchant schema.

CREATE
EXTENSION IF NOT EXISTS btree_gist;


-- =========================================================
-- CUSTOMERS
-- Registered customers belonging to this merchant.
-- =========================================================

CREATE TABLE customers
(
    id            UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    full_name     VARCHAR(150) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    phone         VARCHAR(30),
    password_hash VARCHAR(255),
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uk_customers_email UNIQUE (email)
);


-- =========================================================
-- CARTS
-- Exactly one owner: registered customer OR guest token.
-- =========================================================

CREATE TABLE carts
(
    id          UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    customer_id UUID,
    guest_token UUID,
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    expires_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_carts_customer
        FOREIGN KEY (customer_id)
            REFERENCES customers (id)
            ON DELETE RESTRICT,

    CONSTRAINT chk_carts_owner
        CHECK (
            (customer_id IS NOT NULL AND guest_token IS NULL)
                OR
            (customer_id IS NULL AND guest_token IS NOT NULL)
            ),

    CONSTRAINT chk_carts_status
        CHECK (status IN ('ACTIVE', 'CHECKED_OUT', 'ABANDONED'))
);

CREATE UNIQUE INDEX uk_carts_customer_active
    ON carts (customer_id) WHERE customer_id IS NOT NULL
      AND status = 'ACTIVE';

CREATE UNIQUE INDEX uk_carts_guest_active
    ON carts (guest_token) WHERE guest_token IS NOT NULL
      AND status = 'ACTIVE';

CREATE INDEX idx_carts_customer
    ON carts (customer_id);


-- =========================================================
-- CART ITEMS
-- =========================================================

CREATE TABLE cart_items
(
    id              UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    cart_id         UUID        NOT NULL,
    product_id      UUID        NOT NULL,
    variant_id      UUID        NOT NULL,
    quantity        INTEGER     NOT NULL DEFAULT 1,
    rental_start_at TIMESTAMPTZ NOT NULL,
    rental_end_at   TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_cart_items_cart
        FOREIGN KEY (cart_id)
            REFERENCES carts (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_cart_items_product
        FOREIGN KEY (product_id)
            REFERENCES products (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_cart_items_variant
        FOREIGN KEY (variant_id)
            REFERENCES product_variants (id)
            ON DELETE RESTRICT,

    CONSTRAINT chk_cart_items_quantity
        CHECK (quantity > 0),

    CONSTRAINT chk_cart_items_rental_period
        CHECK (rental_end_at > rental_start_at)
);

CREATE INDEX idx_cart_items_cart ON cart_items (cart_id);
CREATE INDEX idx_cart_items_variant ON cart_items (variant_id);


-- =========================================================
-- ORDERS
-- customer_id NULL means guest checkout.
-- Customer details are immutable checkout snapshots.
-- =========================================================

CREATE TABLE orders
(
    id               UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    order_number     VARCHAR(50)    NOT NULL UNIQUE,
    customer_id      UUID,
    cart_id          UUID,

    status           VARCHAR(30)    NOT NULL DEFAULT 'PENDING_PAYMENT',

    customer_name    VARCHAR(150)   NOT NULL,
    customer_email   VARCHAR(255)   NOT NULL,
    customer_phone   VARCHAR(30),

    shipping_address JSONB,
    billing_address  JSONB,

    subtotal         NUMERIC(14, 2) NOT NULL DEFAULT 0,
    deposit_total    NUMERIC(14, 2) NOT NULL DEFAULT 0,
    discount_total   NUMERIC(14, 2) NOT NULL DEFAULT 0,
    tax_total        NUMERIC(14, 2) NOT NULL DEFAULT 0,
    delivery_fee     NUMERIC(14, 2) NOT NULL DEFAULT 0,
    total_amount     NUMERIC(14, 2) NOT NULL DEFAULT 0,
    currency         CHAR(3)        NOT NULL DEFAULT 'IDR',

    notes            TEXT,
    placed_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    cancelled_at     TIMESTAMPTZ,
    completed_at     TIMESTAMPTZ,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT fk_orders_customer
        FOREIGN KEY (customer_id)
            REFERENCES customers (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_orders_cart
        FOREIGN KEY (cart_id)
            REFERENCES carts (id)
            ON DELETE SET NULL,

    CONSTRAINT chk_orders_status
        CHECK (status IN (
                          'PENDING_PAYMENT',
                          'CONFIRMED',
                          'PROCESSING',
                          'READY',
                          'ACTIVE_RENTAL',
                          'COMPLETED',
                          'CANCELLED',
                          'EXPIRED'
            )),

    CONSTRAINT chk_orders_amounts
        CHECK (
            subtotal >= 0
                AND deposit_total >= 0
                AND discount_total >= 0
                AND tax_total >= 0
                AND delivery_fee >= 0
                AND total_amount >= 0
            )
);

CREATE INDEX idx_orders_customer_created
    ON orders (customer_id, created_at DESC);

CREATE INDEX idx_orders_status_created
    ON orders (status, created_at DESC);

CREATE INDEX idx_orders_guest_email
    ON orders (customer_email, created_at DESC) WHERE customer_id IS NULL;


-- =========================================================
-- ORDER ITEMS
-- =========================================================

CREATE TABLE order_items
(
    id                    UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    order_id              UUID           NOT NULL,
    product_id            UUID           NOT NULL,
    variant_id            UUID           NOT NULL,

    product_name          VARCHAR(255)   NOT NULL,
    variant_name          VARCHAR(255)   NOT NULL,
    sku                   VARCHAR(100)   NOT NULL,

    rental_start_at       TIMESTAMPTZ    NOT NULL,
    rental_end_at         TIMESTAMPTZ    NOT NULL,
    rental_duration_value INTEGER        NOT NULL,
    rental_duration_unit  VARCHAR(20)    NOT NULL,

    quantity              INTEGER        NOT NULL,
    rental_unit_price     NUMERIC(14, 2) NOT NULL,
    deposit_unit_amount   NUMERIC(14, 2) NOT NULL DEFAULT 0,
    discount_amount       NUMERIC(14, 2) NOT NULL DEFAULT 0,
    tax_amount            NUMERIC(14, 2) NOT NULL DEFAULT 0,
    line_total            NUMERIC(14, 2) NOT NULL,

    created_at            TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id)
            REFERENCES orders (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_order_items_product
        FOREIGN KEY (product_id)
            REFERENCES products (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_order_items_variant
        FOREIGN KEY (variant_id)
            REFERENCES product_variants (id)
            ON DELETE RESTRICT,

    CONSTRAINT chk_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT chk_order_items_rental_period
        CHECK (rental_end_at > rental_start_at),
    CONSTRAINT chk_order_items_duration
        CHECK (rental_duration_value > 0),
    CONSTRAINT chk_order_items_duration_unit
        CHECK (rental_duration_unit IN ('HOUR', 'DAY', 'WEEK', 'MONTH')),
    CONSTRAINT chk_order_items_amounts
        CHECK (
            rental_unit_price >= 0
                AND deposit_unit_amount >= 0
                AND discount_amount >= 0
                AND tax_amount >= 0
                AND line_total >= 0
            )
);

CREATE INDEX idx_order_items_order ON order_items (order_id);
CREATE INDEX idx_order_items_variant ON order_items (variant_id);


-- =========================================================
-- PAYMENTS
-- =========================================================

CREATE TABLE payments
(
    id                  UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    order_id            UUID           NOT NULL,

    payment_type        VARCHAR(20)    NOT NULL DEFAULT 'ORDER',
    provider            VARCHAR(50)    NOT NULL,
    provider_payment_id VARCHAR(255),
    idempotency_key     VARCHAR(100),

    amount              NUMERIC(14, 2) NOT NULL,
    currency            CHAR(3)        NOT NULL DEFAULT 'IDR',
    status              VARCHAR(30)    NOT NULL DEFAULT 'PENDING',

    payment_url         TEXT,
    expires_at          TIMESTAMPTZ,
    paid_at             TIMESTAMPTZ,
    failed_at           TIMESTAMPTZ,

    provider_response   JSONB,
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT fk_payments_order
        FOREIGN KEY (order_id)
            REFERENCES orders (id)
            ON DELETE RESTRICT,

    CONSTRAINT chk_payments_type
        CHECK (payment_type IN ('ORDER', 'DEPOSIT', 'RENTAL')),

    CONSTRAINT chk_payments_status
        CHECK (status IN (
                          'PENDING', 'SUCCEEDED', 'FAILED', 'CANCELLED',
                          'EXPIRED', 'PARTIALLY_REFUNDED', 'REFUNDED'
            )),

    CONSTRAINT chk_payments_amount CHECK (amount > 0)
);

CREATE INDEX idx_payments_order ON payments (order_id);

CREATE UNIQUE INDEX uk_payments_provider_payment
    ON payments (provider, provider_payment_id) WHERE provider_payment_id IS NOT NULL;

CREATE UNIQUE INDEX uk_payments_idempotency
    ON payments (idempotency_key) WHERE idempotency_key IS NOT NULL;


-- =========================================================
-- PAYMENT REFUNDS
-- =========================================================

CREATE TABLE payment_refunds
(
    id                 UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    payment_id         UUID           NOT NULL,
    provider_refund_id VARCHAR(255),
    amount             NUMERIC(14, 2) NOT NULL,
    status             VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    reason             TEXT,
    processed_at       TIMESTAMPTZ,
    created_at         TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT fk_payment_refunds_payment
        FOREIGN KEY (payment_id)
            REFERENCES payments (id)
            ON DELETE RESTRICT,

    CONSTRAINT chk_payment_refunds_amount CHECK (amount > 0),

    CONSTRAINT chk_payment_refunds_status
        CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED', 'CANCELLED'))
);

CREATE INDEX idx_payment_refunds_payment
    ON payment_refunds (payment_id);

CREATE UNIQUE INDEX uk_payment_refunds_provider
    ON payment_refunds (provider_refund_id) WHERE provider_refund_id IS NOT NULL;


-- =========================================================
-- RENTAL ALLOCATIONS
-- =========================================================

CREATE TABLE rental_allocations
(
    id                UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    inventory_unit_id UUID        NOT NULL,
    order_item_id     UUID        NOT NULL,

    rental_start_at   TIMESTAMPTZ NOT NULL,
    rental_end_at     TIMESTAMPTZ NOT NULL,

    status            VARCHAR(20) NOT NULL DEFAULT 'HELD',
    hold_expires_at   TIMESTAMPTZ,

    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_rental_allocations_inventory_unit
        FOREIGN KEY (inventory_unit_id)
            REFERENCES inventory_units (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_rental_allocations_order_item
        FOREIGN KEY (order_item_id)
            REFERENCES order_items (id)
            ON DELETE RESTRICT,

    CONSTRAINT chk_rental_allocations_period
        CHECK (rental_end_at > rental_start_at),

    CONSTRAINT chk_rental_allocations_status
        CHECK (status IN ('HELD', 'CONFIRMED', 'RELEASED', 'COMPLETED'))
);

CREATE INDEX idx_rental_allocations_order_item
    ON rental_allocations (order_item_id);

CREATE INDEX idx_rental_allocations_expiring_holds
    ON rental_allocations (hold_expires_at) WHERE status = 'HELD';

ALTER TABLE rental_allocations
    ADD CONSTRAINT ex_rental_allocations_no_overlap EXCLUDE USING GIST (
        inventory_unit_id WITH =,
        tstzrange(rental_start_at, rental_end_at, '[)') WITH &&
    )
    WHERE (status IN ('HELD', 'CONFIRMED'));
