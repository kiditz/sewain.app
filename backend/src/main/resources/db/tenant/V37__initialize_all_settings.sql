-- Register every SettingKey before merchant features read settings.
-- Insert missing rows only: existing merchant preferences must remain unchanged.
INSERT INTO settings (setting_key, setting_value)
VALUES
    ('REGIONAL', jsonb_build_object(
        'countryId', (SELECT id FROM public.countries WHERE iso2 = 'ID' LIMIT 1),
        'currency', 'IDR',
        'currencyPosition', 'before',
        'timezone', 'Asia/Jakarta',
        'locale', 'id',
        'dateFormat', 'DD/MM/YYYY',
        'firstDayOfWeek', 'monday'
    )),
    ('RENTAL_PLAN', '[
        {"durationUnit":"DAY","durationValue":1,"percentage":3}
    ]'::jsonb),
    ('PAYMENT', '{
        "bankTransfer": false,
        "cashAtPickup": true,
        "cashAtReturn": false,
        "instructions": "",
        "downPaymentEnabled": true,
        "downPaymentType": "percentage",
        "downPaymentValue": 30,
        "fullPaymentEnabled": true
    }'::jsonb),
    ('PAYMENT_BANK_ACCOUNTS', '{"accounts":[]}'::jsonb),
    ('RENTAL', '{
        "minimumNoticeHours": 2,
        "maximumAdvanceDays": 90,
        "bufferHours": 0,
        "operatingDays": ["MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY"],
        "openingTime": "09:00",
        "closingTime": "18:00",
        "lateReturnEnabled": true,
        "lateReturnGraceHours": 1
    }'::jsonb)
ON CONFLICT (setting_key) DO NOTHING;
