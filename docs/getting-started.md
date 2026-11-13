# Local setup and development

A multi-tenant rental application with a Spring Boot backend, Nuxt frontend, PostgreSQL, and a WhatsApp Web gateway. This guide covers local development with Docker and with services running directly on your computer.

## Project structure

| Folder/file | Contents |
| --- | --- |
| `backend/` | Spring Boot, Java 21, Gradle Wrapper, Flyway, business logic, and APIs |
| `web/` | Nuxt/Vue merchant dashboard and storefront |
| `whatsapp-api/` | Node.js gateway using whatsapp-web.js and Chromium |
| `compose.yaml` | Main Docker stack for all services |
| `.env.example` | Local stack configuration template |
| `web/e2e/` | Playwright tests with dependencies isolated from the web application |
| `scripts/` | Dummy data scripts and utilities |
| `docs/` | Feature documentation and testing guides |

## Environment setup

Run from the repository root. Copy the template **only on the first setup**. If `.env` already exists, edit it instead of overwriting it.

```sh
cp .env.example .env
```

Fill in these three secrets in `.env`. Run each generator and copy its output into the corresponding field. Do not commit the generated values:

```sh
# POSTGRES_PASSWORD
openssl rand -hex 32

# WHATSAPP_API_KEY
openssl rand -hex 32

# INTEGRATIONS_ENCRYPTION_KEY
openssl rand -base64 32
```

| Variable | Purpose |
| --- | --- |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | Docker database credentials; database/user default to `sewain` |
| `INTEGRATIONS_ENCRYPTION_KEY` | Base64-encoded 32-byte key for Email integration credentials; preserve it across restarts/deployments |
| `WHATSAPP_API_KEY` | Private gateway access; Compose also uses it as the webhook signing secret |
| `PUBLIC_API_URL` | Browser-accessible API/media URL; defaults to `http://localhost:8080` |
| `APP_WEBSOCKET_ALLOWED_ORIGIN_PATTERNS` | Browser origins allowed for STOMP; the template includes Sewain local domains |
| `MIGRATE_TENANTS` | Run migrations for existing merchant schemas at backend startup; defaults to `true` in root Compose |

Secrets belong to the backend/gateway. Do not put API or encryption keys in `NUXT_PUBLIC_*` configuration. Changing the password in `.env` after the PostgreSQL volume is initialized does not automatically change the existing database role password.

## Running with Docker

### Requirements

- Running Docker Engine/Desktop and Docker Compose v2 (`docker compose`).
- Available host ports: web `3000`, backend `8080`, PostgreSQL `5433`, gateway `3001`.
- Java, Node.js, and Gradle do not need to be installed on the host in this mode.

Use **the root `compose.yaml`**. Compose files in subfolders are separate configurations; do not start them simultaneously on the same ports.

### First build and startup

From the project root, after filling in `.env`:

```sh
docker compose config --quiet
docker compose build
docker compose up -d
docker compose ps
```

The backend Dockerfile already runs Gradle `bootJar -x test`, so you do not need to build a JAR first. Image builds do not run backend tests. The first build downloads base images, Java/npm dependencies, and Gradle.

Watch startup and migration logs:

```sh
docker compose logs -f --tail=100 backend web whatsapp-api
```

Confirm that the backend starts successfully and migrations have no errors. The current backend healthcheck checks only its TCP port, not the readiness of every tenant.

### Local addresses

