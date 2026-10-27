CREATE TABLE settings
(
    id            UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    setting_key   VARCHAR(100) NOT NULL,
    setting_value TEXT,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_settings_key UNIQUE (setting_key)
);