# Tenant integrations

Built-in integrations implement `IntegrationPlugin` and are discovered as Spring components.
`GET /api/integrations` lists their metadata and the current tenant's installation status.
Other backend modules can inject `OutboundEmailProvider` to send through the active email plugin without depending on SMTP. Its requests are validated and use the same deduplicated delivery path as the UI.
Installations and message history live in tenant schemas; public-schema requests are rejected.
The existing authenticated tenant membership filter also protects these endpoints.

## Email: SMTP + IMAP

Open **Settings → Integrations → Email** (`/merchant/integrations/email`).
Enable incoming and/or outgoing email, configure the provider's host, port, username and password,
and save with the integration enabled. Use an app password where the provider requires one.
SSL uses implicit TLS; STARTTLS is mandatory when selected. Certificate identity checking is enabled.
OAuth-only providers need a future OAuth connector; this release uses password/app-password authentication.

Connection checks authenticate against the saved account without sending a message.
Outbound messages are plain text, with one recipient. The recipient and content are entered explicitly.
An SMTP acceptance is recorded as `SENT`; it is not proof of delivery to the final mailbox.
A timeout/failure is recorded as `UNKNOWN` and is never automatically retried.
A process crash after recording `SENDING` also requires manual delivery verification.
Client-generated request IDs prevent duplicate sends on HTTP retries. Use the same request ID and body
when recovering a failed HTTP request; a reused ID with different content is rejected.

Inbound synchronization is manual. Each sync considers only the latest 50 messages in the configured folder.
It does not backfill older mail, delete server messages, or mark messages as read. The connection opens the
folder read-only and uses PEEK. Imported mail is deduplicated by mailbox identity, UIDVALIDITY and UID.
Bodies are bounded, converted to plain text, and rendered without HTML. Attachments are omitted;
messages above 2 MB or with unknown size are skipped. Both inbox and outbound history are paginated.
Disconnecting removes the installation and encrypted credentials but preserves message history.

No order notifications or scheduled polling are enabled automatically. Those can call the email capability
once their event rules and templates are configured.

## Encryption key

Set `APP_INTEGRATIONS_ENCRYPTION_KEY` to a base64-encoded **32-byte** random key on the backend.
Generate one with `openssl rand -base64 32` and inject it through the deployment's secret manager.
Keep it stable across restarts and share the same key between replicas. Back it up independently of the database.
Changing or losing the key makes existing credentials unreadable; key rotation is not implemented yet.

For local development, if no key is configured, the backend creates `data/integrations.key` on first save.
This file is outside the database, permission restricted, and ignored by Git. Preserve it across restarts.
Production deployments should always supply the environment key rather than rely on a local container filesystem.
AES-256-GCM authenticated encryption binds the ciphertext to the tenant and plugin code.
Passwords are never included in settings responses; blank password fields preserve saved credentials.

## Database migration

`V34__create_integrations_and_email.sql` creates tenant installation and message tables.
Use the existing tenant migration maintenance runner (`--app.maintenance.migrate-tenants=true`) for existing tenants; new tenants receive it during setup.
Do not apply the SQL manually without Flyway bookkeeping. Restart/rebuild the backend after adding the mail starter.

## Endpoints

- `GET /api/integrations`
- `GET|PUT|DELETE /api/integrations/email`
- `POST /api/integrations/email/check-smtp` and `/check-imap`
- `POST /api/integrations/email/sync`
- `GET /api/integrations/email/messages?direction=INBOUND|OUTBOUND&page=0`
- `GET /api/integrations/email/messages/{id}`
- `POST /api/integrations/email/messages` with `{requestId, to, subject, body}`

Saving uses `{enabled, configuration, smtpPassword, imapPassword}`. Passwords are write-only fields;
the response includes `smtpPasswordSet` / `imapPasswordSet` flags instead.
