"""Publish a mart from Databricks into the backend's Postgres database.

Airflow runs this after dbt succeeds, and you run it by hand with
`uv run --extra sync python -m src.publishing.sync`. Both go through `run()` below, so the
scheduled publish and your own use one connection string and one set of
defaults rather than two that drift.

See the README, "The two schemas", for how development and production targets
stay separated.
"""

import argparse
import json
import logging
import os
import sys
from datetime import UTC, datetime
from typing import LiteralString
from uuid import UUID, uuid4

import psycopg
from psycopg.sql import SQL, Identifier, Literal, Placeholder

from ..common.warehouse import Queryable, Warehouse
from . import health_metrics

logger = logging.getLogger(__name__)

DEFAULT_MART = "fct_external_events_enriched"
DEFAULT_TABLE = "external_events"
UUID_COLUMNS = {"logical_event_id"}

# What a Databricks column becomes in Postgres. Anything not listed becomes
# text: keeping the value beats guessing at it.
TYPE_MAP: dict[str, LiteralString] = {
    "BIGINT": "bigint",
    "INT": "integer",
    "SMALLINT": "smallint",
    "DOUBLE": "double precision",
    "FLOAT": "double precision",
    "DECIMAL": "numeric",
    "BOOLEAN": "boolean",
    "DATE": "date",
    "TIMESTAMP": "timestamptz",
    "TIMESTAMP_NTZ": "timestamp",
}


def postgres_type(
    databricks_type: str,
    column_name: str | None = None,
) -> LiteralString:
    """Translate one Databricks column type into its Postgres type."""
    if column_name in UUID_COLUMNS:
        return "uuid"

    normalized = databricks_type.upper().replace(" ", "")
    if normalized == "ARRAY<STRING>":
        return "text[]"
    return TYPE_MAP.get(normalized.split("(")[0], "text")


def postgres_value(
    value,
    databricks_type: str,
    column_name: str | None = None,
):
    """Convert warehouse values that need native Postgres representations."""
    if column_name in UUID_COLUMNS:
        return None if value is None else UUID(str(value))

    normalized = databricks_type.upper().replace(" ", "")
    if normalized != "ARRAY<STRING>" or value is None or isinstance(value, list):
        return value

    if isinstance(value, str):
        parsed = json.loads(value)
        if isinstance(parsed, list) and all(isinstance(item, str) for item in parsed):
            return parsed

    raise ValueError(f"expected ARRAY<STRING> value, received {value!r}")


def read_mart(
    warehouse: Queryable, schema: str, table: str
) -> tuple[list[tuple[str, str]], list[list]]:
    """Read a whole published table out of the warehouse, with its columns."""
    qualified = f"{warehouse.catalog}.{schema}.{table}"
    columns, rows = warehouse.query(f"select * from {qualified}")
    logger.info("read %d rows and %d columns from %s", len(rows), len(columns), qualified)
    if not rows:
        raise ValueError(f"{qualified} returned no rows: refusing to publish an empty mart")
    return columns, rows


def read_backend_table(dsn: str, table: str, schema: str = "app") -> list[dict]:
    """Read one of the backend's own tables.

    `analytics_user` has read and nothing else on the `app` schema, so the
    worst a mistake here can do is return the wrong rows.
    """
    statement = SQL("select * from {}").format(Identifier(schema, table))
    with psycopg.connect(dsn) as connection, connection.cursor() as cursor:
        cursor.execute(statement)
        names = [column.name for column in cursor.description or []]
        rows = [dict(zip(names, row, strict=True)) for row in cursor.fetchall()]
    logger.info("read %d rows from %s.%s", len(rows), schema, table)
    return rows


