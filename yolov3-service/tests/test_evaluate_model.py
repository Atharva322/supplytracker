import json
import os
import unittest

import evaluate_model


class EvaluationCommandTests(unittest.TestCase):
    def test_evaluates_labels_decisions_map_and_latency(self):
        rows = [
            {"id": "1", "expected_labels": ["apple"], "predicted_labels": ["apple"], "expected_decision": "APPROVE", "predicted_decision": "APPROVE", "confidence": 0.95, "latency_ms": 20},
            {"id": "2", "expected_labels": ["mango"], "predicted_labels": ["apple"], "expected_decision": "REJECT", "predicted_decision": "APPROVE", "confidence": 0.50, "latency_ms": 40},
            {"id": "3", "expected_labels": ["banana"], "predicted_labels": [], "expected_decision": "APPROVE", "predicted_decision": "REVIEW", "confidence": 0.20, "latency_ms": 80},
        ]

        report = evaluate_model.evaluate(rows)

        self.assertEqual(report["samples"], 3)
        self.assertAlmostEqual(report["labels"]["precision"], 0.5)
        self.assertAlmostEqual(report["labels"]["recall"], 1 / 3)
        self.assertEqual(report["decisions"]["false_accept"], 1)
        self.assertEqual(report["decisions"]["false_reject"], 1)
        self.assertEqual(report["latency"]["max_ms"], 80)
        self.assertIn("map", report)

    def test_command_writes_report_and_candidate_comparison(self):
        baseline = [
            {"id": "1", "expected_labels": ["apple"], "predicted_labels": ["apple"], "expected_decision": "APPROVE", "predicted_decision": "APPROVE", "confidence": 0.9, "latency_ms": 10}
        ]
        candidate = [
            {"id": "1", "expected_labels": ["apple"], "predicted_labels": ["banana"], "expected_decision": "APPROVE", "predicted_decision": "REVIEW", "confidence": 0.4, "latency_ms": 30}
        ]
        predictions = os.path.join(os.getcwd(), ".test_predictions.jsonl")
        candidate_path = os.path.join(os.getcwd(), ".test_candidate.jsonl")
        output = os.path.join(os.getcwd(), ".test_report.json")
        try:
            with open(predictions, "w", encoding="utf-8") as handle:
                handle.write(json.dumps(baseline[0]) + "\n")
            with open(candidate_path, "w", encoding="utf-8") as handle:
                handle.write(json.dumps(candidate[0]) + "\n")

            rows = evaluate_model.load_jsonl(predictions)
            candidate_rows = evaluate_model.load_jsonl(candidate_path)
            with open(output, "w", encoding="utf-8") as handle:
                json.dump(evaluate_model.evaluate(rows, candidate_rows), handle)

            with open(output, "r", encoding="utf-8") as handle:
                report = json.load(handle)
        finally:
            for path in (predictions, candidate_path, output):
                if os.path.exists(path):
                    os.remove(path)
        self.assertIn("candidate_comparison", report)
        self.assertEqual(report["candidate_comparison"]["baseline_f1"], 1.0)
        self.assertEqual(report["candidate_comparison"]["candidate_f1"], 0.0)


if __name__ == "__main__":
    unittest.main()
