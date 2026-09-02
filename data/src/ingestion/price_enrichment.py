"""Coordinate deterministic enrichment for unique external event listings."""

from __future__ import annotations

import logging
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass, field
from datetime import UTC, datetime
from functools import partial
from typing import Any, Literal
from urllib.parse import urlsplit, urlunsplit

import requests

from src.ingestion.price_models import PriceEnrichmentRecord
from src.ingestion.ticketmaster_prices import fetch_ticketmaster_price
from src.ingestion.universe_prices import fetch_universe_price

logger = logging.getLogger(__name__)

Provider = Literal["ticketmaster", "universe"]
MAX_CONCURRENT_REQUESTS = 3


@dataclass
class PriceEnrichmentTarget:
    """One unique external listing shared by one or more API events."""

    provider: Provider
    listing_key: str
    normalized_source_url: str
    external_event_ids: list[str] = field(default_factory=list)


def parse_price_target(url: str) -> tuple[Provider, str, str] | None:
    """Identify a supported provider and remove query parameters."""

    parsed = urlsplit(url)
    host = (parsed.hostname or "").lower()
    path = parsed.path.rstrip("/")
    parts = [part for part in path.split("/") if part]

    if (
        host in {"ticketmaster.nl", "www.ticketmaster.nl"}
        and len(parts) >= 3
        and parts[0] == "event"
        and parts[-1].isdigit()
    ):
        provider: Provider = "ticketmaster"
        listing_key = parts[-1]
    elif (
        host in {"universe.com", "www.universe.com"}
        and len(parts) == 2
        and parts[0] == "events"
        and parts[1]
    ):
        provider = "universe"
        listing_key = parts[1]
    else:
        return None

    normalized_url = urlunsplit(
        (
            "https",
            host,
            path,
            "",
            "",
        )
    )

    return provider, listing_key, normalized_url


def build_price_targets(
    records: list[Any],
) -> list[PriceEnrichmentTarget]:
    """Deduplicate event rows by their external sales listing."""

    targets: dict[tuple[Provider, str], PriceEnrichmentTarget] = {}

    for record in records:
        if not isinstance(record, dict):
            continue

        event_id = record.get("id")
        source_url = record.get("url")

        if not isinstance(event_id, str) or not event_id:
            continue
        if not isinstance(source_url, str) or not source_url:
            continue

        parsed_target = parse_price_target(source_url)
        if parsed_target is None:
            continue

        provider, listing_key, normalized_url = parsed_target
        target_key = (provider, listing_key)

        if target_key not in targets:
            targets[target_key] = PriceEnrichmentTarget(
                provider=provider,
                listing_key=listing_key,
                normalized_source_url=normalized_url,
            )

        event_ids = targets[target_key].external_event_ids
        if event_id not in event_ids:
            event_ids.append(event_id)

    return list(targets.values())


def _enrich_target(
    target: PriceEnrichmentTarget,
    *,
    session: requests.Session | None,
    extracted_at: datetime,
) -> PriceEnrichmentRecord:
    if target.provider == "ticketmaster":
        return fetch_ticketmaster_price(
            listing_key=target.listing_key,
            normalized_source_url=target.normalized_source_url,
            external_event_ids=target.external_event_ids,
            session=session,
            extracted_at=extracted_at,
        )

    return fetch_universe_price(
        listing_key=target.listing_key,
        normalized_source_url=target.normalized_source_url,
        external_event_ids=target.external_event_ids,
        session=session,
        extracted_at=extracted_at,
    )


def enrich_event_prices(
    records: list[Any],
    *,
    session: requests.Session | None = None,
    extracted_at: datetime | None = None,
) -> list[PriceEnrichmentRecord]:
    """Enrich each supported unique listing exactly once."""

    timestamp = extracted_at or datetime.now(tz=UTC)
    targets = build_price_targets(records)
    enriched: list[PriceEnrichmentRecord] = []

    if session is not None:
        enriched = [
            _enrich_target(
                target,
                session=session,
                extracted_at=timestamp,
            )
            for target in targets
        ]
    else:
        worker = partial(
            _enrich_target,
            session=None,
            extracted_at=timestamp,
        )

        with ThreadPoolExecutor(max_workers=MAX_CONCURRENT_REQUESTS) as executor:
            enriched = list(executor.map(worker, targets))

    successful = sum(record.extraction_status == "success" for record in enriched)
    known = sum(record.is_price_known for record in enriched)

    logger.info(
        "Price enrichment finished: %d target(s), %d successful, " "%d with known prices",
        len(enriched),
        successful,
        known,
    )

    return enriched
