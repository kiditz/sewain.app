ALTER TABLE orders
    ADD COLUMN deposit_waived BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN deposit_waiver_reason VARCHAR(200);

ALTER TABLE orders
    ADD CONSTRAINT chk_orders_deposit_waiver_reason
        CHECK (deposit_waived = FALSE OR
               (deposit_waiver_reason IS NOT NULL AND length(trim(deposit_waiver_reason)) > 0));
