-- Initialize new and existing tenants without replacing merchant preferences.
-- Storage remains UTC; this timezone controls display and business calendar dates.
INSERT INTO settings (setting_key, setting_value)
VALUES (
    'REGIONAL',
    jsonb_build_object(
        'countryId', (SELECT id FROM public.countries WHERE iso2 = 'ID' LIMIT 1),
        'currency', 'IDR',
        'currencyPosition', 'before',
        'timezone', 'Asia/Jakarta',
        'locale', 'id',
        'dateFormat', 'DD/MM/YYYY',
        'firstDayOfWeek', 'monday'
    )
)
ON CONFLICT (setting_key) DO NOTHING;
