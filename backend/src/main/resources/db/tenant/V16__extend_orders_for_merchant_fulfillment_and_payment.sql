-- Merchant-created rental orders share the existing order and payment model.
ALTER TABLE orders
    ADD COLUMN fulfillment_type VARCHAR(20) NOT NULL DEFAULT 'PICKUP',
    ADD COLUMN fulfillment_address JSONB,
    ADD COLUMN pickup_at TIMESTAMPTZ,
    ADD COLUMN delivery_at TIMESTAMPTZ,
    ADD COLUMN return_at TIMESTAMPTZ,
    ADD COLUMN payment_method VARCHAR(30),
    ADD COLUMN balance_due_at VARCHAR(20),
    ADD COLUMN payment_terms_snapshot JSONB NOT NULL DEFAULT '{}'::JSONB;

ALTER TABLE orders
    ADD CONSTRAINT chk_orders_fulfillment_type
        CHECK (fulfillment_type IN ('PICKUP', 'DELIVERY')),
    ADD CONSTRAINT chk_orders_payment_method
        CHECK (payment_method IS NULL OR payment_method IN ('BANK_TRANSFER', 'CASH')),
    ADD CONSTRAINT chk_orders_balance_due_at
        CHECK (balance_due_at IS NULL OR balance_due_at IN ('PICKUP', 'RETURN'));

CREATE INDEX idx_orders_fulfillment_created
    ON orders (fulfillment_type, created_at DESC);

-- Payment rows also represent merchant-recorded cash/transfer installments.
ALTER TABLE payments
    ADD COLUMN payment_stage VARCHAR(20) NOT NULL DEFAULT 'INITIAL',
    ADD COLUMN method VARCHAR(30);

ALTER TABLE payments
    ADD CONSTRAINT chk_payments_stage
        CHECK (payment_stage IN ('INITIAL', 'BALANCE', 'DEPOSIT')),
    ADD CONSTRAINT chk_payments_method
        CHECK (method IS NULL OR method IN ('BANK_TRANSFER', 'CASH'));

CREATE INDEX idx_payments_order_stage
    ON payments (order_id, payment_stage, created_at);
