CREATE TABLE merchant_order_activities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    order_number VARCHAR(50) NOT NULL,
    customer_name VARCHAR(150) NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    previous_status VARCHAR(30),
    current_status VARCHAR(30),
    payment_stage VARCHAR(20),
    amount NUMERIC(14, 2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_merchant_order_activities_created_at
    ON merchant_order_activities (created_at DESC);

CREATE INDEX idx_merchant_order_activities_order_id
    ON merchant_order_activities (order_id, created_at DESC);
