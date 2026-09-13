# OPTIONAL. A read-only health page for the data pipeline.
"""Show whether Team A's event pipeline is producing fresh, usable data."""

import logging
import os
from datetime import UTC, datetime
from itertools import pairwise
from uuid import UUID

import altair as alt
import psycopg
import streamlit as st
from azure.identity import DefaultAzureCredential, ManagedIdentityCredential
from azure.keyvault.secrets import SecretClient
from azure.storage.blob import BlobServiceClient
from dotenv import load_dotenv
from psycopg import sql

load_dotenv()

logger = logging.getLogger(__name__)

TEAM = os.getenv("TEAM", "team-a")
SCHEMA = os.getenv("SCHEMA", os.getenv("BACKEND_PG_PUBLISH_SCHEMA", "analytics"))
STORAGE_ACCOUNT = os.getenv("STORAGE_ACCOUNT", "")
STORAGE_CONTAINER = os.getenv("STORAGE_CONTAINER", "prod")
AIRFLOW_URL = os.getenv("AIRFLOW_URL", "").rstrip("/")
KEY_VAULT_URL = os.getenv("KEY_VAULT_URL", "https://kv-hyf-data.vault.azure.net")
LOC_CATEGORIES = (
    "Music",
    "Arts & Culture",
    "Theatre & Performance",
    "Family & Kids",
    "Community & Social",
    "Sports & Fitness",
    "Food & Drink",
    "Other",
)

st.set_page_config(
    page_title="Loc — Event data health",
    page_icon="📍",
    layout="wide",
)

# Static presentation only: no database values are interpolated into this HTML.
# Streamlit layout selectors may need revisiting after a Streamlit upgrade.
st.markdown(
    """
    <style>
    .stMainBlockContainer {
        max-width: 1160px;
        padding-top: 5rem;
        padding-bottom: 2rem;
    }
    [data-testid="stVerticalBlock"] { gap: 0.65rem; }
    [data-testid="stHeadingWithActionElements"] h3 {
        font-size: 1.4rem;
        padding-top: 0.25rem;
        padding-bottom: 0.25rem;
    }
    [data-testid="stMetricValue"] {
        font-size: 2rem;
        font-weight: 650;
    }
    [data-testid="stMarkdownContainer"] hr { margin: 0.7rem 0; }
    .loc-header { display: flex; align-items: center; gap: 18px; }
    .loc-brand { display: flex; align-items: center; gap: 8px; }
    .loc-pin {
        width: 42px; height: 42px; border-radius: 10px;
        background: #ea580c; display: flex;
        align-items: center; justify-content: center; font-size: 24px;
    }
    .loc-word { font-size: 22px; font-weight: 750; }
    .loc-header .loc-title {
        margin: 0 !important; padding: 0 !important;
        font-size: 28px !important; line-height: 1.2 !important;
        font-weight: 700;
    }
    .loc-subtitle { margin: 5px 0 0; color: #64748b; font-size: 15px; }
    .loc-steps {
        list-style: none; padding: 0; margin: 22px 0 12px;
        display: flex !important; flex-direction: row !important;
        flex-wrap: nowrap !important; width: 100%;
    }
    .loc-steps .loc-step {
        flex: 1 1 0; min-width: 0; position: relative;
        text-align: center; font-size: 14px;
    }
    .loc-steps .loc-step:not(:last-child)::after {
        content: ""; position: absolute; top: 16px;
        left: calc(50% + 18px); width: calc(100% - 36px);
        height: 1px; background: #cbd5e1;
    }
    .loc-step-number {
        width: 32px; height: 32px; border-radius: 50%;
        display: flex; align-items: center; justify-content: center;
        margin: 0 auto 7px; background: #fff3e8;
        color: #9a3412; border: 1px solid #fed7aa; font-weight: 650;
    }
    @media (max-width: 600px) {
        .loc-header { align-items: flex-start; gap: 12px; }
        .loc-header .loc-title { font-size: 23px !important; }
        .loc-word { display: none; }
        .loc-steps .loc-step { font-size: 12px; padding: 0 3px; }
        [data-testid="stMetricValue"] { font-size: 1.7rem; }
    }
    .loc-header {
    display: grid;
    grid-template-columns: 120px minmax(0, 1fr) 120px;
    align-items: center;
    gap: 12px;
    }

    .loc-header > div:last-child {
        text-align: center;
        min-width: 0;
    }

    @media (max-width: 600px) {
        .loc-header {
            grid-template-columns: 46px minmax(0, 1fr) 46px;
            gap: 8px;
        }
    }
    </style>
    <div class="loc-header">
        <div class="loc-brand" aria-label="Loc">
            <span class="loc-pin" aria-hidden="true">📍</span>
            <span class="loc-word">Loc</span>
        </div>
        <div>
            <h1 class="loc-title">Event data health</h1>
            <p class="loc-subtitle">External event source: Ticketmaster</p>
        </div>
    </div>
    <div class="loc-steps" role="list" aria-label="How the data is prepared">
        <div class="loc-step" role="listitem"><span class="loc-step-number">1</span>Source data</div>
        <div class="loc-step" role="listitem"><span class="loc-step-number">2</span>Filtering &amp; grouping</div>
        <div class="loc-step" role="listitem"><span class="loc-step-number">3</span>Enrichment</div>
        <div class="loc-step" role="listitem"><span class="loc-step-number">4</span>Ready for the app</div>
    </div>
    """,
    unsafe_allow_html=True,
)


