"""The publish step: type mapping and transactional full refresh.

The ordering test is the one that matters: staging must be complete before the
published table is refreshed, and the published table must never be dropped.
"""

import pytest
from conftest import FakeWarehouse

from src.publishing import sync

COLUMNS = [
    ("external_event_key", "STRING"),
    ("external_event_id", "STRING"),
    ("source", "STRING"),
    ("title", "STRING"),
    ("start_at", "TIMESTAMP"),
    ("end_at", "TIMESTAMP"),
    ("occurrence_count", "BIGINT"),
    ("price_min", "DECIMAL(10,2)"),
    ("is_cancelled", "BOOLEAN"),
]
ROWS = [
    [
        "ticketmaster:a1",
        "a1",
        "ticketmaster",
        "Example event",
        "2026-09-01T18:00:00Z",
        None,
        1,
        25.00,
        False,
    ]
]


def test_ticketmaster_event_publish_defaults():
    assert sync.DEFAULT_MART == "fct_external_events"
    assert sync.DEFAULT_TABLE == "external_events"


class FakeCursor:
    def __init__(self, log: list[str]) -> None:
        self.log = log

    def execute(self, statement, params=None):
        # Statements are psycopg SQL objects now, not strings. as_string()
        # renders one the way the server will see it, which is what the
        # ordering assertions below read.
        self.log.append(" ".join(statement.as_string().split()))

    def executemany(self, statement, rows):
        self.log.append(f"INSERT x{len(list(rows))}")

    def __enter__(self):
        return self

    def __exit__(self, *_exc):
        return None


class FakeConnection:
    def __init__(self) -> None:
        self.log: list[str] = []
        self.committed = False
        self.closed = False

    def cursor(self):
        return FakeCursor(self.log)

    def commit(self):
        self.committed = True

    def close(self):
        self.closed = True


@pytest.fixture
def connection(monkeypatch) -> FakeConnection:
    fake = FakeConnection()
    monkeypatch.setattr(sync.psycopg, "connect", lambda *a, **k: fake)
    return fake


def test_type_mapping():
    assert sync.postgres_type("BIGINT") == "bigint"
    assert sync.postgres_type("DECIMAL(5,1)") == "numeric"
    assert sync.postgres_type("TIMESTAMP") == "timestamptz"


def test_unknown_type_becomes_text():
    """Keeping the value beats guessing at it. A column nobody thought about
    should not fail the run."""
    assert sync.postgres_type("ARRAY<STRING>") == "text"
    assert sync.postgres_type("MAP<STRING,INT>") == "text"


def index_of(statements: list[str], fragment: str) -> int:
    """Position of the first statement containing `fragment`.

    A named failure rather than a bare `next()`, so a test that breaks tells
    you which statement went missing instead of raising StopIteration.
    """
    for position, statement in enumerate(statements):
        if fragment in statement:
            return position
    raise AssertionError(f"no statement contained {fragment!r}: {statements}")


def test_publish_refreshes_existing_table_in_the_right_order(connection):
    count = sync.publish("dsn", "analytics", "external_events", COLUMNS, ROWS)
    assert count == 1

    statements = connection.log
    staging_created = index_of(statements, 'create table "analytics"."external_events__staging"')
    staged = index_of(statements, "INSERT x1")
    target_created = index_of(
        statements, 'create table if not exists "analytics"."external_events"'
    )
    truncated = index_of(statements, 'truncate table "analytics"."external_events"')
    refreshed = index_of(statements, 'insert into "analytics"."external_events"')
    staging_dropped = index_of(statements, 'drop table "analytics"."external_events__staging"')

    assert staging_created < staged < target_created < truncated < refreshed < staging_dropped
    assert connection.committed


def test_first_publish_works_with_no_existing_table(connection):
    """The target is created only when the first publish has no table yet."""
    sync.publish("dsn", "analytics", "external_events", COLUMNS, ROWS)
    create = connection.log[
        index_of(
            connection.log,
            'create table if not exists "analytics"."external_events"',
        )
    ]
    assert "if not exists" in create


def test_published_table_is_never_dropped_or_renamed(connection):
    """The backend view depends on this exact table object."""
    sync.publish("dsn", "analytics", "external_events", COLUMNS, ROWS)

    assert not any(
        'drop table if exists "analytics"."external_events"' in statement
        for statement in connection.log
    )
    assert not any("rename to" in statement for statement in connection.log)


def test_publishing_zero_rows_is_refused(connection):
    """An empty mart over a good table is a data loss incident."""
    with pytest.raises(ValueError, match="zero rows"):
        sync.publish("dsn", "analytics", "external_events", COLUMNS, [])
    assert connection.log == []


def test_reading_an_empty_mart_is_refused():
    warehouse = FakeWarehouse()
    with pytest.raises(ValueError, match="no rows"):
        sync.read_mart(warehouse, "main", "fct_external_events")


def test_the_source_schema_is_stamped_on_the_table(connection):
    """One shared `analytics_dev` means the last publish wins, which is right for
    a place two tracks meet but leaves nobody able to say why the columns changed.
    The comment names the warehouse schema the rows came from."""
    sync.publish("dsn", "analytics_dev", "external_events", COLUMNS, ROWS, source="team_a.dev_alex")

    comment = connection.log[index_of(connection.log, "comment on table")]
    assert '"analytics_dev"."external_events"' in comment
    assert "from team_a.dev_alex at " in comment


def test_the_stamp_lands_after_the_refresh(connection):
    """Stamp the published table only after all new rows are inserted."""
    sync.publish("dsn", "analytics_dev", "external_events", COLUMNS, ROWS, source="s")
    assert index_of(connection.log, 'insert into "analytics_dev"."external_events"') < index_of(
        connection.log, "comment on table"
    )


def test_no_source_means_no_comment(connection):
    """Callers that do not know where the rows came from should not write a
    misleading stamp, and an unstamped table is better than a wrong one."""
    sync.publish("dsn", "analytics", "external_events", COLUMNS, ROWS)
    assert not any("comment on table" in statement for statement in connection.log)
