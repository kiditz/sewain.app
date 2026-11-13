-- Tenant GiST exclusion constraints need these operator classes during provisioning.
-- Keep the shared extension visible to every Flyway tenant search path.
CREATE EXTENSION IF NOT EXISTS btree_gist WITH SCHEMA public;

-- Earlier installations may have created the relocatable extension in a tenant.
-- Relocation preserves object OIDs and existing index dependencies.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_extension e
        JOIN pg_namespace n ON n.oid = e.extnamespace
        WHERE e.extname = 'btree_gist' AND n.nspname <> 'public'
    ) THEN
        ALTER EXTENSION btree_gist SET SCHEMA public;
    END IF;
END $$;