@st.cache_resource
def azure_credential():
    """Use the app identity in Azure and Azure CLI credentials locally."""

    client_id = os.getenv("AZURE_CLIENT_ID")
    if client_id:
        return ManagedIdentityCredential(client_id=client_id)
    return DefaultAzureCredential()


@st.cache_resource
def secret_client() -> SecretClient:
    return SecretClient(vault_url=KEY_VAULT_URL, credential=azure_credential())


def postgres_connection():
    """Connect directly during local development or through Key Vault in Azure."""

    direct_settings = {
        "host": os.getenv("BACKEND_PG_HOST"),
        "dbname": os.getenv("BACKEND_PG_DB"),
        "user": os.getenv("BACKEND_PG_USER"),
        "password": os.getenv("BACKEND_PG_PASSWORD"),
    }
    if all(direct_settings.values()):
        return psycopg.connect(
            **direct_settings,
            port=os.getenv("BACKEND_PG_PORT", "5432"),
        )

    password = secret_client().get_secret(f"fp-smoke-pg-reader-{TEAM}").value
    return psycopg.connect(
        host=os.environ["PG_HOST"],
        port=os.getenv("PG_PORT", "5432"),
        dbname=os.environ["PG_DB"],
        user="analytics_reader",
        password=password,
        sslmode="require",
    )


@st.cache_data(ttl=60)
def load_event_stats() -> dict[str, object]:
    query = sql.SQL(
        """
        select
            coalesce(
                sum(occurrence_count) filter (where is_published is true),
                0
            ) as source_occurrences,
            count(*) as total_events,
            count(*) filter (where is_published is true) as current_events,
            count(*) filter (where is_published is false) as retained_events,
            count(*) filter (
                where is_published is true
                    and coalesce(cardinality(categories), 0) > 0
            ) as categorized_events,
            count(*) filter (
                where is_published is true
                    and coalesce(cardinality(categories), 0) > 1
            ) as multiple_category_events,
            count(*) filter (
                where is_published is true and is_price_known is true
            ) as known_prices,
            count(*) filter (
                where is_published is true and is_price_known is false
            ) as unknown_prices,
                        count(*) filter (
                where is_published is true and venue_setting = 'indoor'
            ) as indoor_events,
            count(*) filter (
                where is_published is true and venue_setting = 'outdoor'
            ) as outdoor_events,
            count(*) filter (
                where is_published is true and venue_setting = 'mixed'
            ) as mixed_events,
            count(*) filter (
                where is_published is true and venue_setting = 'unknown'
            ) as unknown_venue_events,
            max(ingested_at) filter (where is_published is true) as last_ingested
        from {}.{}
        """
    ).format(sql.Identifier(SCHEMA), sql.Identifier("external_events"))

    with postgres_connection() as connection:
        connection.execute("SET TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY")
        connection.execute("SET LOCAL lock_timeout = '3s'")
        connection.execute("SET LOCAL statement_timeout = '10s'")
        connection.execute(
            sql.SQL("LOCK TABLE {}.{} IN ACCESS SHARE MODE").format(
                sql.Identifier(SCHEMA),
                sql.Identifier("external_events"),
            )
        )

        with connection.cursor() as cursor:
            cursor.execute(query)
            values = cursor.fetchone()
            if values is None:
                raise RuntimeError("The event statistics query returned no result")
            columns = [column.name for column in cursor.description]
            stats = dict(zip(columns, values, strict=True))

        try:
            with connection.transaction():
                stats["category_distribution"] = load_category_distribution(connection)
        except psycopg.Error:
            stats["category_distribution"] = None

        try:
            with connection.transaction():
                stats["processing_metrics"] = load_processing_metrics(
                    connection,
                    current_events=stats["current_events"],
                    source_occurrences=stats["source_occurrences"],
                )
        except (psycopg.Error, ValueError, TypeError, KeyError):
            logger.exception("Processing metrics are unavailable or inconsistent")
            stats["processing_metrics"] = None

        return stats


