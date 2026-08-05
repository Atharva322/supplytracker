# SupplyTracker Capability Status

This file prevents roadmap items and unverified measurements from being presented as completed production capabilities.

## Implemented today

- React/Vite frontend with product, farm, tracking, dashboard, GraphQL playground, notification, and object-detection views.
- Spring Boot 3.3.6 / Java 21 backend using MongoDB.
- JWT/password authentication, Google OAuth integration, and method-level role checks on selected mutations.
- REST product/farm/auth/detection APIs and a GraphQL schema/controller layer.
- Product tracking history embedded in product documents.
- SSE product-update stream and STOMP-based notification components.
- FastAPI/OpenCV YOLOv3 inference service with detect, batch-detect, quality-check, and health endpoints.
- AWS Bedrock description integration with a local fallback description.
- Prometheus/Grafana/Loki configuration and Docker deployment files.

## Present but not fully integrated

- Redis containers/configuration exist, but the Spring cache currently uses `ConcurrentMapCacheManager`; Redis is not the application cache source of truth.
- Prometheus/Grafana/Loki assets exist, but production SLOs and end-to-end observability have not been verified.
- GraphQL subscription types exist in the schema, while the current UI uses SSE for product updates; subscription support must be verified before it is claimed.
- AWS S3 dependencies/configuration exist, but the current detection request path sends multipart bytes synchronously to FastAPI.

## Planned architecture

- Organizations/facilities and organization-scoped authorization.
- ProductBatch v2 and immutable traceability event ledger.
- Idempotent commands and transactional outbox messaging.
- Custody transfers, shipments, cold-chain sensor processing, and incident alerts.
- Asynchronous, versioned AI inspection jobs with object storage, retries/DLQ, and human review.
- Batch split/merge genealogy and targeted recall traversal.
- QR public traceability and bounded offline/PWA workflows.
- OpenTelemetry traces, SLOs, load/failure testing, and reproducible performance reports.

## Implemented on `agent/phases-1-4-supply-chain` (pending Java 21 CI before merge)

- Security hardening for registration, JWT configuration, business routes, notification ownership, product SSE, STOMP authentication, and bootstrap credentials.
- V1 product application/query services with bounded pagination, sort allowlisting, Mongo-backed search, and GraphQL delegation.
- Organization/facility/membership tenant model and `/api/v2` authorization boundary.
- Versioned ProductBatch projection, typed state machine, append-only trace ledger, idempotency records, transactional outbox, and legacy migration dry-run/write workflow.
- Custody transfer, quality-gated shipment dispatch/receipt, sensor ingestion, cold-chain excursion incidents, trace events, and organization-scoped notifications.

See `docs/PHASES_1_4_IMPLEMENTATION.md` for APIs, verification gates, and blockers.

## Measurements that require re-verification

Do not use the existing README claims for model accuracy, sub-200 ms inference, GraphQL performance improvement, 99.9% availability, cost savings, image throughput, or concurrent-user capacity on a resume until a reproducible benchmark/evaluation report is committed.

## Phase 0 verification contract

Every pull request should run:

- `mvn verify` for the backend.
- `npm test`, `npm run lint`, and `npm run build` for the frontend.
- Python compile and unit tests for the inference service.
- Docker Compose configuration validation.

Phase 1 will address the security blockers already identified in the architecture/risk plan; Phase 0 intentionally does not mark those items as resolved.
