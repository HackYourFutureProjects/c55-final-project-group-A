"""The ingestion job: fetch, validate, land. This is what the container runs.

    uv run python -m src.ingestion.pipeline [--run-date YYYY-MM-DD]

Settings come from the environment: .env on your machine and the job definition
in Azure. The Ticketmaster API key is supplied as a secret environment variable
and must never be logged or committed. See the README, "Settings".
"""

import argparse
import logging
import os
import sys
from dataclasses import dataclass
from datetime import UTC, datetime
from pathlib import Path

from dotenv import load_dotenv

from .ingest import fetch_raw, parse_records
from .storage import (
    LOCAL_LANDING_DIR,
    PRODUCTION_CONTAINER,
    PRODUCTION_PREFIX,
    blob_path,
    land_local_json,
    land_raw_json,
)

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(name)s %(message)s",
)
logger = logging.getLogger("pipeline")

# Landing-folder name under LANDING_PREFIX (local / aca-dev / prod raw).
# Same in every environment for a single source — not an env var.
SOURCE_NAME = "events"


class MissingSetting(RuntimeError):
    """A required environment variable is not set."""


@dataclass(frozen=True)
class Config:
    """Configuration required by the ingestion job."""

    source_api_url: str
    source_name: str
    ticketmaster_api_key: str
    # Empty only for a --local run, which never opens a connection to Azure.
    storage_account: str
    databricks_catalog: str
    landing_container: str
    landing_prefix: str


def load_config(local: bool = False) -> Config:
    """Read settings, failing at startup rather than ten minutes in.

    A local run needs the source and nothing else. Demanding a storage account
    to write a file to your own disk would put the cloud in the way of the one
    step that exists to get a look at a new API before any of it is set up.
    """
    load_dotenv()

    def required(name: str) -> str:
        value = os.getenv(name)
        if not value:
            raise MissingSetting(f"{name} is not set. Copy .env.example to .env and fill it in.")
        return value

    return Config(
        source_api_url=required("SOURCE_API_URL"),
        source_name=os.getenv("SOURCE_NAME", "source"),
        ticketmaster_api_key=required("TICKETMASTER_API_KEY"),
        storage_account="" if local else required("STORAGE_ACCOUNT"),
        databricks_catalog=os.getenv("DATABRICKS_CATALOG", "team_a"),
        # The scheduled run writes `prod/raw`. Your own runs write
        # `dev/<your name>`, a different container that you alone can write.
        landing_container=os.getenv("LANDING_CONTAINER", PRODUCTION_CONTAINER),
        landing_prefix=os.getenv("LANDING_PREFIX", PRODUCTION_PREFIX),
    )


def run(run_date: str | None = None, local_dir: Path | None = None) -> int:
    """Run one execution and return the number of records landed.

    `local_dir` writes to this machine instead of the landing zone. See
    `storage.land_local_json` for why that is a look, not a stage.
    """
    config = load_config(local=local_dir is not None)
    run_date = run_date or datetime.now(tz=UTC).date().isoformat()

    destination = (
        f"local:{local_dir}"
        if local_dir is not None
        else f"{config.landing_container}/{config.landing_prefix}"
    )
    logger.info(
        "Pipeline started: source=%s, run_date=%s, destination=%s",
        config.source_name,
        run_date,
        destination,
    )

    records = fetch_raw(config.source_api_url, api_key=config.ticketmaster_api_key)
    parsed, rejected = parse_records(records)

    # An empty batch is a failed extraction, not a quiet success: it would
    # leave yesterday's mart in place with every test still passing.
    if not parsed:
        raise RuntimeError(f"No valid records: {len(records)} received, {rejected} rejected")
    if rejected:
        logger.warning(
            "%d of %d records failed validation and are still being landed",
            rejected,
            len(records),
        )

    # Land what the source sent, not what validation produced. Parsing is a
    # gate, not a transformation. See the README, "Raw means raw".
    path = blob_path(SOURCE_NAME, run_date, config.landing_prefix)

    if local_dir is not None:
        landed = land_local_json(local_dir, path, records)
        logger.info(
            "Pipeline finished: %d written locally, %d rejected. Open the file, decide "
            "what the staging model should keep, then re-run without --local.",
            landed,
            rejected,
        )
        return landed

    landed = land_raw_json(
        account=config.storage_account,
        path=path,
        records=records,
        container=config.landing_container,
    )

    landing_root = os.getenv("LANDING_PATH")
    # Team A LANDING_PATH is the source folder itself (…/events), matching the
    # Ticketmaster staging contract. Do not append SOURCE_NAME again.
    readable = landing_root or "(set LANDING_PATH so dbt reads what you just wrote)"
    logger.info(
        "Pipeline finished: %d landed, %d rejected, readable at %s",
        landed,
        rejected,
        readable,
    )
    return landed


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Run one ingestion.")
    parser.add_argument(
        "--run-date",
        default=None,
        help="the day this run belongs to, YYYY-MM-DD. Defaults to today.",
    )
    parser.add_argument(
        "--local",
        nargs="?",
        const=LOCAL_LANDING_DIR,
        default=None,
        type=Path,
        metavar="DIR",
        help=(
            "write the file to this machine instead of the landing zone, for looking at "
            f"a new source before you wire it up. Defaults to {LOCAL_LANDING_DIR}/. "
            "dbt cannot read it: the warehouse has no access to your disk."
        ),
    )
    args = parser.parse_args()

    try:
        run(args.run_date, args.local)
    except Exception:
        logger.exception("Pipeline failed")
        sys.exit(1)
