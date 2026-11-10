# WhatsApp integration

Open **Merchant → Integrasi → WhatsApp** (`/merchant/integrations/whatsapp`). Connect, then scan the QR from the merchant phone's WhatsApp → Linked devices. Each tenant has a server-generated UUID session. Browsers never receive the global API key or choose a gateway URL/session ID. This uses the bundled WhatsApp Web gateway, not Meta Cloud API.

Implemented: connect/QR/status, unlink, outbound direct text, incoming direct text, paginated history, provider capability `OutboundWhatsAppProvider`, STOMP notifications with HTTP snapshots after reconnect. No polling. No groups, media sending/downloads, historic-chat sync, order automation, delivery/read receipts or guaranteed callback replay yet. The Node gateway retries callbacks three times; messages received during a longer backend outage may be missing from the inbox. Sending is only exposed via the explicit send action/provider call; connecting never sends a test message automatically.

## Docker setup

Root Compose already has the gateway and durable `whatsapp-sessions` volume. The new wiring passes the existing root `.env` `WHATSAPP_API_KEY` to backend and gateway. It also uses that secret for HMAC callback signatures. No new secret is required. Gateway URL is internal `http://whatsapp-api:3000`; callback URL is `http://backend:8080/api/integrations/whatsapp/webhook`. Never put this API key into Nuxt public config.

When ready, from repository root:

```sh
docker compose build backend web whatsapp-api
docker compose up -d --no-deps backend web whatsapp-api
```

These commands were not run by the implementation agent. Backend startup applies public Flyway V12 plus tenant V36 when `MIGRATE_TENANTS=true` (current default). Keep existing volumes; don't reset the DB or remove volumes. Unlink removes that phone session from the gateway but retains integration message history. Local non-Docker backend settings: `WHATSAPP_BASE_URL` (default localhost:3001), `WHATSAPP_API_KEY`, optional `WHATSAPP_WEBHOOK_SECRET` (default same API key). Node needs matching `WEBHOOK_SECRET`, `WEBHOOK_TEXT_ONLY=true`, and correct `BASE_WEBHOOK_URL`.

## Security and delivery behavior

- All merchant operations use the authenticated/resolved tenant. Session IDs are looked up on the server. Callback tenant selection ignores tenant headers/host and resolves only through `public.whatsapp_sessions` after verifying an HMAC-SHA256 signature over `timestamp.rawBody` and a timestamp window. Blank secrets reject callbacks. Payload capped at 64 KB. QR is only returned through authenticated, non-cached status responses, not persisted or included in callback notifications.
- Webhook text projection drops media and client internals. Inbound messages deduplicate on `(session_id,external_id)` inside the tenant table. Socket subscriptions only use the authenticated personal queue; notifications contain a session ID, never message text or QR. Member lists are resolved server-side, then clients retrieve authorized snapshots.
- Outbound messages reserve a unique requestId before sending. Reusing it returns the saved result and rejects changed payloads. Gateway failures/timeouts become UNKNOWN and are not auto-retried; WhatsApp may already have accepted the message. Backend restart changes stranded SENDING entries to UNKNOWN. SENT means gateway accepted, not delivered/read. Check the phone before deliberately creating a new message after an uncertain outcome.
- Disable takes effect before remote termination. If termination fails, the session remains recorded so it can be retried. Phone LocalAuth is stored in `whatsapp-sessions`, so container recreation preserves linked devices. Service remains internal/loopback-bound as in existing Compose.
- One backend instance is assumed (startup SENDING recovery); gateway sessions also currently run in a single Node process. Callback retries are not a durable queue.

## Verification commands (run manually)

```sh
cd backend
./gradlew test --tests 'app.sewain.integrations.whatsapp.*' --tests app.sewain.config.ProvisioningWebSocketConfigurationTest
```

From repository root, Node unit test without launching Chromium:

```sh
cd whatsapp-api
npm test -- --runTestsByPath tests/webhook.test.js
```

Manual checks:

1. Open integrations in two different merchants; connect each and confirm different QR/number/session/history. An unselected/public tenant must be rejected.
2. Scan QR, watch status transition to Connected without polling, refresh page/reconnect socket, and confirm snapshot recovers. Recreate gateway container without deleting volumes and confirm phone remains linked.
3. Explicitly send text to an authorized test number; send the same requestId again and confirm only one gateway send. Change payload with that ID and expect 409.
4. Reply from that phone; confirm incoming text appears in the correct merchant, including after a duplicate webhook. Confirm groups/media don't enter this text-only inbox.
5. Tamper signature/body/timestamp or submit an unknown/disabled session ID; no message should be persisted. Other users' socket queues must be rejected.
6. Simulate send timeout, confirm UNKNOWN and no automatic resend. Restart backend with a SENDING entry, expect UNKNOWN.
7. Unlink while starting/QR/connected; verify no recovery handler resurrects the deleted session and that history remains. If gateway is unavailable, expect disabled integration plus a retryable termination error.

Only `node --check` for modified gateway JS and `git diff --check` were run. Java/Node test suites, builds, containers, migrations and real WhatsApp pairing/messages were not run.