| Service | Address from your computer/browser |
| --- | --- |
| Web | [sewain.app.localhost:3000](http://sewain.app.localhost:3000) |
| API/media | [localhost:8080](http://localhost:8080) |
| Gateway health | [localhost:3001/ping](http://localhost:3001/ping) |
| PostgreSQL | `127.0.0.1:5433`; database/user follow `.env` |

Use `sewain.app.localhost` for the web application to match local tenant/CORS host conventions. Merchant hosts can use `http://<merchant-slug>.sewain.app.localhost:3000`. If your resolver/browser does not recognize a hostname, map that hostname to `127.0.0.1` through your OS hosts/resolver settings.

Inside Docker, service addresses differ from browser addresses: `postgres:5432`, `backend:8080`, and `whatsapp-api:3000`. Compose sets `NUXT_API_BASE_URL=http://backend:8080` for SSR; browsers use `PUBLIC_API_URL`.

### Update only the changed service

Frontend only:

```sh
docker compose build web
docker compose up -d --no-deps web
```

Backend only:

```sh
docker compose build backend
docker compose up -d --no-deps backend
```

Gateway only:

```sh
docker compose build whatsapp-api
docker compose up -d --no-deps whatsapp-api
```

If Compose/environment configuration changes, recreate the affected service with `up -d`. `restart` alone does not apply new environment values or images. `--no-deps` prevents Compose from starting dependencies during a single-service update; those dependencies must already be running.

### Stopping services and persistent data

```sh
# Stop containers without removing them
docker compose stop

# Remove containers/network while preserving named volumes
docker compose down
```

| Named volume | Data |
| --- | --- |
| `postgres-data` | Database and tenant schemas |
| `backend-data` | JWT keys and backend data files |
| `backend-upload` | Media files under `/app/upload` |
| `whatsapp-sessions` | Linked WhatsApp device credentials |
| `whatsapp-cache` | Gateway cache |

**`docker compose down -v` deletes these volumes and their data.** Do not use it for routine updates. Native database/files on your computer do not automatically become part of Docker named volumes; migrating data requires backup/restore and copying the corresponding files.

## Running without Docker

### Requirements

- JDK **21**.
- Node.js **22** and npm, matching the Dockerfile.
- Local PostgreSQL; version **16** matches the Docker stack. The `pgcrypto` extension must be available.
- For the gateway: local Google Chrome/Chromium, or the Puppeteer browser installed with its dependencies.

A global Gradle installation is not required; use `backend/gradlew`. The current configuration disables vector-store auto-configuration, so the `vector` extension is not required for normal startup. Docker uses a PostgreSQL image with pgvector available if that feature is enabled later.

### 1. Set up local PostgreSQL

For a new installation, open `psql` as a PostgreSQL administrator. Adjust the administrator username to match your installation:

```sh
psql -U postgres -d postgres
```

Inside `psql`:

```sql
CREATE ROLE sewain LOGIN;
\password sewain
-- Enter the same password as POSTGRES_PASSWORD in .env.
CREATE DATABASE sewain OWNER sewain;
ALTER DATABASE sewain SET timezone TO 'UTC';
\connect sewain
CREATE EXTENSION IF NOT EXISTS pgcrypto;
\quit
```

Skip `CREATE ROLE`/`CREATE DATABASE` if they already exist. These examples use native PostgreSQL port **5432**, rather than Docker host port **5433**. The application role must own the database/schema and be able to create merchant schemas and run Flyway.

### 2. Backend — first terminal

Open a new terminal from the project root:

```sh
cd backend

# Spring does not load the root .env automatically. Load this project-owned file into the shell.
set -a
source ../.env
set +a

export SPRING_DOCKER_COMPOSE_ENABLED=false
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/${POSTGRES_DB:-sewain}"
export SPRING_DATASOURCE_USERNAME="${POSTGRES_USER:-sewain}"
export SPRING_DATASOURCE_PASSWORD="$POSTGRES_PASSWORD"
export APP_INTEGRATIONS_ENCRYPTION_KEY="$INTEGRATIONS_ENCRYPTION_KEY"
export APP_MAINTENANCE_MIGRATE_TENANTS="${MIGRATE_TENANTS:-true}"
export APP_MEDIA_STORAGE_PATH=upload
export APP_MEDIA_BASE_URL="${PUBLIC_API_URL:-http://localhost:8080}"
export WHATSAPP_BASE_URL=http://localhost:3001
export SERVER_PORT="${BACKEND_PORT:-8080}"
export SPRING_JPA_SHOW_SQL=false
export LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_SECURITY=INFO

./gradlew bootRun
```

`SPRING_DOCKER_COMPOSE_ENABLED=false` is important: Spring's development dependency may otherwise start `backend/compose.yaml`. Do not rely on credentials in `application.yaml`; the example overrides them through environment variables.

With `backend` as the working directory, JWT/data files are stored under `backend/data` and media under `backend/upload`. JVM/JDBC use UTC. Preserve these files across restarts, just as you preserve volumes in Docker mode.

### 3. Frontend — second terminal

From the project root:

```sh
cd web
npm ci
export NUXT_API_BASE_URL=http://localhost:8080
export NUXT_PUBLIC_API_BASE_URL=http://localhost:8080
npm run dev -- --host 0.0.0.0
```

Open [http://sewain.app.localhost:3000](http://sewain.app.localhost:3000). If the backend uses a different port, update both API URLs. If the web port changes, also update the allowed CORS/STOMP origins.

To run a local Nuxt production build:

```sh
npm run build
NUXT_API_BASE_URL=http://localhost:8080 NUXT_PUBLIC_API_BASE_URL=http://localhost:8080 node .output/server/index.mjs
```

Stop the development server first to avoid a port conflict.

### 4. WhatsApp gateway — third terminal (optional)

The backend/frontend can run without the gateway; WhatsApp integration works once the gateway is available. This example uses Chrome already installed on macOS:

```sh
cd whatsapp-api
set -a
source ../.env
set +a

export PORT=3001
export API_KEY="$WHATSAPP_API_KEY"
export WEBHOOK_SECRET="$WHATSAPP_API_KEY"
export WEBHOOK_TEXT_ONLY=true
export BASE_WEBHOOK_URL=http://localhost:8080/api/integrations/whatsapp/webhook
export SESSIONS_PATH=./sessions
export RECOVER_SESSIONS=true
export SET_MESSAGES_AS_SEEN=false
export ENABLE_LOCAL_CALLBACK_EXAMPLE=false
export ENABLE_SWAGGER_ENDPOINT=false
export CHROME_BIN='/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'

# A local browser is available, so skip the Puppeteer browser download.
PUPPETEER_SKIP_DOWNLOAD=true npm ci
npm start
```

On Linux/Windows, point `CHROME_BIN` to your Chrome/Chromium executable. To use Puppeteer's bundled browser, install dependencies without `PUPPETEER_SKIP_DOWNLOAD` and leave `CHROME_BIN` unset; installation may download a browser.

The gateway's `API_KEY` must match the backend's `WHATSAPP_API_KEY`, and `WEBHOOK_SECRET` must match the backend webhook secret. The example uses the same key through the backend fallback. If you configure a separate `WHATSAPP_WEBHOOK_SECRET`, use that value for the gateway's `WEBHOOK_SECRET`.

Check [http://localhost:3001/ping](http://localhost:3001/ping). Then open **Merchant → Integrations → WhatsApp → Connect WhatsApp** and scan the QR code from the merchant's phone. Native sessions are stored under `whatsapp-api/sessions`, separately from Docker volumes.

### Development alternative: Docker database only

To run Spring/Nuxt directly on the host with the database from root Compose:

```sh
# Run from the root after filling in .env.
docker compose up -d postgres
```

Follow the native backend steps, replacing the port 5432 URL with:

```sh
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5433/${POSTGRES_DB:-sewain}"
./gradlew bootRun
```

Apply this override and run `bootRun` in the `backend` terminal with its environment already loaded. If `.env` overrides `POSTGRES_PORT`, use that port. Keep following the native gateway steps for a local gateway to avoid mixing Docker and host callback addresses.

## Registration and merchant setup

1. Open the web application, register an account, and log in.
2. Create a merchant and wait for store setup to finish. Provisioning/Flyway creates the tenant schema and default settings.
3. Complete store, regional, rental, and payment settings.
4. Add products/variants/inventory, or open **Products → Import CSV** and download its template.
5. Email integration requires the merchant's SMTP/IMAP account. WhatsApp integration uses the merchant's phone number linked through a QR code.

Public Flyway migrations run at backend startup. For existing merchants, `APP_MAINTENANCE_MIGRATE_TENANTS=true` runs tenant migrations. Investigate missing settings/tables after updates together with migration logs. Wait for provisioning status READY before using merchant pages.

## Tests

Backend, from the repository root:

```sh
cd backend
./gradlew test
```

Unit tests use mocks/direct instances. The architecture test verifies modules and generates documentation without starting an application context. Gradle compiles all test sources even when `--tests` selects only one class.

Business logic suite guide: [docs/backend-business-unit-tests.md](backend-business-unit-tests.md).

Available frontend tests, from the repository root:

```sh
cd web
npm run test:storefront
npm run test:product-tags
```

Browser E2E uses an already running stack and separate dependencies:

```sh
cd web/e2e
npm install
npx playwright install chromium
```

Set `E2E_BASE_URL`, `E2E_EMAIL`, `E2E_PASSWORD`, `E2E_MERCHANT_NAME`, and `E2E_REQUIRE_AUTH=true`, then run `npm test` from that directory. Scenarios and reports: [web/e2e/README.md](../web/e2e/README.md).

## Troubleshooting

| Symptom | What to check |
| --- | --- |
| PostgreSQL connection refused | Service running, native port `5432` or Docker port `5433`, URL/credentials matching the selected mode |
| Register/login calls Nuxt `/api/auth/...` and returns 404 | `NUXT_PUBLIC_API_BASE_URL` must point to Spring; SSR also needs the correct `NUXT_API_BASE_URL` |
| STOMP rejected | `APP_WEBSOCKET_ALLOWED_ORIGIN_PATTERNS` includes the complete web origin: scheme, hostname, and port |
| Tenant settings/table missing | Provisioning/Flyway logs, `MIGRATE_TENANTS` in Compose or native `APP_MAINTENANCE_MIGRATE_TENANTS` |
| WhatsApp gateway unavailable | Gateway running, backend using the correct internal Docker URL or `localhost:3001`, matching API keys |
| Inbound messages missing | Callback URL reachable from gateway, matching webhook secret, active session; only direct text messages are supported |
| Updating web also starts backend | Build the specific service and recreate web with `docker compose up -d --no-deps web` |
| Gradle downloads again | The wrapper downloads its configured version when the cache is missing or the version changes; pruning build cache can also remove Docker's Gradle cache |

You do not need to delete volumes to fix the frontend, update images, or run tests. These commands are instructions; writing this README did not run builds, containers, migrations, or tests.

## Further documentation

- [Docker and storage](../DOCKER.md)
- [WhatsApp integration](whatsapp-integration.md)
- [Product CSV import](product-csv-import.md)
- [Backend outbound HTTP standard](backend-http-clients.md)
- [Backend business unit tests](backend-business-unit-tests.md)
- [Frontend E2E](../web/e2e/README.md)
