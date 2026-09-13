"""Offline checks for optional Health Page failure handling.

Load only the three functions under test, without starting Streamlit or Azure.
The function bodies come directly from app.py; external services are mocked.
"""

import ast
import logging
import unittest
from datetime import UTC, datetime
from itertools import pairwise
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import MagicMock
from uuid import UUID


class DatabaseError(Exception):
    """Simulate a database read failure without connecting to PostgreSQL."""


class HealthPageTests(unittest.TestCase):
    def test_optional_metrics_do_not_hide_event_data(self):
        app_path = Path(__file__).with_name("app.py")
        tree = ast.parse(app_path.read_text())
        names = {"load_event_stats", "load_processing_metrics", "render_dashboard"}
        functions = [
            node for node in tree.body if isinstance(node, ast.FunctionDef) and node.name in names
        ]
        self.assertEqual({node.name for node in functions}, names)
        for node in functions:
            node.decorator_list = []
        module = ast.Module(body=functions, type_ignores=[])
        code = compile(ast.fix_missing_locations(module), str(app_path), "exec")

        publication_id = "9f91a68c-a403-4d73-948c-a69dda623331"
        good_metrics = {
            "source_records": "2218",
            "after_date_status_checks": "1772",
            "after_parking_removed": "1623",
            "after_ticket_extras_removed": "1444",
            "grouped_event_cards": "897",
            "represented_records": "1444",
        }
        scenarios = (
            "valid",
            "missing_row",
            "missing_table",
            "wrong_publication",
            "wrong_event_count",
            "wrong_record_count",
        )
        for scenario in scenarios:
            with self.subTest(scenario=scenario):
                self.check_scenario(code, publication_id, good_metrics, scenario)

    def check_scenario(self, code, publication_id, good_metrics, scenario):
        metrics = dict(good_metrics)
        if scenario == "wrong_event_count":
            metrics["grouped_event_cards"] = "896"
        if scenario == "wrong_record_count":
            metrics["represented_records"] = "1443"
        marker_id = publication_id
        if scenario == "wrong_publication":
            marker_id = "00000000-0000-0000-0000-000000000001"

        connection = MagicMock()
        connection.__enter__.return_value = connection
        marker = (f"from dev; publication_id={marker_id}",)
        result = None if scenario == "missing_row" else (publication_id, metrics)
        connection.execute.return_value.fetchone.side_effect = (
            [marker, DatabaseError("table unavailable")]
            if scenario == "missing_table"
            else [marker, result]
        )
        event_data = {
            "source_occurrences": 1444,
            "total_events": 943,
            "current_events": 897,
            "retained_events": 46,
            "categorized_events": 897,
            "multiple_category_events": 104,
            "known_prices": 890,
            "unknown_prices": 7,
            "indoor_events": 773,
            "outdoor_events": 17,
            "mixed_events": 0,
            "unknown_venue_events": 107,
        }
        cursor = connection.cursor.return_value.__enter__.return_value
        cursor.description = [SimpleNamespace(name=key) for key in event_data]
        cursor.fetchone.return_value = tuple(event_data.values())

        st = MagicMock()
        st.button.return_value = False
        column = MagicMock()
        st.columns.side_effect = lambda spec: [column] * (
            spec if isinstance(spec, int) else len(spec)
        )
        namespace = {
            "psycopg": SimpleNamespace(Connection=object, Error=DatabaseError),
            "sql": MagicMock(),
            "UUID": UUID,
            "pairwise": pairwise,
            "SCHEMA": "analytics_dev",
            "logger": MagicMock(spec=logging.Logger),
            "postgres_connection": MagicMock(return_value=connection),
            "load_category_distribution": MagicMock(return_value=[("Music", 471)]),
            "st": st,
            "alt": MagicMock(),
            "LOC_CATEGORIES": ("Music",),
            "load_file_stats": MagicMock(return_value=(1, datetime.now(UTC))),
            "age": MagicMock(return_value="1 h ago"),
            "AIRFLOW_URL": "",
            "datetime": datetime,
            "UTC": UTC,
        }
        # Execute only selected function definitions from the trusted local app.py.
        exec(code, namespace)  # noqa: S102
        stats = namespace["load_event_stats"]()
        for key, value in event_data.items():
            self.assertEqual(stats[key], value)
        self.assertEqual(stats["category_distribution"], [("Music", 471)])
        if scenario == "valid":
            self.assertEqual(stats["processing_metrics"]["grouped_event_cards"], 897)
        else:
            self.assertIsNone(stats["processing_metrics"])
            namespace["logger"].exception.assert_called_once()

        # Render the returned snapshot: check the warning and unaffected metrics.
        namespace["load_event_stats"] = MagicMock(return_value=stats)
        namespace["render_dashboard"]()
        st.error.assert_not_called()
        if scenario == "valid":
            st.warning.assert_not_called()
        else:
            st.warning.assert_called_once()
            self.assertIn("Processing figures are unavailable", st.warning.call_args.args[0])
        column.metric.assert_any_call("Current events", "897")
        column.metric.assert_any_call("Kept for Saved / Going", "46")
        column.metric.assert_any_call("Known", "890")
        column.metric.assert_any_call("Known", "790")


if __name__ == "__main__":
    unittest.main()
