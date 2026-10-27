INSERT INTO settings (setting_key, setting_value)
VALUES ('RENTAL_PLAN',
        '[
            {
                "durationUnit": "DAY",
                "durationValue": 1,
                "percentage": 3
            }
        ]'::jsonb) ON CONFLICT (setting_key)
DO
UPDATE SET
    setting_value = EXCLUDED.setting_value,
    updated_at = CURRENT_TIMESTAMP;