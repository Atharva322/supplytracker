# Phase 5 Async Inspection Pipeline

Phase 5 adds an asynchronous inspection workflow beside the legacy synchronous `/api/detection/*` endpoints.

## Backend API

- `POST /api/v2/inspection-jobs/upload-slot`
  - Authenticated organization member requests an upload location.
  - Returns object key, local upload URL, expiry, and max size.
- `PUT /api/v2/inspection-uploads/**`
  - Stores image bytes in the local object-storage adapter and returns checksum metadata only.
- `POST /api/v2/inspection-jobs`
  - Creates a queued inspection job and returns `202 Accepted`.
  - Requires `Idempotency-Key`.
  - Does not call model inference in the request path.
- `GET /api/v2/inspection-jobs/{jobId}`
  - Reads job status and results for authorized organization members.
- `GET /api/v2/inspection-jobs?organizationId=...`
  - Lists organization jobs.

## Worker and Queue

- `InspectionJobWorker` is scheduled but disabled by default.
- Enable with `INSPECTION_WORKER_ENABLED=true`.
- Queue messages move through `READY`, `IN_FLIGHT`, `RETRY`, `ACKED`, and `DLQ`.
- Transient worker failures schedule retry/backoff.
- Terminal failures move the queue message to `DLQ` and the job to `FAILED`.

## Inference Contract

- Java worker reads uploaded bytes from object storage, verifies checksum, and calls the YOLO service.
- Python service exposes `GET /contract` with `inspection-inference.v1`.
- Detection responses include contract, model, and dataset version metadata.

## Metrics

Published through Micrometer/Prometheus:

- `inspection.jobs.queued`
- `inspection.jobs.completed{status=...}`
- `inspection.jobs.retry`
- `inspection.jobs.failed`
- `inspection.queue.delay`

## Verification

- `cd supplytracker1 && mvn verify`
- `cd yolov3-service && python -m unittest discover -s tests -v`
- `cd yolov3-service && python -m compileall -q app.py tests`
