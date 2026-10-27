ALTER TABLE products
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    ADD CONSTRAINT chk_products_status
        CHECK (status IN ('DRAFT', 'ACTIVE', 'UNLISTED'));