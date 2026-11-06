CREATE TABLE integration_installations (
    id UUID PRIMARY KEY,
    code VARCHAR(80) NOT NULL UNIQUE,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    configuration JSONB NOT NULL DEFAULT '{}',
    encrypted_secrets TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE integration_email_messages (
    id UUID PRIMARY KEY,
    external_key VARCHAR(255) NOT NULL UNIQUE,
    direction VARCHAR(10) NOT NULL CHECK (direction IN ('INBOUND','OUTBOUND')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('RECEIVED','SENDING','SENT','UNKNOWN')),
    sender VARCHAR(512) NOT NULL,
    recipient VARCHAR(512) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_integration_email_messages_direction_date ON integration_email_messages(direction, occurred_at DESC);
