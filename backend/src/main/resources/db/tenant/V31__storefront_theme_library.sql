-- Preserve the existing split resources; they remain the migration backup.
INSERT INTO storefront_configurations (id, config_key, configuration, created_at, updated_at)
SELECT gen_random_uuid(), 'THM:original', jsonb_build_object(
    'id', 'original', 'name', COALESCE((SELECT configuration->>'name' FROM storefront_configurations WHERE config_key = 'THEME'), 'My store'),
    'source', 'existing', 'version', '1.0.0', 'schemaVersion', 1,
    'draft', document, 'published', jsonb_set(document, '{pages}',
        COALESCE((SELECT jsonb_agg(page) FROM jsonb_array_elements(document->'pages') page WHERE page->>'status' = 'PUBLISHED'), '[]'::jsonb)),
    'revision', 0, 'history', '[]'::jsonb
)::json, now(), now()
FROM (SELECT jsonb_build_object(
    'theme', COALESCE((SELECT configuration::jsonb FROM storefront_configurations WHERE config_key = 'THEME'), '{}'::jsonb),
    'pages', COALESCE((SELECT jsonb_agg(configuration::jsonb ORDER BY created_at) FROM storefront_configurations WHERE config_key LIKE 'PAGE:%'), '[]'::jsonb)
) AS document) source
ON CONFLICT (config_key) DO NOTHING;

INSERT INTO storefront_configurations (id, config_key, configuration, created_at, updated_at)
VALUES (gen_random_uuid(), 'THEME_LIBRARY', '{"activeId":"original"}', now(), now())
ON CONFLICT (config_key) DO NOTHING;

-- Custom page content belongs to the store, so installing/switching a theme
-- never hides pages created after that theme was installed. These are drafts;
-- public reads still use the complete published snapshot.
INSERT INTO storefront_configurations (id, config_key, configuration, created_at, updated_at)
SELECT gen_random_uuid(), 'CONTENT_PAGE:' || (configuration->>'id'), configuration, created_at, updated_at
FROM storefront_configurations
WHERE config_key LIKE 'PAGE:%' AND configuration->>'kind' = 'CUSTOM'
ON CONFLICT (config_key) DO NOTHING;