def publish(
    dsn: str,
    schema: str,
    table: str,
    columns: list[tuple[str, str]],
    rows: list[list],
    source: str | None = None,
    publication_id: str | None = None,
) -> int:
    """Refresh the backend's table in place, return the total row count written.

    Load the current mart into staging and retain previously published external
    events that still have active Saved or Going references. Then truncate and
    refill the published table inside one transaction. Keeping the published
    table itself preserves backend views, grants, and indexes. Rows absent from
    the current mart are removed unless the backend still references them.

    `source` is the warehouse schema the rows came from, and it is recorded as a
    comment on the table. One shared `analytics_dev` means the last publish wins,
    while the comment records which warehouse schema supplied the latest data.
    """
    if not rows:
        raise ValueError("refusing to publish zero rows over an existing table")

    if publication_id is not None:
        publication_id = str(UUID(publication_id))

    # Names are composed with psycopg's SQL objects, not pasted into an
    # f-string: a table name cannot be a query parameter.
    staging = Identifier(schema, f"{table}__staging")
    published = Identifier(schema, table)
    definition = SQL(", ").join(
        SQL("{} {}").format(
            Identifier(name),
            SQL(postgres_type(type_text, name)),
        )
        for name, type_text in columns
    )
    column_names = SQL(", ").join(Identifier(name) for name, _ in columns)
    available_columns = {name for name, _ in columns}
    retention_columns = {
        "logical_event_id",
        "source",
        "source_url",
        "external_event_id",
        "external_venue_id",
        "start_date",
        "is_published",
    }
    retained_values = SQL(", ").join(
        SQL("false") if name == "is_published" else SQL("previous.{}").format(Identifier(name))
        for name, _ in columns
    )
    retained_count = 0

    prepared_rows = [
        [
            postgres_value(value, type_text, name)
            for value, (name, type_text) in zip(row, columns, strict=True)
        ]
        for row in rows
    ]

    connection = psycopg.connect(dsn, autocommit=False)
    try:
        with connection.cursor() as cursor:
            cursor.execute(SQL("drop table if exists {}").format(staging))
            cursor.execute(SQL("create table {} ({})").format(staging, definition))
            cursor.executemany(
                SQL("insert into {} ({}) values ({})").format(
                    staging,
                    column_names,
                    SQL(", ").join([Placeholder()] * len(columns)),
                ),
                prepared_rows,
            )
            # Create only on the first publish. Later runs preserve this table
            # so backend views, grants, and indexes remain attached to it.
            cursor.execute(SQL("create table if not exists {} ({})").format(published, definition))
            if any(name == "is_published" for name, _ in columns):
                cursor.execute(
                    SQL(
                        "alter table {} add column if not exists {} "
                        "boolean not null default true"
                    ).format(published, Identifier("is_published"))
                )
                cursor.execute(
                    SQL("alter table {} alter column {} set default true").format(
                        published, Identifier("is_published")
                    )
                )
                cursor.execute(
                    SQL("alter table {} alter column {} set not null").format(
                        published, Identifier("is_published")
                    )
                )
            if table == DEFAULT_TABLE and {"category", "categories"} <= available_columns:
                cursor.execute(
                    SQL("alter table {} add column if not exists {} text[]").format(
                        published, Identifier("categories")
                    )
                )
                cursor.execute(
                    SQL("update {} set {} = array[{}] where {} is null").format(
                        published,
                        Identifier("categories"),
                        Identifier("category"),
                        Identifier("categories"),
                    )
                )
                cursor.execute(
                    SQL("alter table {} alter column {} set not null").format(
                        published, Identifier("categories")
                    )
                )
            if table == DEFAULT_TABLE and "logical_event_id" in available_columns:
                cursor.execute(
                    SQL("alter table {} add column if not exists {} uuid").format(
                        published,
                        Identifier("logical_event_id"),
                    )
                )
                cursor.execute(
                    SQL(
                        """
                        update {}
                        set {} = app.canonical_event_uuid(
                            app.build_stable_key(
                                source,
                                source_url,
                                external_event_id,
                                external_venue_id,
                                start_date
                            )
                        )
                        where {} is null
                        """
                    ).format(
                        published,
                        Identifier("logical_event_id"),
                        Identifier("logical_event_id"),
                    )
                )
                cursor.execute(
                    SQL("alter table {} alter column {} set not null").format(
                        published,
                        Identifier("logical_event_id"),
                    )
                )

            if table == DEFAULT_TABLE and "venue_setting" in available_columns:
                cursor.execute(
                    SQL("alter table {} add column if not exists {} text").format(
                        published,
                        Identifier("venue_setting"),
                    )
                )
                cursor.execute(
                    SQL("update {} set {} = 'unknown' where {} is null").format(
                        published,
                        Identifier("venue_setting"),
                        Identifier("venue_setting"),
                    )
                )
                cursor.execute(
                    SQL("alter table {} alter column {} set not null").format(
                        published,
                        Identifier("venue_setting"),
                    )
                )

            # Carry forward the complete card data for events that disappeared
            # from the current mart but still have active Saved or Going
            # references. Current mart rows always take precedence.
            if table == DEFAULT_TABLE and retention_columns <= available_columns:
                cursor.execute(
                    SQL(
                        """
                        insert into {} ({})
                        select {}
                        from {} as previous
                        where exists (
                            select 1
                            from app.event_registry as registry
                            where registry.id = previous.logical_event_id
                            and (
                                exists (
                                    select 1
                                    from app.saved_events as saved
                                    where saved.event_id = registry.id
                                )
                                or exists (
                                    select 1
                                    from app.event_attendees as attendee
                                    where attendee.event_id = registry.id
                                )
                            )
                        )
                        and not exists (
                            select 1
                            from {} as current
                            where current.logical_event_id = previous.logical_event_id
                        )
                        """
                    ).format(
                        staging,
                        column_names,
                        retained_values,
                        published,
                        staging,
                    )
                )
                retained_count = cursor.rowcount
            cursor.execute(SQL("truncate table {}").format(published))
            cursor.execute(
                SQL("insert into {} ({}) select {} from {}").format(
                    published, column_names, column_names, staging
                )
            )
            cursor.execute(SQL("drop table {}").format(staging))
            if source or publication_id:
                stamp = datetime.now(tz=UTC).strftime("%Y-%m-%dT%H:%MZ")
                comment = f"from {source or 'unspecified'} at {stamp}"
                if publication_id:
                    comment += f"; publication_id={publication_id}"

                cursor.execute(
                    SQL("comment on table {} is {}").format(
                        published,
                        Literal(comment),
                    )
                )
        connection.commit()
    finally:
        connection.close()

    published_count = len(rows) + retained_count
    logger.info(
        "published %d rows to %s.%s: %d current, %d retained",
        published_count,
        schema,
        table,
        len(rows),
        retained_count,
    )
    return published_count


