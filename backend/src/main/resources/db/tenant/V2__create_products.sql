-- ============================================================================
-- 1. PRODUCTS
-- Produk adalah konsep/kategori barang dalam katalog merchant.
-- Contoh: Carrier, Tenda, Sleeping Bag.
-- ============================================================================

CREATE TABLE products
(
    id          UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    media       TEXT,
    slug        VARCHAR(255) NOT NULL,
    description TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- ============================================================================
-- 2. ATTRIBUTES
-- Definisi atribut yang bisa digunakan ulang.
-- Contoh: Capacity, Color, Material.
-- ============================================================================

CREATE TABLE attributes
(
    id          UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_attributes_name UNIQUE (name)
);


-- ============================================================================
-- 3. ATTRIBUTE VALUES
-- Contoh:
-- Capacity -> 40L, 60L, 80L
-- Color    -> Black, Red
-- ============================================================================

CREATE TABLE attribute_values
(
    id           UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    attribute_id UUID         NOT NULL,
    value        VARCHAR(100) NOT NULL,
    slug         VARCHAR(100) NOT NULL,
    position     INT          NOT NULL DEFAULT 0,

    CONSTRAINT fk_attribute_values_attribute
        FOREIGN KEY (attribute_id)
            REFERENCES attributes (id)
            ON DELETE CASCADE,

    CONSTRAINT uk_attribute_values_slug
        UNIQUE (attribute_id, slug),

    CONSTRAINT uk_attribute_values_value
        UNIQUE (attribute_id, value),

    CONSTRAINT uk_attribute_values_id_attribute
        UNIQUE (id, attribute_id),

    CONSTRAINT chk_attribute_values_position
        CHECK (position >= 0)
);

CREATE INDEX idx_attribute_values_attribute_id
    ON attribute_values (attribute_id);


-- ============================================================================
-- 4. PRODUCT VARIANTS
-- Variant adalah spesifikasi barang yang ditawarkan untuk disewa.
-- Contoh: Carrier 60L, Carrier 80L.
-- ============================================================================

CREATE TABLE product_variants
(
    id             UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    product_id     UUID           NOT NULL,
    name           VARCHAR(255)   NOT NULL,
    sku            VARCHAR(100)   NOT NULL,
    media_list     TEXT[]         NOT NULL DEFAULT '{}',
    buy_price      NUMERIC(12, 2),
    deposit_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    is_active      BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_product_variants_product
        FOREIGN KEY (product_id)
            REFERENCES products (id)
            ON DELETE RESTRICT,

    CONSTRAINT uk_product_variants_product_sku
        UNIQUE (product_id, sku),

    CONSTRAINT uk_product_variants_product_id_id
        UNIQUE (product_id, id),

    CONSTRAINT chk_product_variants_buy_price
        CHECK (buy_price IS NULL OR buy_price >= 0),

    CONSTRAINT chk_product_variants_deposit
        CHECK (deposit_amount >= 0)
);

CREATE INDEX idx_product_variants_product_id
    ON product_variants (product_id);


-- ============================================================================
-- 5. PRODUCT VARIANT VALUES
-- Menghubungkan variant dengan nilai atributnya.
-- Satu variant hanya boleh punya satu nilai untuk setiap atribut.
-- ============================================================================

CREATE TABLE product_variant_values
(
    variant_id         UUID NOT NULL,
    attribute_id       UUID NOT NULL,
    attribute_value_id UUID NOT NULL,

    CONSTRAINT pk_product_variant_values
        PRIMARY KEY (variant_id, attribute_id),

    CONSTRAINT fk_variant_values_variant
        FOREIGN KEY (variant_id)
            REFERENCES product_variants (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_variant_values_attribute_value
        FOREIGN KEY (attribute_value_id, attribute_id)
            REFERENCES attribute_values (id, attribute_id)
            ON DELETE RESTRICT
);

CREATE INDEX idx_variant_values_attribute_value
    ON product_variant_values (attribute_value_id);


-- ============================================================================
-- 6. PRODUCT RENTAL PLANS
-- Harga sewa ditentukan per variant dan durasi.
-- ============================================================================

CREATE TABLE variant_rental_plans
(
    id             UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    variant_id     UUID           NOT NULL,
    duration_unit  VARCHAR(20)    NOT NULL,
    duration_value INT            NOT NULL,
    rental_fee     NUMERIC(12, 2) NOT NULL,
    is_active      BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_rental_plans_variant
        FOREIGN KEY (variant_id)
            REFERENCES product_variants (id)
            ON DELETE RESTRICT,

    CONSTRAINT uk_rental_plans_duration
        UNIQUE (variant_id, duration_unit, duration_value),

    CONSTRAINT chk_rental_plans_duration_unit
        CHECK (duration_unit IN ('HOUR', 'DAY', 'WEEK', 'MONTH')),

    CONSTRAINT chk_rental_plans_duration_value
        CHECK (duration_value > 0),

    CONSTRAINT chk_rental_plans_fee
        CHECK (rental_fee >= 0)
);

CREATE INDEX idx_rental_plans_variant_id
    ON variant_rental_plans (variant_id);


-- ============================================================================
-- 7. INVENTORY UNITS
-- Satu row mewakili satu barang fisik milik merchant.
-- ============================================================================

CREATE TABLE inventory_units
(
    id              UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    variant_id      UUID         NOT NULL,
    sku_barcode     VARCHAR(100) NOT NULL UNIQUE,
    brand           VARCHAR(100),
    model           VARCHAR(100),
    serial_number   VARCHAR(150),
    status          VARCHAR(50)  NOT NULL DEFAULT 'AVAILABLE',
    condition_notes TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_inventory_units_variant
        FOREIGN KEY (variant_id)
            REFERENCES product_variants (id)
            ON DELETE RESTRICT,

    CONSTRAINT chk_inventory_units_status
        CHECK (status IN (
                          'AVAILABLE',
                          'RENTED',
                          'MAINTENANCE',
                          'DAMAGED'
            ))
);

CREATE INDEX idx_inventory_units_variant_status
    ON inventory_units (variant_id, status);

CREATE INDEX idx_inventory_units_serial_number
    ON inventory_units (serial_number);