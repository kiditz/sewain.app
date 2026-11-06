# Sewain: full local Docker stack

The root `compose.yaml` runs PostgreSQL + pgvector, the Spring backend, the production Nuxt server,
and the WhatsApp gateway. It does not depend on the standalone Compose files in `backend/` or `whatsapp-api/`.
Images are built from the local source. Backend image builds explicitly skip tests.

## Setup and start

From the repository root:

```sh
cp .env.example .env
```

Fill these values in the root `.env`:

- `POSTGRES_PASSWORD`: choose a database password (`openssl rand -hex 32`).
- `WHATSAPP_API_KEY`: choose a private gateway key (`openssl rand -hex 32`).
- `INTEGRATIONS_ENCRYPTION_KEY`: base64-encoded 32-byte key (`openssl rand -base64 32`).

Keep `.env` private and preserve the encryption key. These settings belong to the root Compose;
`whatsapp-api/.env` is used only by that gateway's standalone Compose.

Stop local Nuxt/Spring processes or containers already occupying ports 3000, 8080, or 3001.
The database uses host port 5433 to avoid the existing development database on 5432.
Do not run the standalone WhatsApp Compose at the same time on port 3001.

When ready, run these commands yourself:

```sh
docker compose config --quiet
docker compose build
docker compose up -d
docker compose ps
```

Open:

- Web: http://sewain.app.localhost:3000
- API: http://localhost:8080
- WhatsApp health: http://127.0.0.1:3001/ping
- PostgreSQL: `127.0.0.1:5433`

The web hostname matches the application's existing local CORS/tenant conventions. Local merchant
hostnames look like `http://kiditz-outdoor.sewain.app.localhost:3000` (use the actual merchant slug).
Use the root hostname above rather than `localhost:3000` for browser API access with the current CORS policy.

All published ports bind to host loopback. The services communicate through Compose's private default
network using `postgres:5432`, `backend:8080`, and `whatsapp-api:3000`. This is a local deployment baseline;
HTTPS, public domains, a reverse proxy, and their CORS settings are separate deployment work.
Changing public ports/domains requires corresponding `PUBLIC_API_URL` and CORS configuration changes.

## Data and migrations

This stack creates **new named volumes**. It does not import the existing development database,
`backend/data`, `backend/upload`, or standalone WhatsApp sessions. Existing development files and
containers are not modified. To retain your current merchants/products, restore a database backup into
the new PostgreSQL database **before starting the backend for the first time**, and copy matching upload
files and JWT keys into the corresponding volumes. Do not restore into an already initialized database
without planning the restore. You can also keep using the development stack until your data is migrated.

Volumes:

- `postgres-data`: database, including tenant schemas.
- `backend-data`: generated JWT keys (preserve these to retain token validity).
- `backend-upload`: uploaded media.
- `whatsapp-sessions`: linked-device credentials.
- `whatsapp-cache`: WhatsApp Web cache.

The backend runs public Flyway migrations at startup. `MIGRATE_TENANTS=true` also invokes the existing
maintenance runner for registered tenant schemas. Disable it after migrations if you prefer a separately
managed migration step. New merchants receive tenant migrations during their normal setup.
The backend healthcheck checks its listening TCP port; it is not a database/mail/tenant migration check.
Confirm successful migration/startup in the backend logs before using the app.

`PUBLIC_API_URL` is the browser-facing API/media origin. `NUXT_API_BASE_URL` is set internally by Compose
so Nuxt SSR reaches Spring through `backend:8080` rather than its own container's localhost.

## WhatsApp integration status

The gateway is packaged and can expose its REST API after startup. The Spring WhatsApp connector and
`/api/integrations/whatsapp/webhook` route have not been implemented yet; callback delivery to that
placeholder URL will fail until the connector is added. Starting an empty gateway does not create a
WhatsApp session or send messages. Existing sessions in its volume are restored automatically.
API/session endpoints require `x-api-key`; never expose that secret through Nuxt public configuration.

## Operations

```sh
docker compose logs -f --tail=100 backend web whatsapp-api
docker compose restart backend
docker compose up -d --build backend web
docker compose down
```

`down` preserves named volumes. `down -v` deletes database, keys, uploads, and WhatsApp credentials.
The default resource limits are a starting point; raise them as workload/session counts grow.
No image build, container start, test suite, or deployment was executed while preparing this setup.