def dsn_from_env() -> str:
    """The connection string, built from the environment both callers share."""
    missing = [name for name in ("BACKEND_PG_HOST", "BACKEND_PG_DB") if not os.environ.get(name)]
    if missing:
        raise RuntimeError(f"{', '.join(missing)} not set. See data/.env.example.")

    return (
        f"host={os.environ['BACKEND_PG_HOST']} "
        f"port={os.environ.get('BACKEND_PG_PORT', '5432')} "
        f"dbname={os.environ['BACKEND_PG_DB']} "
        f"user={os.environ.get('BACKEND_PG_USER', 'analytics_user')} "
        f"password={os.environ.get('BACKEND_PG_PASSWORD', '')} "
        f"sslmode={os.environ.get('BACKEND_PG_SSLMODE', 'require')}"
    )


def run(
    mart: str = DEFAULT_MART,
    table: str = DEFAULT_TABLE,
    schema: str | None = None,
) -> int:
    """Publish events first, then attempt optional processing metrics."""
    warehouse_schema = os.environ["DBT_SCHEMA"]
    warehouse = Warehouse.from_env()
    columns, rows = read_mart(warehouse, warehouse_schema, mart)
    target_schema = schema or os.environ.get("BACKEND_PG_PUBLISH_SCHEMA", "analytics")
    publication_id = str(uuid4())
    dsn = dsn_from_env()

    published_count = publish(
        dsn,
        target_schema,
        table,
        columns,
        rows,
        source=warehouse_schema,
        publication_id=publication_id,
    )

    # publish() has committed the events before optional work starts.
    if mart == DEFAULT_MART and table == DEFAULT_TABLE:
        try:
            metrics = health_metrics.read_optional_metrics(
                warehouse,
                warehouse_schema,
                columns,
                rows,
                available=os.getenv("HEALTH_METRICS_AVAILABLE", "").lower() == "true",
                build_id=os.getenv("HEALTH_METRICS_BUILD_ID", ""),
            )
            metrics_written = health_metrics.write_optional_metrics(
                dsn,
                target_schema,
                table,
                publication_id,
                metrics,
            )
        except Exception:
            logger.exception("Unexpected failure in optional Health Page metrics")
            metrics_written = False

        if not metrics_written:
            logger.warning(
                "Events were published, but processing metrics are unavailable "
                "for publication %s",
                publication_id,
            )

    return published_count


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s %(message)s")

    parser = argparse.ArgumentParser(description="Publish one mart to the backend's database.")
    parser.add_argument(
        "--mart", default=DEFAULT_MART, help=f"warehouse table to read [{DEFAULT_MART}]"
    )
    parser.add_argument(
        "--table", default=DEFAULT_TABLE, help=f"name to write it under [{DEFAULT_TABLE}]"
    )
    parser.add_argument("--schema", default=None, help="target schema [BACKEND_PG_PUBLISH_SCHEMA]")
    args = parser.parse_args()

    try:
        run(args.mart, args.table, args.schema)
    except Exception:
        logger.exception("Publish failed")
        sys.exit(1)
