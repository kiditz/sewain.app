# Sewain WhatsApp gateway

Run from `whatsapp-api/`:

```sh
cp .env.example .env
# Fill API_KEY with a random private value, e.g. openssl rand -hex 32.
docker compose up -d --build
docker compose ps
curl http://127.0.0.1:3001/ping
```

The API is available only on the host loopback address, port 3001. Nuxt remains on 3000 and Spring on 8080.
All session/client endpoints require the `x-api-key` header matching `API_KEY`. Keep that value in Spring,
never in the Nuxt public runtime configuration. `/ping` is an unauthenticated health endpoint.

The gateway runs as the non-root `node` user. Docker Compose caps it at 1 CPU and 1 GiB RAM by default.
Increase `WHATSAPP_MEMORY_LIMIT` and `WHATSAPP_CPU_LIMIT` as concurrent sessions grow. The healthcheck only
checks HTTP availability; it does not prove individual WhatsApp sessions are connected.

Sessions and browser cache use named volumes. Upload staging uses a temporary 64 MiB filesystem.
`docker compose down` keeps sessions; `docker compose down -v` deletes them and requires QR linking again.
Back up the session volume as credentials: it contains linked-device authentication data.

`BASE_WEBHOOK_URL` points to Spring on the host using `host.docker.internal`. Linux also receives this host mapping.
The `/api/integrations/whatsapp/webhook` backend route is a placeholder for the upcoming integration and does not
exist yet. Until it is implemented, callbacks to that URL will fail. No session is started or message sent by
`docker compose up`; only saved sessions are restored if the volume already contains them.

Use a shared private Docker network and service DNS names if Spring is also containerized.
Do not expose the gateway port publicly. This standalone Compose does not change the existing PostgreSQL service.
Swagger and the sample callback endpoint are disabled. `src/compose.yml` is a legacy unrelated deployment;
use the root `whatsapp-api/compose.yaml` instead.

```sh
docker compose logs -f --tail=100 whatsapp-api
docker compose restart whatsapp-api
docker compose down
```
