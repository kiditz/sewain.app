-- Gateway callbacks have no tenant header: resolve only from a server-generated session ID.
CREATE TABLE public.whatsapp_sessions (
 session_id UUID PRIMARY KEY, tenant VARCHAR(255) NOT NULL UNIQUE,
 enabled BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
