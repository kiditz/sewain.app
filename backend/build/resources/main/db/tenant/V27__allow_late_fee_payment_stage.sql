ALTER TABLE payments
    DROP CONSTRAINT chk_payments_stage;

ALTER TABLE payments
    ADD CONSTRAINT chk_payments_stage
        CHECK (payment_stage IN ('INITIAL', 'BALANCE', 'DEPOSIT', 'LATE_FEE'));
