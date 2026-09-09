# OPTIONAL. A read-only health page for the data pipeline.
"""Show whether Team A's event pipeline is producing fresh, usable data."""

import os
from datetime import UTC, datetime

import psycopg
import streamlit as st
from azure.identity import DefaultAzureCredential, ManagedIdentityCredential
from azure.keyvault.secrets import SecretClient
from azure.storage.blob import BlobServiceClient
from dotenv import load_dotenv
from psycopg import sql

load_dotenv()

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
    page_title="Team A pipeline health",
    page_icon="📊",
    layout="wide",
)
st.title("Team A event pipeline health")
st.caption("Read-only pipeline metrics. Data refreshes every 60 seconds.")
st.markdown("**Data source → Filtered, cleaned and consolidated → " "Quality checks → Backend**")

if st.button("Refresh now"):
    st.cache_data.clear()


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
            coalesce(sum(occurrence_count), 0) as source_occurrences,
            count(*) as total_events,
            count(*) filter (where is_published is true) as current_events,
            count(*) filter (where is_published is false) as retained_events,
            count(*) filter (
                where coalesce(cardinality(categories), 0) > 0
            ) as categorized_events,
            count(*) filter (
                where coalesce(cardinality(categories), 0) > 1
            ) as multiple_category_events,
            count(*) filter (
                where is_published is true and is_price_known is true
            ) as known_prices,
            count(*) filter (
                where is_published is true and is_price_known is false
            ) as unknown_prices,
            max(ingested_at) filter (where is_published is true) as last_ingested
        from {}.{}
        """
    ).format(sql.Identifier(SCHEMA), sql.Identifier("external_events"))

    with postgres_connection() as connection, connection.cursor() as cursor:
        cursor.execute(query)
        values = cursor.fetchone()
        if values is None:
            raise RuntimeError("The event statistics query returned no result")
        columns = [column.name for column in cursor.description]
        return dict(zip(columns, values, strict=True))


@st.cache_data(ttl=60)
def load_category_distribution() -> list[tuple[str, int]]:
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
        group by expanded.category
        order by event_count desc, expanded.category
        """
    ).format(sql.Identifier(SCHEMA), sql.Identifier("external_events"))

    with postgres_connection() as connection, connection.cursor() as cursor:
        cursor.execute(query)
        return [(str(category), int(event_count)) for category, event_count in cursor.fetchall()]


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


event_stats: dict[str, object] | None = None

st.subheader("Published event data")
try:
    event_stats = load_event_stats()
    source_occurrences = int(event_stats["source_occurrences"])
    total = int(event_stats["total_events"])
    current = int(event_stats["current_events"])
    retained = int(event_stats["retained_events"])
    last_ingested = event_stats["last_ingested"]

    event_columns = st.columns(4)
    event_columns[0].metric("Current events", f"{current:,}")
    event_columns[1].metric("Retained Saved/Going", f"{retained:,}")
    event_columns[2].metric("Total stored", f"{total:,}")
    event_columns[3].metric("Latest ingest", age(last_ingested))
except Exception as error:  # noqa: BLE001 - keep the other health checks visible.
    st.error(f"Cannot read the published event table ({type(error).__name__}).")

st.subheader("Transformation overview")
if event_stats is None:
    st.info("Transformation metrics are unavailable until the event table can be read.")
else:
    transformation_columns = st.columns(4)
    transformation_columns[0].metric("Source occurrences represented", f"{source_occurrences:,}")
    transformation_columns[1].metric("Logical published events", f"{current:,}")
    transformation_columns[2].metric("Retained Saved/Going", f"{retained:,}")
    transformation_columns[3].metric("Total stored", f"{total:,}")

st.subheader("LOC category coverage")
if event_stats is None:
    st.info("Category coverage is unavailable until the event table can be read.")
else:
    categorized = int(event_stats["categorized_events"])
    multiple_category = int(event_stats["multiple_category_events"])
    category_coverage = (categorized / total * 100) if total else 0
    multiple_category_share = (multiple_category / total * 100) if total else 0
    coverage_label = "100%" if total and categorized == total else f"{category_coverage:.1f}%"

    category_columns = st.columns(3)
    category_columns[0].metric("LOC categories", f"{len(LOC_CATEGORIES)}")
    category_columns[1].metric("Categorized", coverage_label)
    category_columns[2].metric("Multiple categories", f"{multiple_category:,}")
    category_columns[2].caption(f"{multiple_category_share:.1f}% of Total stored")

    try:
        category_counts = dict(load_category_distribution())
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
        st.bar_chart(
            category_distribution,
            x="LOC category",
            y="Events",
            horizontal=True,
            sort=False,
        )
    except Exception as error:  # noqa: BLE001 - keep the summary metrics visible.
        st.error(f"Cannot read the category distribution ({type(error).__name__}).")

    st.caption(
        "Category counts may add up to more than Total stored because one event "
        "can have multiple categories."
    )

st.subheader("Price coverage")
if event_stats is None:
    st.info("Price coverage is unavailable until the event table can be read.")
else:
    known = int(event_stats["known_prices"])
    unknown = int(event_stats["unknown_prices"])
    coverage = (known / (known + unknown) * 100) if known + unknown else 0

    price_columns = st.columns(3)
    price_columns[0].metric("Known prices", f"{known:,}")
    price_columns[1].metric("Unknown prices", f"{unknown:,}")
    price_columns[2].metric("Known coverage", f"{coverage:.1f}%")

st.subheader("Landing files")
file_sources = (
    ("Event files", "raw/events/"),
    ("Ticketmaster price files", "raw/enrichment/prices/ticketmaster/"),
    ("Universe price files", "raw/enrichment/prices/universe/"),
)
file_columns = st.columns(len(file_sources))
for column, (label, prefix) in zip(file_columns, file_sources, strict=True):
    try:
        file_count, latest_file = load_file_stats(prefix)
        column.metric(label, age(latest_file))
        column.caption(f"{file_count:,} files")
    except Exception as error:  # noqa: BLE001 - keep the other source checks visible.
        column.error(f"Unavailable ({type(error).__name__})")

if AIRFLOW_URL:
    st.link_button(
        "Open production Airflow",
        f"{AIRFLOW_URL}/dags/final_project_pipeline/runs",
    )

st.caption("This page checks the data itself. Airflow contains the task logs and run history.")
