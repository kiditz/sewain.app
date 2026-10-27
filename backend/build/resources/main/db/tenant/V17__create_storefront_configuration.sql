CREATE TABLE storefront_configurations
(
    id              UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    config_key      VARCHAR(50) NOT NULL DEFAULT 'STOREFRONT',
    configuration   JSONB       NOT NULL DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uk_storefront_configurations_key UNIQUE (config_key)
);
