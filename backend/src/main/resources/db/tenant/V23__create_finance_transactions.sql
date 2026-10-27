CREATE TABLE finance_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_type VARCHAR(20) NOT NULL,
    source_id UUID NOT NULL,
    direction VARCHAR(10) NOT NULL,
    category VARCHAR(30) NOT NULL,
    description VARCHAR(255) NOT NULL,
    order_id UUID,
    order_number VARCHAR(50),
    amount NUMERIC(14, 2) NOT NULL CHECK (amount > 0),
    currency CHAR(3) NOT NULL,
    payment_method VARCHAR(30),
    expense_category VARCHAR(30),
    notes TEXT,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_finance_transaction_source_category UNIQUE (source_type, source_id, category),
    CONSTRAINT chk_finance_transaction_direction CHECK (direction IN ('INCOME', 'EXPENSE')),
    CONSTRAINT chk_finance_transaction_category CHECK (category IN (
        'RENTAL_PAYMENT', 'DEPOSIT_RECEIVED', 'RENTAL_REFUND', 'DEPOSIT_REFUND', 'OPERATING_EXPENSE'
    )),
    CONSTRAINT chk_finance_transaction_source CHECK (source_type IN ('PAYMENT', 'REFUND', 'EXPENSE'))
);

CREATE INDEX idx_finance_transactions_occurred_at ON finance_transactions (occurred_at DESC);
CREATE INDEX idx_finance_transactions_order ON finance_transactions (order_id, occurred_at DESC);
CREATE INDEX idx_finance_transactions_direction ON finance_transactions (direction, occurred_at DESC);

INSERT INTO finance_transactions (
    source_type, source_id, direction, category, description, order_id, order_number,
    amount, currency, payment_method, occurred_at
)
SELECT 'PAYMENT', p.id, 'INCOME', 'RENTAL_PAYMENT', 'Rental payment · ' || o.order_number,
       o.id, o.order_number, p.rental_amount, p.currency, p.method, COALESCE(p.paid_at, p.created_at)
FROM payments p
JOIN orders o ON o.id = p.order_id
WHERE p.status IN ('SUCCEEDED', 'PARTIALLY_REFUNDED', 'REFUNDED') AND p.rental_amount > 0;

INSERT INTO finance_transactions (
    source_type, source_id, direction, category, description, order_id, order_number,
    amount, currency, payment_method, occurred_at
)
SELECT 'PAYMENT', p.id, 'INCOME', 'DEPOSIT_RECEIVED', 'Rental deposit · ' || o.order_number,
       o.id, o.order_number, p.deposit_amount, p.currency, p.method, COALESCE(p.paid_at, p.created_at)
FROM payments p
JOIN orders o ON o.id = p.order_id
WHERE p.status IN ('SUCCEEDED', 'PARTIALLY_REFUNDED', 'REFUNDED') AND p.deposit_amount > 0;

INSERT INTO finance_transactions (
    source_type, source_id, direction, category, description, order_id, order_number,
    amount, currency, payment_method, occurred_at
)
SELECT 'REFUND', pr.id, 'EXPENSE', 'RENTAL_REFUND', 'Rental refund · ' || o.order_number,
       o.id, o.order_number, pr.rental_amount, p.currency, p.method, COALESCE(pr.processed_at, pr.created_at)
FROM payment_refunds pr
JOIN payments p ON p.id = pr.payment_id
JOIN orders o ON o.id = p.order_id
WHERE pr.status = 'SUCCEEDED' AND pr.rental_amount > 0;

INSERT INTO finance_transactions (
    source_type, source_id, direction, category, description, order_id, order_number,
    amount, currency, payment_method, occurred_at
)
SELECT 'REFUND', pr.id, 'EXPENSE', 'DEPOSIT_REFUND', 'Deposit refund · ' || o.order_number,
       o.id, o.order_number, pr.deposit_amount, p.currency, p.method, COALESCE(pr.processed_at, pr.created_at)
FROM payment_refunds pr
JOIN payments p ON p.id = pr.payment_id
JOIN orders o ON o.id = p.order_id
WHERE pr.status = 'SUCCEEDED' AND pr.deposit_amount > 0;
