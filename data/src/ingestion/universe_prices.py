"""Extract deterministic prices from the Universe GraphQL API."""

from __future__ import annotations

from datetime import UTC, datetime
from decimal import Decimal, InvalidOperation
from typing import Any, Protocol

import requests

from src.ingestion.price_models import PriceEnrichmentRecord

GRAPHQL_URL = "https://www.universe.com/graphql"
REQUEST_TIMEOUT_SECONDS = 30

EVENT_QUERY = """
query CacheableEvent($id: ID!) {
  event(id: $id) {
    id
    transactionCurrency
    minPrice
    maxPrice
    ageLimit
  }
}
"""


class UniverseSession(Protocol):
    """Minimal HTTP client required by the Universe extractor."""

    def post(
        self,
        url: str,
        *,
        json: dict[str, Any],
        headers: dict[str, str],
        timeout: int,
    ) -> Any: ...


def _decimal(value: Any) -> Decimal | None:
    if isinstance(value, bool) or value is None:
        return None

    try:
        return Decimal(str(value))
    except (InvalidOperation, TypeError, ValueError):
        return None


def _failed_record(
    *,
    listing_key: str,
    normalized_source_url: str,
    external_event_ids: list[str],
    extracted_at: datetime,
    error_code: str,
    raw_payload: dict[str, Any] | None = None,
) -> PriceEnrichmentRecord:
    return PriceEnrichmentRecord(
        provider="universe",
        listing_key=listing_key,
        normalized_source_url=normalized_source_url,
        external_event_ids=external_event_ids,
        extraction_status="failed",
        extraction_method="universe_graphql",
        error_code=error_code,
        extracted_at=extracted_at,
        raw_payload=raw_payload,
    )


def fetch_universe_price(
    *,
    listing_key: str,
    normalized_source_url: str,
    external_event_ids: list[str],
    session: UniverseSession | None = None,
    extracted_at: datetime | None = None,
) -> PriceEnrichmentRecord:
    """Fetch one Universe listing without failing the event ingestion."""

    client = session or requests.Session()
    timestamp = extracted_at or datetime.now(tz=UTC)

    try:
        response = client.post(
            GRAPHQL_URL,
            json={
                "operationName": "CacheableEvent",
                "variables": {"id": listing_key},
                "query": EVENT_QUERY,
            },
            headers={
                "Accept": "application/json",
                "Origin": "https://www.universe.com",
                "Referer": normalized_source_url,
                "User-Agent": (
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
                    "AppleWebKit/537.36 Chrome/127.0 Safari/537.36"
                ),
            },
            timeout=REQUEST_TIMEOUT_SECONDS,
        )
    except requests.RequestException as error:
        return _failed_record(
            listing_key=listing_key,
            normalized_source_url=normalized_source_url,
            external_event_ids=external_event_ids,
            extracted_at=timestamp,
            error_code=f"request_{type(error).__name__.lower()}",
        )

    if not response.ok:
        return _failed_record(
            listing_key=listing_key,
            normalized_source_url=normalized_source_url,
            external_event_ids=external_event_ids,
            extracted_at=timestamp,
            error_code=f"http_{response.status_code}",
        )

    try:
        payload = response.json()
    except ValueError:
        return _failed_record(
            listing_key=listing_key,
            normalized_source_url=normalized_source_url,
            external_event_ids=external_event_ids,
            extracted_at=timestamp,
            error_code="invalid_json",
        )

    if not isinstance(payload, dict):
        return _failed_record(
            listing_key=listing_key,
            normalized_source_url=normalized_source_url,
            external_event_ids=external_event_ids,
            extracted_at=timestamp,
            error_code="invalid_payload",
        )

    if payload.get("errors"):
        return _failed_record(
            listing_key=listing_key,
            normalized_source_url=normalized_source_url,
            external_event_ids=external_event_ids,
            extracted_at=timestamp,
            error_code="graphql_error",
            raw_payload=payload,
        )

    data = payload.get("data")
    event = data.get("event") if isinstance(data, dict) else None

    if not isinstance(event, dict):
        return _failed_record(
            listing_key=listing_key,
            normalized_source_url=normalized_source_url,
            external_event_ids=external_event_ids,
            extracted_at=timestamp,
            error_code="event_not_found",
            raw_payload=payload,
        )

    price_min = _decimal(event.get("minPrice"))
    price_max = _decimal(event.get("maxPrice"))
    currency_value = event.get("transactionCurrency")
    currency = (
        currency_value.strip().upper()
        if isinstance(currency_value, str) and currency_value.strip()
        else None
    )

    age_limit_value = event.get("ageLimit")
    age_limit = (
        age_limit_value.strip()
        if isinstance(age_limit_value, str) and age_limit_value.strip()
        else None
    )

    is_price_known = price_min is not None and price_max is not None and currency is not None

    if not is_price_known:
        price_min = None
        price_max = None
        currency = None

    if is_price_known and price_min is not None and price_max is not None and price_min > price_max:
        return _failed_record(
            listing_key=listing_key,
            normalized_source_url=normalized_source_url,
            external_event_ids=external_event_ids,
            extracted_at=timestamp,
            error_code="invalid_price_range",
            raw_payload=payload,
        )

    return PriceEnrichmentRecord(
        provider="universe",
        listing_key=listing_key,
        normalized_source_url=normalized_source_url,
        external_event_ids=external_event_ids,
        price_min=price_min,
        price_max=price_max,
        currency=currency,
        is_price_known=is_price_known,
        age_limit=age_limit,
        extraction_status="success",
        extraction_method="universe_graphql",
        error_code=None if is_price_known else "price_unavailable",
        extracted_at=timestamp,
        raw_payload=payload,
    )
