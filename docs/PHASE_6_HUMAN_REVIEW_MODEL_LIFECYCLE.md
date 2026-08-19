# Phase 6 Human Review and Model Lifecycle

Phase 6 makes inspection predictions reproducible, reviewable, and measurable.

## Versioned Prediction Records

Each async inspection job now stores:

- input checksum and object key
- model, dataset, preprocessing, label-map, threshold, and scoring-profile versions
- labels, classification, confidence, latency, calibrated score, quality band, automated decision, and final decision
- policy-sensitive routing flag and the threshold used for that prediction

Low-confidence or policy-sensitive predictions move to `REVIEW_REQUIRED`.

## Reviewer Workflow

- `POST /api/v2/inspection-jobs/{jobId}/reviews`
  - Organization manager action.
  - `ACCEPT` approves the automated result.
  - `CORRECT` replaces labels/classification and queues a retraining candidate.
  - `REJECT` rejects the automated result and queues a retraining candidate.
- `GET /api/v2/inspection-jobs/{jobId}/reviews`
  - Organization member read of append-only review actions.

Review actions are stored in `inspection_review_actions`; corrections and rejections are also copied to `inspection_retraining_candidates`.

## Calibrated Scoring

`InspectionScoringService` applies product-specific thresholds from:

- `INSPECTION_REVIEW_CONFIDENCE_THRESHOLD`
- `INSPECTION_SCORING_PRODUCT_THRESHOLDS`
- `INSPECTION_POLICY_SENSITIVE_LABELS`

The scoring profile and threshold versions are persisted on every prediction.

## Evaluation Command

Run a held-out prediction report:

```bash
cd yolov3-service
python evaluate_model.py --predictions path/to/predictions.jsonl --out reports/inspection-eval.json
```

Optional candidate comparison:

```bash
python evaluate_model.py --predictions baseline.jsonl --candidate candidate.jsonl --out reports/model-comparison.json
```

The report includes precision, recall, F1, approximate mAP, confusion matrix, false accept/reject counts, and latency metrics.

## Verification

- `cd supplytracker1 && mvn verify`
- `cd yolov3-service && python -m unittest discover -s tests -v`
- `cd yolov3-service && python -m compileall -q app.py evaluate_model.py tests`
