-- Late return fee (denda keterlambatan).
-- returned_at      : waktu pengembalian aktual (diisi saat return dicatat / order selesai)
-- late_minutes     : lama keterlambatan terhadap jadwal pengembalian (return_at)
-- late_fee_unit    : HOUR / DAY (null jika kebijakan denda tidak aktif)
-- late_fee_*       : hasil perhitungan; late_fee_assessed = sebelum waiver,
--                    late_fee_total = yang benar-benar ditagihkan (masuk total_amount)
ALTER TABLE orders
    ADD COLUMN returned_at            TIMESTAMPTZ,
    ADD COLUMN late_minutes           BIGINT         NOT NULL DEFAULT 0,
    ADD COLUMN late_fee_unit          VARCHAR(10),
    ADD COLUMN late_fee_unit_amount   NUMERIC(14, 2) NOT NULL DEFAULT 0,
    ADD COLUMN late_fee_units         INTEGER        NOT NULL DEFAULT 0,
    ADD COLUMN late_fee_assessed      NUMERIC(14, 2) NOT NULL DEFAULT 0,
    ADD COLUMN late_fee_total         NUMERIC(14, 2) NOT NULL DEFAULT 0,
    ADD COLUMN late_fee_waived        BOOLEAN        NOT NULL DEFAULT FALSE,
    ADD COLUMN late_fee_waiver_reason VARCHAR(200);

ALTER TABLE orders
    ADD CONSTRAINT chk_orders_late_fee_unit
        CHECK (late_fee_unit IS NULL OR late_fee_unit IN ('HOUR', 'DAY')),
    ADD CONSTRAINT chk_orders_late_fee_amounts
        CHECK (late_minutes >= 0
            AND late_fee_unit_amount >= 0
            AND late_fee_units >= 0
            AND late_fee_assessed >= 0
            AND late_fee_total >= 0),
    ADD CONSTRAINT chk_orders_late_fee_waiver_reason
        CHECK (late_fee_waived = FALSE OR
               (late_fee_waiver_reason IS NOT NULL AND length(trim(late_fee_waiver_reason)) > 0));
