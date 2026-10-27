
-- ============================================================================
-- 4. CATEGORIES
-- ============================================================================

-- ============================================================================
-- 5. PRODUCTS
-- ============================================================================
CREATE TABLE products
(
    id             UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    merchant_id    UUID           NOT NULL,
    category_id    UUID           NOT NULL,
    name           VARCHAR(255)   NOT NULL,
    slug           VARCHAR(255)   NOT NULL UNIQUE,
    logo_url       TEXT,
    description    TEXT,
    buy_price      DECIMAL(12, 2) NULL, -- Harga beli produk untuk perbandingan
    deposit_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_products_merchant FOREIGN KEY (merchant_id)
        REFERENCES merchants (id) ON DELETE RESTRICT,
    CONSTRAINT fk_products_category FOREIGN KEY (category_id)
        REFERENCES categories (id) ON DELETE RESTRICT
);

CREATE INDEX idx_products_merchant_id ON products (merchant_id);
CREATE INDEX idx_products_category_id ON products (category_id);
CREATE INDEX idx_products_slug ON products (slug);

-- ============================================================================
-- 6. PRODUCT RENTAL PLANS (DURASI & BIAYA SEWA BERTINGKAT)
-- ============================================================================
CREATE TABLE product_rental_plans
(
    id             UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    product_id     UUID           NOT NULL,
    duration_unit  VARCHAR(20)    NOT NULL, -- Contoh: '2 Minggu', '3 Minggu', '1 Bulan'
    duration_value INT            NOT NULL, -- Contoh: 14, 21, 30, 60
    rental_fee     DECIMAL(12, 2) NOT NULL, -- Harga sewa untuk durasi ini
    is_active      BOOLEAN        NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_rental_plans_product FOREIGN KEY (product_id)
        REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT chk_duration_days CHECK (duration_value > 0)
);

CREATE INDEX idx_rental_plans_product_id ON product_rental_plans (product_id);

-- ============================================================================
-- 7. INVENTORY UNITS (BARANG FISIK)
-- ============================================================================
CREATE TABLE inventory_units
(
    id              UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    product_id      UUID         NOT NULL,
    sku_barcode     VARCHAR(100) NOT NULL UNIQUE,
    status          VARCHAR(50)  NOT NULL DEFAULT 'AVAILABLE',
    condition_notes TEXT,

    CONSTRAINT fk_inventory_units_product FOREIGN KEY (product_id)
        REFERENCES products (id) ON DELETE RESTRICT,
    CONSTRAINT chk_inventory_units_status
        CHECK (status IN ('AVAILABLE', 'RENTED', 'MAINTENANCE', 'DAMAGED'))
);

CREATE INDEX idx_inventory_units_product_status ON inventory_units (product_id, status);

-- ============================================================================
-- 8. ORDERS
-- ============================================================================
CREATE TABLE orders
(
    id               UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    order_number     VARCHAR(100)   NOT NULL UNIQUE,
    user_id          UUID           NOT NULL,
    total_rental_fee DECIMAL(12, 2) NOT NULL,
    deposit_fee      DECIMAL(12, 2) NOT NULL,
    delivery_fee     DECIMAL(12, 2) NOT NULL,
    status           VARCHAR(50)    NOT NULL,
    payment_status   VARCHAR(50)    NOT NULL,
    created_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_orders_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE RESTRICT
);

CREATE INDEX idx_orders_user_id ON orders (user_id);
CREATE INDEX idx_orders_order_number ON orders (order_number);

-- ============================================================================
-- 9. ORDER ITEMS
-- ============================================================================
CREATE TABLE order_items
(
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id          UUID           NOT NULL,
    inventory_unit_id UUID           NOT NULL,
    rental_plan_id    UUID           NOT NULL, -- Merujuk ke plan durasi yang dipilih
    start_date        DATE           NOT NULL,
    end_date          DATE           NOT NULL,
    daily_rate        DECIMAL(12, 2) NOT NULL,
    subtotal          DECIMAL(12, 2) NOT NULL,

    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id)
        REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_inventory_unit FOREIGN KEY (inventory_unit_id)
        REFERENCES inventory_units (id) ON DELETE RESTRICT,
    CONSTRAINT fk_order_items_plan FOREIGN KEY (rental_plan_id)
        REFERENCES product_rental_plans (id) ON DELETE RESTRICT,
    CONSTRAINT chk_order_items_dates CHECK (end_date >= start_date)
);

CREATE INDEX idx_order_items_dates_lookup
    ON order_items (inventory_unit_id, start_date, end_date);


-- ============================================================================
-- 2. VERIFICATION DOCS
-- ============================================================================
CREATE TABLE verification_docs
(
    id          UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL,
    doc_type    VARCHAR(50)  NOT NULL,
    doc_url     VARCHAR(500) NOT NULL,
    status      VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    verified_at TIMESTAMP,

    CONSTRAINT fk_verification_docs_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_verification_docs_type
        CHECK (doc_type IN ('KTP', 'KK')),
    CONSTRAINT chk_verification_docs_status
        CHECK (status IN ('PENDING', 'VERIFIED', 'REJECTED'))
);

CREATE INDEX idx_verification_docs_user_id ON verification_docs (user_id);