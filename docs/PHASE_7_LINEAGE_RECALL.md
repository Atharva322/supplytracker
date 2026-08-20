# Phase 7 Batch Genealogy and Targeted Recall

Phase 7 adds durable batch genealogy and recall scope generation.

## Lineage Operations

- `POST /api/v2/lineage/batches/{batchId}/split`
- `POST /api/v2/lineage/merge`
- `POST /api/v2/lineage/batches/{batchId}/derive`
- `POST /api/v2/lineage/batches/{batchId}/consume`
- `GET /api/v2/lineage/batches/{batchId}/upstream`
- `GET /api/v2/lineage/batches/{batchId}/downstream`
- `GET /api/v2/lineage/batches/{batchId}/traverse`

Lineage edges are immutable parent-child records with operation, quantity, unit, actor, timestamp, and metadata. Split and merge commands enforce quantity conservation, and every edge rejects cycles before persistence.

## Recall Cases

- `POST /api/v2/recalls`
  - Creates a recall case from a source batch.
  - `simulation=true` records scope and simulated notices without notifying users.
- `GET /api/v2/recalls/{recallId}`
- `GET /api/v2/recalls?organizationId=...`
- `POST /api/v2/recalls/{recallId}/acknowledgments`
- `POST /api/v2/recalls/{recallId}/resolution`

Recall cases persist affected batches, shipments, facilities, organizations, inventory holders, recipients, traversal stats, and decision explanations.

## Traversal Controls

- `LINEAGE_TRAVERSAL_MAX_DEPTH`
- `LINEAGE_TRAVERSAL_MAX_NODES`

Traversal is breadth-first, bounded, and cycle-protected.

## Verification

- `cd supplytracker1 && mvn verify`
