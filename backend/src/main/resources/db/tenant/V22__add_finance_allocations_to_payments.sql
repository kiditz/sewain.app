ALTER TABLE payments
    ADD COLUMN rental_amount NUMERIC(14, 2) NOT NULL DEFAULT 0,
    ADD COLUMN deposit_amount NUMERIC(14, 2) NOT NULL DEFAULT 0;

WITH payment_receipts AS (
    SELECT p.id,
           p.amount,
           CASE
               WHEN p.payment_stage = 'DEPOSIT' OR p.payment_type = 'DEPOSIT' THEN 0
               WHEN COALESCE(o.payment_terms_snapshot #>> '{payment,separateDeposit}', 'false') = 'true' THEN p.amount
               ELSE LEAST(
                   p.amount,
                   GREATEST(
                       o.total_amount - o.deposit_total - COALESCE(
                           SUM(p.amount) OVER (
                               PARTITION BY p.order_id
                               ORDER BY p.paid_at, p.created_at, p.id
                               ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING
                           ), 0
                       ), 0
                   )
               )
           END AS rental_amount
    FROM payments p
    JOIN orders o ON o.id = p.order_id
    WHERE p.status IN ('SUCCEEDED', 'PARTIALLY_REFUNDED', 'REFUNDED')
)
UPDATE payments p
SET rental_amount = receipt.rental_amount,
    deposit_amount = receipt.amount - receipt.rental_amount
FROM payment_receipts receipt
WHERE receipt.id = p.id;

ALTER TABLE payment_refunds
    ADD COLUMN rental_amount NUMERIC(14, 2) NOT NULL DEFAULT 0,
    ADD COLUMN deposit_amount NUMERIC(14, 2) NOT NULL DEFAULT 0;

UPDATE payment_refunds pr
SET rental_amount = round(pr.amount * p.rental_amount / p.amount, 2),
    deposit_amount = pr.amount - round(pr.amount * p.rental_amount / p.amount, 2)
FROM payments p
WHERE p.id = pr.payment_id;
