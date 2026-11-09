ALTER TABLE public.merchants
    ADD COLUMN provisioning_status VARCHAR(20) NOT NULL DEFAULT 'PROVISIONING',
    ADD COLUMN provisioning_version BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_merchant_provisioning_status CHECK (provisioning_status IN ('PROVISIONING','READY','FAILED'));

-- Preserve existing, already provisioned tenants. Incomplete tenants resume at startup.
DO $$
DECLARE merchant RECORD;
BEGIN
    FOR merchant IN SELECT id, schema FROM public.merchants LOOP
        IF to_regclass(format('%I.settings', merchant.schema)) IS NOT NULL THEN
            UPDATE public.merchants SET provisioning_status = 'READY' WHERE id = merchant.id;
        END IF;
    END LOOP;
END $$;
