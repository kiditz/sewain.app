-- Keep the legacy STOREFRONT document as a migration backup. Runtime writes
-- now target individual PAGE/MENU records, THEME, and NAVIGATION.
INSERT INTO storefront_configurations (config_key, configuration, created_at, updated_at)
SELECT 'PAGE:' || (page->>'id'), page::json, source.created_at, source.updated_at
FROM storefront_configurations source
CROSS JOIN LATERAL jsonb_array_elements(COALESCE(source.configuration::jsonb->'pages', '[]'::jsonb)) page
WHERE source.config_key = 'STOREFRONT'
ON CONFLICT (config_key) DO NOTHING;

INSERT INTO storefront_configurations (config_key, configuration, created_at, updated_at)
SELECT 'THEME', (configuration::jsonb->'theme')::json, created_at, updated_at
FROM storefront_configurations
WHERE config_key = 'STOREFRONT' AND jsonb_typeof(configuration::jsonb->'theme') = 'object'
ON CONFLICT (config_key) DO NOTHING;

INSERT INTO storefront_configurations (config_key, configuration, created_at, updated_at)
SELECT 'MENU:' || (menu->>'id'),
       jsonb_build_object('id', menu->>'id', 'name', menu->>'name', 'items',
           COALESCE((SELECT jsonb_agg(jsonb_build_object('id', item->>'id', 'pageId', page->>'id') ORDER BY position)
                     FROM jsonb_array_elements(COALESCE(menu->'items', '[]'::jsonb)) WITH ORDINALITY AS entries(item, position)
                     JOIN LATERAL jsonb_array_elements(COALESCE(source.configuration::jsonb->'pages', '[]'::jsonb)) page
                       ON CASE WHEN item ? 'pageId' THEN item->>'pageId' = page->>'id'
                               ELSE item->>'url' = page->>'targetUrl' END), '[]'::jsonb))::json,
       source.created_at, source.updated_at
FROM storefront_configurations source
CROSS JOIN LATERAL jsonb_array_elements(COALESCE(source.configuration::jsonb->'MenuItems', '[]'::jsonb)) menu
WHERE source.config_key = 'STOREFRONT'
ON CONFLICT (config_key) DO NOTHING;

-- Older documents predate configurable menus. Preserve their default navigation.
INSERT INTO storefront_configurations (config_key, configuration, created_at, updated_at)
SELECT 'MENU:main-menu', jsonb_build_object('id', 'main-menu', 'name', 'Main Menu', 'items',
    COALESCE((SELECT jsonb_agg(jsonb_build_object('id', page->>'id', 'pageId', page->>'id')
                ORDER BY CASE page->>'kind' WHEN 'HOME' THEN 0 WHEN 'PRODUCT_GRID' THEN 1 ELSE 2 END)
              FROM jsonb_array_elements(COALESCE(source.configuration::jsonb->'pages', '[]'::jsonb)) page
              WHERE page->>'kind' IN ('HOME', 'PRODUCT_GRID', 'COLLECTIONS')), '[]'::jsonb))::json,
       source.created_at, source.updated_at
FROM storefront_configurations source
WHERE source.config_key = 'STOREFRONT' AND NOT (source.configuration::jsonb ? 'MenuItems')
ON CONFLICT (config_key) DO NOTHING;

INSERT INTO storefront_configurations (config_key, configuration, created_at, updated_at)
SELECT 'NAVIGATION', jsonb_build_object('headerMenuId',
    CASE WHEN configuration::jsonb ? 'headerMenuId' THEN configuration::jsonb->'headerMenuId'
         WHEN EXISTS (SELECT 1 FROM storefront_configurations WHERE config_key = 'MENU:main-menu') THEN '"main-menu"'::jsonb
         ELSE 'null'::jsonb END)::json, created_at, updated_at
FROM storefront_configurations WHERE config_key = 'STOREFRONT'
ON CONFLICT (config_key) DO NOTHING;

-- Enforce page identity even when separate editor requests race.
CREATE UNIQUE INDEX IF NOT EXISTS uk_storefront_page_slug ON storefront_configurations ((configuration->>'slug'))
    WHERE config_key LIKE 'PAGE:%';
CREATE UNIQUE INDEX IF NOT EXISTS uk_storefront_page_kind ON storefront_configurations ((configuration->>'kind'))
    WHERE config_key LIKE 'PAGE:%' AND configuration->>'kind' <> 'CUSTOM';
