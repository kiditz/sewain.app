ALTER TABLE order_items
    ADD COLUMN rental_price_snapshot JSONB NOT NULL DEFAULT '{}'::jsonb;
