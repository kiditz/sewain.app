CREATE TABLE storefront_submissions (
    id UUID PRIMARY KEY,
    kind VARCHAR(20) NOT NULL CHECK (kind IN ('CONTACT', 'EMAIL_SIGNUP')),
    name VARCHAR(120) NOT NULL DEFAULT '',
    email VARCHAR(254) NOT NULL,
    message VARCHAR(5000) NOT NULL DEFAULT '',
    consent BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_storefront_signup_email ON storefront_submissions (lower(email)) WHERE kind = 'EMAIL_SIGNUP';
CREATE INDEX ix_storefront_submissions_created ON storefront_submissions (created_at DESC);
