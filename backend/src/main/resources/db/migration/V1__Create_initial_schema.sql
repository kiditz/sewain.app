
-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================================
-- 1. USERS
-- ============================================================================
CREATE TABLE users
(
    id         UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    username   VARCHAR(100) NOT NULL,
    email      VARCHAR(255) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    full_name  VARCHAR(255) NOT NULL,
    enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users (email);


-- ============================================================================
-- 3. MERCHANTS
-- ============================================================================
CREATE TABLE merchants
(
    id          UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL, -- Owner/Admin dari Merchant
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(255) NOT NULL UNIQUE,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_merchants_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE RESTRICT
);

CREATE INDEX idx_merchants_slug ON merchants (slug);

CREATE TABLE merchant_users
(
    merchant_id UUID        NOT NULL,
    user_id     UUID        NOT NULL,
    role        VARCHAR(30) NOT NULL,

    PRIMARY KEY (merchant_id, user_id),

    CONSTRAINT fk_merchant_users_merchant
        FOREIGN KEY (merchant_id)
            REFERENCES merchants (id),

    CONSTRAINT fk_merchant_users_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
);