def load_category_distribution(
    connection: psycopg.Connection,
) -> list[tuple[str, int]]:
    query = sql.SQL(
        """
        select
            expanded.category,
            count(*) as event_count
        from {}.{} as events
        cross join lateral (
            select distinct category
            from unnest(events.categories) as categories_by_event(category)
        ) as expanded
        where events.is_published is true
        group by expanded.category
        order by event_count desc, expanded.category
        """
    ).format(sql.Identifier(SCHEMA), sql.Identifier("external_events"))

    with connection.cursor() as cursor:
        cursor.execute(query)
        return [(str(category), int(event_count)) for category, event_count in cursor.fetchall()]


def load_processing_metrics(
    connection: psycopg.Connection,
    current_events: int,
    source_occurrences: int,
) -> dict[str, int]:
    """Read processing counts only when they match the event publication."""
    published_table = sql.Identifier(SCHEMA, "external_events")

    comment_row = connection.execute(
        "select obj_description(to_regclass(%s), 'pg_class')",
        (published_table.as_string(connection),),
    ).fetchone()

    comment = comment_row[0] if comment_row else None
    if not isinstance(comment, str):
        raise TypeError("The event publication marker is missing")

    row = connection.execute(
        sql.SQL(
            """
            select publication_id, metrics
            from {}.{}
            where event_table = %s
            """
        ).format(
            sql.Identifier(SCHEMA),
            sql.Identifier("event_processing_metrics"),
        ),
        ("external_events",),
    ).fetchone()

    if row is None:
        raise ValueError("Processing metrics are missing")

    publication_id = str(UUID(str(row[0])))
    if not comment.endswith(f"; publication_id={publication_id}"):
        raise ValueError("Processing metrics belong to another publication")

    metrics = row[1]
    if not isinstance(metrics, dict):
        raise TypeError("Processing metrics must be an object")

    keys = (
        "source_records",
        "after_date_status_checks",
        "after_parking_removed",
        "after_ticket_extras_removed",
        "grouped_event_cards",
        "represented_records",
    )

    counts = {}
    for key in keys:
        value = metrics[key]
        if isinstance(value, bool) or not isinstance(value, (int, str)):
            raise TypeError(f"Invalid count type: {key}")
        if isinstance(value, str) and not value.isdecimal():
            raise ValueError(f"Invalid count: {key}")

        count = int(value)
        if count < 0:
            raise ValueError(f"Negative count: {key}")
        counts[key] = count

    stages = [counts[key] for key in keys[:5]]
    if stages[-1] == 0 or any(before < after for before, after in pairwise(stages)):
        raise ValueError("Processing stages are inconsistent")

    if counts["grouped_event_cards"] != current_events:
        raise ValueError("Event counts do not match")

    if (
        counts["represented_records"] != source_occurrences
        or counts["after_ticket_extras_removed"] != source_occurrences
    ):
        raise ValueError("Represented record counts do not match")

    return counts


@st.cache_data(ttl=60)
def load_file_stats(prefix: str) -> tuple[int, datetime | None]:
    if not STORAGE_ACCOUNT:
        raise RuntimeError("STORAGE_ACCOUNT is not configured")

    service = BlobServiceClient(
        account_url=f"https://{STORAGE_ACCOUNT}.blob.core.windows.net",
        credential=azure_credential(),
    )
    blobs = list(
        service.get_container_client(STORAGE_CONTAINER).list_blobs(name_starts_with=prefix)
    )
    latest = max((blob.last_modified for blob in blobs), default=None)
    return len(blobs), latest


