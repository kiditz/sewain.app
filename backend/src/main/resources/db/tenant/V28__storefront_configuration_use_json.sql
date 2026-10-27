ALTER TABLE storefront_configurations
    ALTER COLUMN configuration DROP DEFAULT,
    ALTER COLUMN configuration TYPE JSON USING configuration::text::json,
    ALTER COLUMN configuration SET DEFAULT '{}'::json;
