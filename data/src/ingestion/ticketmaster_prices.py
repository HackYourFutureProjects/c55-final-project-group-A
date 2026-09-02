"""Extract deterministic prices from Ticketmaster ticket-selection data."""

from __future__ import annotations

import re
from datetime import UTC, datetime
from decimal import Decimal, InvalidOperation
from typing import Any, Protocol

import requests

from src.ingestion.price_models import PriceEnrichmentRecord

ENDPOINT_TEMPLATE = "https://www.ticketmaster.nl/api/ticketselection/{listing_key}"
REQUEST_TIMEOUT_SECONDS = 30
CURRENCY_PATTERN = re.compile(r"\d+(?:[.,]\d+)?" r"(?:\s*-\s*\d+(?:[.,]\d+)?)?" r"\s+([A-Z]{3})\b")


class TicketmasterSession(Protocol):
    """Minimal HTTP client required by the Ticketmaster extractor."""

    def get(
        self,
        url: str,
        *,
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


def _currency(payload: dict[str, Any]) -> str | None:
    currencies: set[str] = set()

    for ticket_type in payload.get("ticketTypes", []):
        for component in ticket_type.get("ticketPriceComponents", []):
            match = CURRENCY_PATTERN.search(str(component).upper())
            if match:
                currencies.add(match.group(1))

    if len(currencies) == 1:
        return currencies.pop()

    return None


def extract_ticketmaster_price(
    payload: dict[str, Any],
) -> tuple[Decimal | None, Decimal | None, str | None]:
    """Return the minimum and maximum total customer price."""

    totals: list[Decimal] = []

    for ticket_type in payload.get("ticketTypes", []):
        if ticket_type.get("locked") is True:
            continue

        for price in ticket_type.get("prices", []):
            face_value = _decimal(price.get("faceValue"))
            if face_value is None:
                continue

            service_fee = _decimal(price.get("serviceFeeChargesValue"))
            upsell_fee = _decimal(price.get("upsellFeeChargesValue"))

            total = face_value
            if service_fee is not None:
                total += service_fee
            if upsell_fee is not None:
                total += upsell_fee

            totals.append(total)

    currency = _currency(payload)

    if not totals or currency is None:
        return None, None, None

    return min(totals), max(totals), currency


def fetch_ticketmaster_price(
    *,
    listing_key: str,
    normalized_source_url: str,
    external_event_ids: list[str],
    session: TicketmasterSession | None = None,
    extracted_at: datetime | None = None,
) -> PriceEnrichmentRecord:
    """Fetch one Ticketmaster listing without failing the event ingestion."""

    client = session or requests.Session()
    endpoint = ENDPOINT_TEMPLATE.format(listing_key=listing_key)
    timestamp = extracted_at or datetime.now(tz=UTC)

    try:
        response = client.get(
            endpoint,
            headers={
                "Accept": "application/json",
                "Referer": normalized_source_url,
                "User-Agent": (
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
                    "AppleWebKit/537.36 Chrome/127.0 Safari/537.36"
                ),
            },
            timeout=REQUEST_TIMEOUT_SECONDS,
        )
    except requests.RequestException as error:
        return PriceEnrichmentRecord(
            provider="ticketmaster",
            listing_key=listing_key,
            normalized_source_url=normalized_source_url,
            external_event_ids=external_event_ids,
            extraction_status="failed",
            extraction_method="ticketmaster_ticketselection",
            error_code=f"request_{type(error).__name__.lower()}",
            extracted_at=timestamp,
        )

    if not response.ok:
        return PriceEnrichmentRecord(
            provider="ticketmaster",
            listing_key=listing_key,
            normalized_source_url=normalized_source_url,
            external_event_ids=external_event_ids,
            extraction_status="failed",
            extraction_method="ticketmaster_ticketselection",
            error_code=f"http_{response.status_code}",
            extracted_at=timestamp,
        )

    try:
        payload = response.json()
    except ValueError:
        return PriceEnrichmentRecord(
            provider="ticketmaster",
            listing_key=listing_key,
            normalized_source_url=normalized_source_url,
            external_event_ids=external_event_ids,
            extraction_status="failed",
            extraction_method="ticketmaster_ticketselection",
            error_code="invalid_json",
            extracted_at=timestamp,
        )

    if not isinstance(payload, dict):
        return PriceEnrichmentRecord(
            provider="ticketmaster",
            listing_key=listing_key,
            normalized_source_url=normalized_source_url,
            external_event_ids=external_event_ids,
            extraction_status="failed",
            extraction_method="ticketmaster_ticketselection",
            error_code="invalid_payload",
            extracted_at=timestamp,
        )

    price_min, price_max, currency = extract_ticketmaster_price(payload)
    is_price_known = price_min is not None and price_max is not None and currency is not None

    return PriceEnrichmentRecord(
        provider="ticketmaster",
        listing_key=listing_key,
        normalized_source_url=normalized_source_url,
        external_event_ids=external_event_ids,
        price_min=price_min,
        price_max=price_max,
        currency=currency,
        is_price_known=is_price_known,
        extraction_status="success",
        extraction_method="ticketmaster_ticketselection",
        error_code=None if is_price_known else "price_unavailable",
        extracted_at=timestamp,
        raw_payload=payload,
    )
