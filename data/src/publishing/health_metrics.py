"""Validate optional processing metrics against the events being published."""

import logging
from hashlib import sha256
from itertools import pairwise
from uuid import UUID

import psycopg
from psycopg.sql import SQL, Identifier
from psycopg.types.json import Jsonb

from src.common.warehouse import Queryable

logger = logging.getLogger(__name__)


def count_value(value: object) -> int:
    """Accept non-negative integer counts from warehouse results."""
    if isinstance(value, bool) or not isinstance(value, (int, str)):
        raise TypeError("Expected an integer count")

    if isinstance(value, str) and not value.isdecimal():
        raise ValueError("Expected a non-negative integer count")

    result = int(value)
    if result < 0:
        raise ValueError("Counts cannot be negative")
    return result


def event_summary(
    columns: list[tuple[str, str]],
    rows: list[list],
) -> tuple[int, int, str]:
    """Return card count, represented records and a stable fingerprint."""
    names = [name for name, _ in columns]
    id_index = names.index("logical_event_id")
    occurrence_index = names.index("occurrence_count")

    seen = set()
    entries = []
    represented = 0

    for row in rows:
        event_id = str(UUID(str(row[id_index])))
        occurrences = count_value(row[occurrence_index])

        if event_id in seen:
            raise ValueError("Duplicate logical event ID")
        if occurrences == 0:
            raise ValueError("Each event must represent at least one record")

        seen.add(event_id)
        represented += occurrences
        entries.append(f"{event_id}:{occurrences}")

    fingerprint = sha256("|".join(sorted(entries)).encode("utf-8")).hexdigest()
    return len(rows), represented, fingerprint


def validate_metrics(
    metrics: dict,
    columns: list[tuple[str, str]],
    rows: list[list],
) -> None:
    """Reject inconsistent metrics without deciding event publication policy."""
    stages = [
        count_value(metrics[name])
        for name in (
            "source_records",
            "after_date_status_checks",
            "after_parking_removed",
            "after_ticket_extras_removed",
            "grouped_event_cards",
        )
    ]

    if stages[-1] == 0 or any(before < after for before, after in pairwise(stages)):
        raise ValueError("Processing counts are inconsistent")

    cards, represented, fingerprint = event_summary(columns, rows)

    if stages[-1] != cards:
        raise ValueError("Metrics do not match the number of published events")

    if stages[-2] != represented or count_value(metrics["represented_records"]) != represented:
        raise ValueError("Metrics do not match represented records")

    if metrics["event_set_fingerprint"] != fingerprint:
        raise ValueError("Metrics belong to a different event set")


def read_optional_metrics(
    warehouse: Queryable,
    schema: str,
    event_columns: list[tuple[str, str]],
    event_rows: list[list],
    *,
    available: bool,
    build_id: str,
) -> dict | None:
    """Read only validated metrics from the expected build attempt."""
    if not available:
        logger.warning("Processing metrics were not built successfully; skipping them")
        return None

    try:
        expected_id = str(UUID(build_id))

        qualified = ".".join(
            "`" + part.replace("`", "``") + "`"
            for part in (
                warehouse.catalog,
                schema,
                "agg_event_processing_metrics",
            )
        )
        columns, rows = warehouse.query(f"select * from {qualified}")

        if len(rows) != 1:
            raise ValueError("Expected exactly one processing metrics row")

        metrics = dict(
            zip(
                [name for name, _ in columns],
                rows[0],
                strict=True,
            )
        )

        if str(UUID(str(metrics["metrics_build_id"]))) != expected_id:
            raise ValueError("Processing metrics belong to another build attempt")

        validate_metrics(metrics, event_columns, event_rows)
        return metrics

    except Exception:
        logger.exception(
            "Processing metrics are unavailable or invalid; " "event publication may continue"
        )
        return None


def write_optional_metrics(
    dsn: str,
    schema: str,
    event_table: str,
    publication_id: str,
    metrics: dict | None,
) -> bool:
    """Write optional metrics only while the matching publication is current."""
    if metrics is None:
        return False

    try:
        expected_id = str(UUID(publication_id))
        published = Identifier(schema, event_table)
        metrics_table = Identifier(schema, "event_processing_metrics")

        with psycopg.connect(dsn) as connection, connection.cursor() as cursor:
            cursor.execute(SQL("set local lock_timeout = '5s'"))
            cursor.execute(SQL("set local statement_timeout = '15s'"))

            # Prevent the event table from being replaced between checking
            # its publication marker and saving the matching metrics.
            cursor.execute(SQL("lock table {} in access share mode").format(published))
            cursor.execute(
                SQL("select obj_description(to_regclass(%s), 'pg_class')"),
                (published.as_string(),),
            )
            row = cursor.fetchone()
            comment = row[0] if row else None

            if not isinstance(comment, str) or not comment.endswith(
                f"; publication_id={expected_id}"
            ):
                raise ValueError("The current event publication has changed")

            cursor.execute(
                SQL(
                    """
                        create table if not exists {} (
                            event_table text primary key,
                            publication_id uuid not null,
                            metrics jsonb not null,
                            recorded_at timestamptz not null
                                default current_timestamp
                        )
                        """
                ).format(metrics_table)
            )

            cursor.execute(
                SQL(
                    """
                        insert into {} (
                            event_table, publication_id, metrics
                        )
                        values (%s, %s, %s)
                        on conflict (event_table) do update
                        set
                            publication_id = excluded.publication_id,
                            metrics = excluded.metrics,
                            recorded_at = current_timestamp
                        """
                ).format(metrics_table),
                (event_table, UUID(expected_id), Jsonb(metrics)),
            )

        return True

    except Exception:
        logger.exception(
            "Could not publish Health Page processing metrics; "
            "the event publication is not rolled back"
        )
        return False
