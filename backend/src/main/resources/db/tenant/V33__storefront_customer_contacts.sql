ALTER TABLE customers ADD COLUMN source VARCHAR(30) NOT NULL DEFAULT 'MANUAL';
ALTER TABLE customers ADD COLUMN email_subscribed BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE customers ADD COLUMN email_consent_at TIMESTAMPTZ;
CREATE INDEX ix_customers_email_upper ON customers (upper(email));
ALTER TABLE storefront_submissions ADD COLUMN customer_id UUID REFERENCES customers(id) ON DELETE SET NULL;
CREATE INDEX ix_storefront_submissions_customer ON storefront_submissions (customer_id, created_at DESC);

-- Keep existing customer identities and profiles. Only create a contact when
-- no case-insensitive email match exists; retain every original form message.
INSERT INTO customers (id, full_name, email, enabled, source, created_at, updated_at)
SELECT gen_random_uuid(), left(COALESCE(NULLIF(trim(initial_contact.name), ''), initial_contact.email), 150), initial_contact.email, TRUE,
       CASE WHEN initial_contact.kind = 'EMAIL_SIGNUP' THEN 'EMAIL_SIGNUP' ELSE 'CONTACT_FORM' END,
       initial_contact.created_at, now()
FROM (
    SELECT DISTINCT ON (lower(trim(email))) lower(trim(email)) AS email, name, kind, created_at
    FROM storefront_submissions ORDER BY lower(trim(email)), created_at, id
) initial_contact
WHERE NOT EXISTS (SELECT 1 FROM customers c WHERE lower(c.email) = initial_contact.email)
ON CONFLICT DO NOTHING;

UPDATE storefront_submissions s SET customer_id = (
    SELECT c.id FROM customers c WHERE lower(c.email) = lower(trim(s.email))
    ORDER BY c.created_at, c.id LIMIT 1
) WHERE s.customer_id IS NULL;

UPDATE customers c SET email_subscribed = TRUE, email_consent_at = optin.consented_at
FROM (
    SELECT customer_id, min(created_at) AS consented_at FROM storefront_submissions
    WHERE kind = 'EMAIL_SIGNUP' AND consent = TRUE GROUP BY customer_id
) optin WHERE c.id = optin.customer_id;
