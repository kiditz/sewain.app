# Async merchant provisioning

Public Flyway V10 adds persistent `PROVISIONING`, `READY`, `FAILED` status and an optimistic version.
Existing tenants with a settings table are treated as already provisioned. Incomplete tenants are resumed
at application readiness. Newly created merchants emit the existing durable Spring Modulith event.
The worker locks the merchant row, migrates its schema, commits the resulting status, then sends a user
notification. Duplicate workers serialize on that row and do not rerun a READY migration. FAILED jobs
are retried explicitly through the UI. A process interrupted while PROVISIONING resumes after restart.

The setup page uses the auth layout, not the merchant dashboard. On refresh/login it first resolves the
owner's merchants through the public-schema directory, then subscribes to STOMP and requests a status
snapshot. Subscription precedes snapshot SEND on the same connection; Spring preserves receive order.
Every reconnect repeats this sequence. Versions prevent old snapshots from overriding newer events.
UI delays are informational, not migration failures. There is no status polling.

## STOMP contract

- WebSocket endpoint: `/ws/provisioning` (native WebSocket, no SockJS).
- Authenticate `CONNECT` with `Authorization: Bearer <JWT>` in the STOMP frame, never the query string.
- Subscribe only to `/user/queue/provisioning`.
- Send `{merchantId}` to `/app/provisioning/status` or `/app/provisioning/retry`.
- Responses: `{merchantId,status,version}` or a safe `{error}` message.
- Server checks token validity, permitted destinations, and merchant ownership. Notifications are user-scoped.
- Heartbeats run every 10 seconds. Clients reconnect with exponential backoff, capped at 30 seconds.
- Tenant API access is rejected with 503 while the tenant is not READY.

The WebSocket connection uses the browser-facing API origin, converted to `ws:`/`wss:`. If deployed
behind a reverse proxy, forward Upgrade/Connection headers for `/ws/provisioning`. Configure
`APP_WEBSOCKET_ALLOWED_ORIGIN_PATTERNS` with the actual frontend origins. Do not use a wildcard in production.
The in-memory Spring broker supports the current single-backend setup. Multiple backend replicas need
shared event fanout/broker relay; otherwise live pushes can miss clients on another replica (reconnect
snapshots still read the database). Do not scale this implementation horizontally without adding that.

Logout waits for persisted state writes to settle, deletes auth/merchant/user/guest cookies, and performs
a full navigation to login. Provisioning WebSockets are closed when their page unmounts.

## Commands for the developer (not executed)

```sh
cd backend
./gradlew test --tests app.sewain.merchants.MerchantProvisioningServiceTest --tests app.sewain.config.ProvisioningWebSocketConfigurationTest
```

Manual checks after your own build/migrations:

1. Create a merchant: spinner is visible and no settings request is made before READY.
2. Refresh while provisioning: the same merchant resumes its status stream.
3. Disconnect/reconnect the network: snapshot recovers completion missed during disconnection.
4. Restart the backend mid-migration: pending work resumes; Flyway migration locking prevents conflicts.
5. Force a migration failure: FAILED is visible; retry does not start concurrent duplicate jobs.
6. A different user cannot request status/retry for this merchant or subscribe to arbitrary queues.
7. Logout: merchant/user/auth cookies disappear, and refresh does not restore the old identity.

No build, test suite, image build, or container command was run for this change.