def age(value: datetime | None) -> str:
    if value is None:
        return "No files"
    if value.tzinfo is None:
        value = value.replace(tzinfo=UTC)

    seconds = max(0, int((datetime.now(UTC) - value).total_seconds()))
    if seconds < 120:
        return "Just now"
    if seconds < 7_200:
        return f"{seconds // 60} min ago"
    if seconds < 172_800:
        return f"{seconds // 3_600} h ago"
    return f"{seconds // 86_400} days ago"


@st.fragment(run_every="60s")
def render_dashboard() -> None:
    """Refresh the dashboard while keeping its static header unchanged."""
    st.caption(
        "Dashboard refreshes every 60 seconds while this page is active. "
        "Source update times are shown below."
    )
    if st.button("Refresh now"):
        load_event_stats.clear()
        load_file_stats.clear()

    event_stats: dict[str, object] | None = None

    st.subheader("Events ready for the app")

    try:
        event_stats = load_event_stats()
    except Exception:
        logger.exception("Could not load published event data")
        event_stats = None

    if event_stats is None:
        st.error("Event data is temporarily unavailable.")
    else:
        total = int(event_stats["total_events"])
        current = int(event_stats["current_events"])
        retained = int(event_stats["retained_events"])

        event_columns = st.columns(4)
        event_columns[0].metric("Current events", f"{current:,}")
        event_columns[1].metric("Kept for Saved / Going", f"{retained:,}")
        event_columns[2].metric("Total stored", f"{total:,}")

        st.caption("Older events stay available while Saved or Going still needs them.")

    st.markdown("---")

    st.subheader("How records become events")

    processing = event_stats.get("processing_metrics") if event_stats is not None else None

    if processing is None:
        st.warning(
            "Processing figures are unavailable for the current event update. "
            "Other available figures are shown separately."
        )
    else:
        stages = (
            ("Ticketmaster data", "source_records"),
            ("After date and availability checks", "after_date_status_checks"),
            ("Without parking and ticket extras", "after_ticket_extras_removed"),
            ("One event card per daily group", "grouped_event_cards"),
        )

        for column, (label, key) in zip(st.columns(4), stages, strict=True):
            column.metric(label, f"{processing[key]:,}")

        st.caption(
            "Filtering removes unsuitable records. The final step keeps one "
            "representative record per daily group."
        )

        date_removed = processing["source_records"] - processing["after_date_status_checks"]
        parking_removed = (
            processing["after_date_status_checks"] - processing["after_parking_removed"]
        )
        extras_removed = (
            processing["after_parking_removed"] - processing["after_ticket_extras_removed"]
        )

        with st.popover("About these processing figures"):
            st.write(
                "The starting count includes the latest known version of each "
                "Ticketmaster record, not just the latest download."
            )
            st.write(
                f"{date_removed:,} records excluded by date and availability checks. "
                f"Then {parking_removed:,} parking records and "
                f"{extras_removed:,} ticket extras excluded."
            )

    st.markdown("---")
    st.subheader("Categories")
    if event_stats is None:
        st.info("Category coverage is unavailable until the event table can be read.")
    else:
        categorized = int(event_stats["categorized_events"])
        multiple_category = int(event_stats["multiple_category_events"])
        category_coverage = (categorized / current * 100) if current else 0
        multiple_category_share = (multiple_category / current * 100) if current else 0
        coverage_label = (
            "100%" if current and categorized == current else f"{category_coverage:.1f}%"
        )

        category_columns = st.columns(4)
        category_columns[0].metric("LOC categories", f"{len(LOC_CATEGORIES)}")
        category_columns[1].metric("With a category", coverage_label)
        category_columns[2].metric(
            "With multiple categories",
            f"{multiple_category:,}",
        )
        category_columns[2].caption(f"{multiple_category_share:.1f}% of current events")

        try:
            category_rows = event_stats["category_distribution"]
            if category_rows is None:
                raise RuntimeError("Category distribution is unavailable")
            category_counts = dict(category_rows)
            category_order = sorted(
                set(LOC_CATEGORIES) | set(category_counts),
                key=lambda category: (-category_counts.get(category, 0), category),
            )
            category_distribution = [
                {
                    "LOC category": category,
                    "Events": category_counts.get(category, 0),
                }
                for category in category_order
            ]
            largest_count = max(
                (row["Events"] for row in category_distribution),
                default=0,
            )

            base = alt.Chart(alt.Data(values=category_distribution)).encode(
                y=alt.Y(
                    "LOC category:N",
                    sort=category_order,
                    title=None,
                    axis=alt.Axis(
                        labelLimit=300,
                        labelFontSize=14,
                        labelPadding=10,
                        ticks=False,
                        domain=False,
                    ),
                ),
                x=alt.X(
                    "Events:Q",
                    title=None,
                    scale=alt.Scale(
                        domain=[0, max(1, largest_count) * 1.12],
                    ),
                    axis=None,
                ),
            )

            bars = base.mark_bar(
                color="#F58245",
                size=20,
            ).encode(
                tooltip=[
                    alt.Tooltip("LOC category:N", title="Category"),
                    alt.Tooltip("Events:Q", title="Events", format=","),
                ],
            )

            labels = base.mark_text(
                align="left",
                baseline="middle",
                dx=7,
                fontSize=14,
                color="#374151",
            ).encode(
                text=alt.Text("Events:Q", format=","),
            )

            chart = (bars + labels).properties(height=250).configure_view(strokeWidth=0)

            st.altair_chart(chart, width="stretch", theme=None)
        except Exception as error:  # noqa: BLE001 - keep the summary metrics visible.
            st.error(f"Cannot read the category distribution ({type(error).__name__}).")

        st.caption(
            "One event can appear in more than one category, so category totals can exceed the event count."
        )

    st.caption("Example: Arts & Theatre + Family → Arts & Culture + Family & Kids")
    st.markdown("---")

    st.subheader("Ticket prices")

    if event_stats is None:
        st.info("Price coverage is unavailable until the event table can be read.")
    else:
        known = int(event_stats["known_prices"])
        unknown = int(event_stats["unknown_prices"])
        coverage = f"{known / (known + unknown) * 100:.1f}%" if known + unknown else "—"

        price_columns = st.columns(3)
        price_columns[0].metric("Known", f"{known:,}")
        price_columns[1].metric("Unknown", f"{unknown:,}")
        price_columns[2].metric("Coverage", coverage)

        st.caption("An unknown price does not mean the event is free.")

    st.markdown("---")

    st.subheader("Venue settings")

    if event_stats is None:
        st.info("Venue settings are unavailable until event data can be read.")
    else:
        indoor = int(event_stats["indoor_events"])
        outdoor = int(event_stats["outdoor_events"])
        mixed = int(event_stats["mixed_events"])
        unknown_venue = int(event_stats["unknown_venue_events"])

        known_venue = indoor + outdoor + mixed
        settings_total = known_venue + unknown_venue

        if settings_total != current:
            st.warning("Some venue settings are missing or invalid.")
        else:
            venue_coverage = f"{known_venue / current * 100:.1f}%" if current else "—"
            venue_columns = st.columns(3)
            venue_columns[0].metric("Known", f"{known_venue:,}")
            venue_columns[1].metric("Unknown", f"{unknown_venue:,}")
            venue_columns[2].metric("Coverage", venue_coverage)

            st.caption(f"Indoor: {indoor:,} · Outdoor: {outdoor:,} · Mixed: {mixed:,}")

        st.caption(
            "Inferred from event and venue information, for current events only. "
            "Unknown means insufficient evidence or unavailable enrichment."
        )

    st.markdown("---")
    st.subheader("Latest source updates")
    st.caption(
        "These times show when source files were last updated, "
        "not when the dashboard was refreshed."
    )
    file_sources = (
        ("Events", "raw/events/"),
        ("Ticketmaster prices", "raw/enrichment/prices/ticketmaster/"),
        ("Universe prices", "raw/enrichment/prices/universe/"),
    )
    for label, prefix in file_sources:
        source_label, source_time = st.columns([2, 1])
        source_label.write(label)
        try:
            _, latest_file = load_file_stats(prefix)
            source_time.write(age(latest_file))
        except Exception:
            logger.exception("Could not read source updates for %s", label)
            source_time.write("Unavailable")

    if AIRFLOW_URL:
        st.link_button(
            "Open production Airflow",
            f"{AIRFLOW_URL}/dags/final_project_pipeline/runs",
        )
    else:
        st.button(
            "Open production Airflow",
            disabled=True,
            help="AIRFLOW_URL is not configured for this preview.",
        )

    st.caption(
        f"Dashboard last rendered: {datetime.now(UTC):%H:%M:%S} UTC. "
        "This is not the source update or publication time."
    )


render_dashboard()
