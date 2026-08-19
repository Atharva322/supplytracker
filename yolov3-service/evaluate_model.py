import argparse
import json
import statistics
from collections import Counter, defaultdict
from pathlib import Path


def load_jsonl(path):
    rows = []
    with open(path, "r", encoding="utf-8") as handle:
        for line_number, line in enumerate(handle, start=1):
            stripped = line.strip()
            if stripped:
                row = json.loads(stripped)
                row["_line"] = line_number
                rows.append(row)
    return rows


def labels(row, key):
    value = row.get(key) or []
    if isinstance(value, str):
        return [value]
    return list(value)


def decision(row, key):
    return str(row.get(key) or "REVIEW").upper()


def label_metrics(rows):
    true_positive = Counter()
    false_positive = Counter()
    false_negative = Counter()
    confusion = defaultdict(Counter)
    for row in rows:
        expected = set(labels(row, "expected_labels"))
        predicted = set(labels(row, "predicted_labels"))
        for label in expected & predicted:
            true_positive[label] += 1
        for label in predicted - expected:
            false_positive[label] += 1
        for label in expected - predicted:
            false_negative[label] += 1
        expected_primary = sorted(expected)[0] if expected else "NONE"
        predicted_primary = sorted(predicted)[0] if predicted else "NONE"
        confusion[expected_primary][predicted_primary] += 1
    tp = sum(true_positive.values())
    fp = sum(false_positive.values())
    fn = sum(false_negative.values())
    precision = tp / (tp + fp) if tp + fp else 0.0
    recall = tp / (tp + fn) if tp + fn else 0.0
    f1 = 2 * precision * recall / (precision + recall) if precision + recall else 0.0
    return {
        "precision": precision,
        "recall": recall,
        "f1": f1,
        "true_positive": dict(true_positive),
        "false_positive": dict(false_positive),
        "false_negative": dict(false_negative),
        "confusion_matrix": {actual: dict(predicted) for actual, predicted in confusion.items()},
    }


def decision_metrics(rows):
    false_accept = 0
    false_reject = 0
    matrix = defaultdict(Counter)
    for row in rows:
        expected = decision(row, "expected_decision")
        predicted = decision(row, "predicted_decision")
        matrix[expected][predicted] += 1
        if expected != "APPROVE" and predicted == "APPROVE":
            false_accept += 1
        if expected == "APPROVE" and predicted != "APPROVE":
            false_reject += 1
    return {
        "false_accept": false_accept,
        "false_reject": false_reject,
        "decision_confusion_matrix": {actual: dict(predicted) for actual, predicted in matrix.items()},
    }


def latency_metrics(rows):
    values = [float(row["latency_ms"]) for row in rows if row.get("latency_ms") is not None]
    if not values:
        return {"count": 0, "average_ms": 0.0, "p95_ms": 0.0, "max_ms": 0.0}
    ordered = sorted(values)
    p95_index = min(len(ordered) - 1, int(round(0.95 * (len(ordered) - 1))))
    return {
        "count": len(values),
        "average_ms": statistics.fmean(values),
        "p95_ms": ordered[p95_index],
        "max_ms": max(values),
    }


def approximate_map(rows):
    labels_seen = sorted({label for row in rows for label in labels(row, "expected_labels") + labels(row, "predicted_labels")})
    if not labels_seen:
        return 0.0
    scores = []
    for label in labels_seen:
        relevant = 0
        precision_sum = 0.0
        ranked = sorted(rows, key=lambda row: float(row.get("confidence", 0.0)), reverse=True)
        hits = 0
        for index, row in enumerate(ranked, start=1):
            if label in labels(row, "expected_labels"):
                relevant += 1
                if label in labels(row, "predicted_labels"):
                    hits += 1
                    precision_sum += hits / index
        scores.append(precision_sum / relevant if relevant else 0.0)
    return statistics.fmean(scores)


def evaluate(rows, candidate_rows=None):
    report = {
        "samples": len(rows),
        "labels": label_metrics(rows),
        "decisions": decision_metrics(rows),
        "latency": latency_metrics(rows),
        "map": approximate_map(rows),
    }
    if candidate_rows is not None:
        report["candidate_comparison"] = {
            "baseline_f1": report["labels"]["f1"],
            "candidate_f1": label_metrics(candidate_rows)["f1"],
            "baseline_average_latency_ms": report["latency"]["average_ms"],
            "candidate_average_latency_ms": latency_metrics(candidate_rows)["average_ms"],
        }
    return report


def main():
    parser = argparse.ArgumentParser(description="Evaluate inspection model predictions against a held-out JSONL dataset.")
    parser.add_argument("--predictions", required=True, help="JSONL rows with expected_labels, predicted_labels, decisions, confidence, latency_ms.")
    parser.add_argument("--candidate", help="Optional candidate-model JSONL on the same held-out sample ids.")
    parser.add_argument("--out", required=True, help="Path to write the evaluation report JSON.")
    args = parser.parse_args()

    report = evaluate(load_jsonl(args.predictions), load_jsonl(args.candidate) if args.candidate else None)
    output = Path(args.out)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, indent=2, sort_keys=True), encoding="utf-8")


if __name__ == "__main__":
    main()
