# Phases 1–4 Implementation and Verification

Branch: `agent/phases-1-4-supply-chain`

This branch evolves the Phase 0 baseline without deleting the legacy v1 product model. New supply-chain writes live under `/api/v2`; the old UI/API can continue operating while migration is dry-run and verified.

## Phase 1 — security foundation

- Public registration has no roles input and always assigns `ROLE_USER`.
- JWT signing material has no code fallback; the access-token default lifetime is 15 minutes.
- Business REST/GraphQL/detection/notification routes require authentication. Only login, registration, OAuth bootstrap, health/info, static content, and the STOMP handshake are public at the HTTP layer.
- STOMP `CONNECT` validates the JWT and derives the principal server-side. Client-supplied usernames are ignored; notification subscriptions are restricted to `/user/queue/notifications`.
- Product SSE is authenticated/user-scoped and no longer accepts query-string tokens or broadcasts full products globally.
- Notification read/delete operations query by notification ID + authenticated recipient.
- The built-in hard-coded demo administrator credential is gone. Demo bootstrap is disabled by default and requires an injected 12+ character password.
- Grafana/JWT fallback credentials were removed from runtime files and operational docs.

## Phase 2 — application/query services

- `ProductService` owns compatible v1 product commands.
- `ProductQueryService` owns paging, search, dashboard queries, a 100-row page cap, and an explicit sort allowlist.
- Product REST and GraphQL controllers delegate to services instead of implementing collection scans/business writes.
- Unsupported/global GraphQL subscriptions were removed from the schema and playground.
- Global error handling now maps validation, authorization, missing-resource, and conflict errors to stable HTTP status categories.

## Phase 3 — organization + batch v2 + trace ledger

New collections:

| Collection | Purpose |
|---|---|
| `organizations`, `facilities`, `memberships` | Tenant and facility boundary |
| `product_batches` | Typed/versioned current batch projection |
| `trace_events` | Append-only, uniquely sequenced batch history |
| `idempotency_records` | Command replay protection |
| `outbox_events` | Durable integration-event handoff |

`ProductBatch` uses `@Version` optimistic locking, typed quantity/unit/date/status/quality fields, and organization/custodian identity. `BatchTransitionPolicy` rejects skipped/backward lifecycle transitions.

All create/transition commands require an `Idempotency-Key`. Event `(batchId, sequenceNumber)`, public `batchId`, organization slug/facility code, membership, migration source, and sensor reading identities have unique/indexed constraints where applicable.

The legacy migration endpoint supports dry-run first, idempotent reruns, explicit default quantity/unit for fields missing from v1, date quarantine in its report, and conversion of embedded tracking history into immutable legacy-import events.

## Phase 4 — custody + shipment + cold chain

Workflow:

1. Current custodian offers the full unsplit batch quantity to another organization. Partial custody is intentionally deferred until the lineage split/merge phase can preserve quantity conservation.
2. A member of the recipient organization accepts custody.
3. A shipment can be created only for accepted custody when the batch is quality-approved and `READY_FOR_SHIPMENT`.
4. Sender dispatch moves the batch to `IN_TRANSIT` and appends a trace/outbox event.
5. Sensor readings are idempotent by `readingId`.
6. Temperature outside the shipment policy creates a `cold_chain_incidents` record, immutable batch event, outbox event, persistent notifications, and user-scoped real-time notifications for both organizations.
7. Recipient receipt completes shipment and custody, moves the batch to `DELIVERED`, and changes its custodian.

Use `scripts/simulate_cold_chain.py` to send a deterministic sequence containing an excursion to an in-transit shipment.

## API surface

- `/api/v2/organizations` and `/{id}/facilities`
- `/api/v2/batches` and `/{batchId}/transitions|timeline`
- `/api/v2/migrations/legacy-products`
- `/api/v2/custody-transfers` and `/{id}/accept`
- `/api/v2/shipments`, `/{id}/dispatch|receive`
- `/api/v2/shipments/{id}/sensor-readings|incidents`

## Verification gates

Automated unit coverage added for:

- public-registration role escalation;
- cross-user notification mutation protection;
- valid/invalid batch state transitions;
- idempotent batch command replay;
- organization creator ownership;
- legacy migration dry-run/failure reporting;
- custody recipient authorization;
- cold-chain incident/event/notification creation.

Required before merge:

```text
cd supplytracker1 && mvn verify                 # Java 21
cd supplytracker-frontend && npm test && npm run lint && npm run build
cd yolov3-service && python -m unittest discover -s tests -v
docker compose config --quiet
docker compose -f docker-compose.prod.yml config --quiet
```

Also run an integration environment with the MongoDB replica set and exercise create → transition → custody → dispatch → excursion → receive before merging.

## Known blockers / intentionally deferred work

- Mongo multi-document transactions require the replica set introduced in Compose. A standalone developer Mongo will reject transactional v2 commands.
- Mongo automatic index creation is enabled for the new uniqueness guarantees. Existing databases must be checked for duplicate legacy user/indexed values before first deployment; index creation should be treated as a deployment preflight, not silently bypassed.
- Java compilation/tests require Java 21 + Maven; do not waive the CI gate on this branch.
- The outbox is persisted transactionally in these phases; broker publishing/retry/DLQ workers belong to the later asynchronous platform/AI phase.
- Legacy `Product` records contain no quantity/unit, so migration requires explicit defaults and must be reviewed before writes.
- V2 intentionally coexists with v1. Do not delete `products`/embedded tracking history until migration parity has been measured.
- This branch does not claim load/performance numbers. Benchmark evidence is still required before adding metrics to a resume.
