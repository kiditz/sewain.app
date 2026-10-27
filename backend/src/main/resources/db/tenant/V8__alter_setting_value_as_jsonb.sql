ALTER TABLE settings
ALTER COLUMN setting_value TYPE JSONB
    USING setting_value::jsonb;