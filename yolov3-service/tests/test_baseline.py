import asyncio
import os
import unittest

import app


class BaselineServiceTests(unittest.TestCase):
    def setUp(self):
        self.original_net = app.net
        self.original_classes = app.class_names

    def tearDown(self):
        app.net = self.original_net
        app.class_names = self.original_classes

    def test_default_model_paths_are_repository_portable(self):
        self.assertEqual(os.path.dirname(app.CFG_PATH), app.MODEL_DIR)
        self.assertEqual(os.path.dirname(app.WEIGHTS_PATH), app.MODEL_DIR)

    def test_health_contract_reports_model_not_loaded(self):
        app.net = None
        app.class_names = []
        response = asyncio.run(app.health_check())
        self.assertEqual(response["status"], "model_not_loaded")
        self.assertFalse(response["ready"])
        self.assertEqual(response["total_classes"], 0)

    def test_root_contract_lists_operational_endpoints(self):
        app.net = None
        response = asyncio.run(app.root())
        self.assertEqual(response["service"], "YOLOv3 Detection Service")
        self.assertIn("/health", response["endpoints"])
        self.assertIn("/detect", response["endpoints"])


if __name__ == "__main__":
    unittest.main()
