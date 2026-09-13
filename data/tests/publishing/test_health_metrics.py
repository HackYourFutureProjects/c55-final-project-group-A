from hashlib import sha256
from unittest.mock import MagicMock

import pytest

from src.publishing import health_metrics
from src.publishing.health_metrics import event_summary, read_optional_metrics, validate_metrics

COLUMNS = [
    ("logical_event_id", "STRING"),
    ("occurrence_count", "BIGINT"),
]
FIRST_ID = "00000000-0000-0000-0000-000000000001"
SECOND_ID = "00000000-0000-0000-0000-000000000002"
ROWS = [[FIRST_ID, 2], [SECOND_ID, 1]]
BUILD_ID = "00000000-0000-0000-0000-000000000010"


class MetricsWarehouse:
    catalog = "team_a"

    def __init__(self, metrics: dict) -> None:
        self.metrics = metrics
        self.calls = 0

    def query(self, statement: str) -> tuple[list[tuple[str, str]], list[list]]:
        self.calls += 1
        return (
            [(name, "STRING") for name in self.metrics],
            [list(self.metrics.values())],
        )

    def run(self, statement: str) -> list[list]:
        return self.query(statement)[1]


def test_failed_build_does_not_read_old_metrics():
    warehouse = MetricsWarehouse(valid_metrics())

    result = read_optional_metrics(
        warehouse,
        "dev_test",
        COLUMNS,
        ROWS,
        available=False,
        build_id=BUILD_ID,
    )

    assert result is None
    assert warehouse.calls == 0


def test_metrics_from_another_build_are_rejected():
    metrics = {
        **valid_metrics(),
        "metrics_build_id": "00000000-0000-0000-0000-000000000011",
    }

    result = read_optional_metrics(
        MetricsWarehouse(metrics),
        "dev_test",
        COLUMNS,
        ROWS,
        available=True,
        build_id=BUILD_ID,
    )

    assert result is None


def test_matching_build_and_events_are_accepted():
    metrics = {**valid_metrics(), "metrics_build_id": BUILD_ID}

    result = read_optional_metrics(
        MetricsWarehouse(metrics),
        "dev_test",
        COLUMNS,
        ROWS,
        available=True,
        build_id=BUILD_ID,
    )

    assert result == metrics


def test_warehouse_failure_does_not_propagate():
    class UnavailableWarehouse:
        catalog = "team_a"

        def query(self, statement: str) -> tuple[list[tuple[str, str]], list[list]]:
            raise RuntimeError("Warehouse unavailable")

        def run(self, statement: str) -> list[list]:
            return self.query(statement)[1]

    result = read_optional_metrics(
        UnavailableWarehouse(),
        "dev_test",
        COLUMNS,
        ROWS,
        available=True,
        build_id=BUILD_ID,
    )

    assert result is None


def valid_metrics():
    return {
        "source_records": 6,
        "after_date_status_checks": 5,
        "after_parking_removed": 4,
        "after_ticket_extras_removed": 3,
        "grouped_event_cards": 2,
        "represented_records": 3,
        "event_set_fingerprint": sha256(f"{FIRST_ID}:2|{SECOND_ID}:1".encode()).hexdigest(),
    }


def test_matching_metrics_are_accepted():
    validate_metrics(valid_metrics(), COLUMNS, ROWS)


def test_row_order_does_not_change_fingerprint():
    assert event_summary(COLUMNS, ROWS) == event_summary(COLUMNS, ROWS[::-1])


def test_same_count_but_different_events_are_rejected():
    different_rows = [
        [FIRST_ID, 2],
        ["00000000-0000-0000-0000-000000000003", 1],
    ]
    with pytest.raises(ValueError, match="different event set"):
        validate_metrics(valid_metrics(), COLUMNS, different_rows)


def test_changed_occurrence_distribution_is_rejected():
    different_rows = [[FIRST_ID, 1], [SECOND_ID, 2]]
    with pytest.raises(ValueError, match="different event set"):
        validate_metrics(valid_metrics(), COLUMNS, different_rows)


def test_inconsistent_stages_are_rejected():
    metrics = valid_metrics()
    metrics["after_parking_removed"] = 7

    with pytest.raises(ValueError, match="Processing counts"):
        validate_metrics(metrics, COLUMNS, ROWS)


def test_duplicate_ids_are_rejected():
    with pytest.raises(ValueError, match="Duplicate"):
        event_summary(COLUMNS, [[FIRST_ID, 2], [FIRST_ID, 1]])


def test_string_counts_are_supported():
    validate_metrics(
        valid_metrics(),
        COLUMNS,
        [[FIRST_ID, "2"], [SECOND_ID, "1"]],
    )


def metrics_connection(monkeypatch, comment):
    connection = MagicMock()
    cursor = MagicMock()
    connection.__enter__.return_value = connection
    connection.cursor.return_value.__enter__.return_value = cursor
    cursor.fetchone.return_value = (comment,)

    connect = MagicMock(return_value=connection)
    monkeypatch.setattr(health_metrics.psycopg, "connect", connect)
    return connection, cursor, connect


def test_missing_metrics_do_not_open_a_connection(monkeypatch):
    _, _, connect = metrics_connection(monkeypatch, None)

    result = health_metrics.write_optional_metrics(
        "dsn", "analytics", "external_events", BUILD_ID, None
    )

    assert result is False
    connect.assert_not_called()


def test_matching_publication_writes_metrics(monkeypatch):
    connection, cursor, _ = metrics_connection(
        monkeypatch,
        f"from analytics at example; publication_id={BUILD_ID}",
    )

    result = health_metrics.write_optional_metrics(
        "dsn", "analytics", "external_events", BUILD_ID, valid_metrics()
    )

    assert result is True
    statements = [call.args[0].as_string() for call in cursor.execute.call_args_list]
    assert any("lock table" in statement for statement in statements)
    assert any("insert into" in statement for statement in statements)
    assert connection.__exit__.call_args.args == (None, None, None)


def test_changed_publication_does_not_write_metrics(monkeypatch):
    _, cursor, _ = metrics_connection(
        monkeypatch,
        "from analytics at example; publication_id=" "00000000-0000-0000-0000-000000000099",
    )

    result = health_metrics.write_optional_metrics(
        "dsn", "analytics", "external_events", BUILD_ID, valid_metrics()
    )

    assert result is False
    statements = [call.args[0].as_string() for call in cursor.execute.call_args_list]
    assert not any("insert into" in statement for statement in statements)


def test_metrics_connection_failure_does_not_propagate(monkeypatch):
    monkeypatch.setattr(
        health_metrics.psycopg,
        "connect",
        MagicMock(side_effect=RuntimeError("Connection unavailable")),
    )

    assert (
        health_metrics.write_optional_metrics(
            "dsn", "analytics", "external_events", BUILD_ID, valid_metrics()
        )
        is False
    )
