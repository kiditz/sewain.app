ALTER TABLE public.merchants
    ADD COLUMN description TEXT,
    ADD COLUMN logo_url VARCHAR(500),
    ADD COLUMN favicon_url VARCHAR(500),
    ADD COLUMN contact_email VARCHAR(255),
    ADD COLUMN contact_phone VARCHAR(50),
    ADD COLUMN custom_domain VARCHAR(255),
    ADD COLUMN is_published BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN business_name VARCHAR(255),
    ADD COLUMN business_address TEXT,
    ADD COLUMN business_email VARCHAR(255),
    ADD COLUMN business_phone VARCHAR(50),
    ADD COLUMN tax_id VARCHAR(100);

CREATE UNIQUE INDEX uk_merchants_custom_domain
    ON public.merchants (custom_domain)
    WHERE custom_domain IS NOT NULL;