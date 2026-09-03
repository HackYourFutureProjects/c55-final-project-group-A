"""Fetch raw Ticketmaster event records and validate them.

Authentication uses TICKETMASTER_API_KEY without including it in log messages.
"""

import logging
from datetime import date, timedelta
from typing import Any

import requests
from pydantic import ValidationError

from .models import TicketmasterEvent

logger = logging.getLogger(__name__)

REQUEST_TIMEOUT_SECONDS = 30
PAGE_SIZE = 200
MAX_PAGES = 5
DATE_WINDOW_DAYS = 30
DATE_WINDOW_COUNT = 5


def fetch_raw(url: str, api_key: str, start_date: date | None = None) -> list[Any]:
    """Fetch and combine paginated Ticketmaster event records."""
    logger.info("Fetching Ticketmaster events from %s", url)

    records: list[Any] = []
    seen_event_ids: set[str] = set()

    windows: list[tuple[date | None, date | None]] = [(None, None)]
    if start_date is not None:
        windows = [
            (
                start_date + timedelta(days=window_number * DATE_WINDOW_DAYS),
                start_date + timedelta(days=(window_number + 1) * DATE_WINDOW_DAYS),
            )
            for window_number in range(DATE_WINDOW_COUNT)
        ]

    for window_start, window_end in windows:
        for requested_page in range(MAX_PAGES):
            params: dict[str, str | int] = {
                "apikey": api_key,
                "size": PAGE_SIZE,
                "page": requested_page,
                "countryCode": "NL",
            }
            if window_start is not None and window_end is not None:
                params.update(
                    {
                        "startDateTime": f"{window_start.isoformat()}T00:00:00Z",
                        "endDateTime": f"{window_end.isoformat()}T00:00:00Z",
                        "sort": "date,asc",
                    }
                )

            try:
                response = requests.get(
                    url,
                    params=params,
                    timeout=REQUEST_TIMEOUT_SECONDS,
                )
            except requests.RequestException as exc:

                raise RuntimeError(
                    f"Ticketmaster API request failed: {type(exc).__name__}"
                ) from None

            if not response.ok:
                raise RuntimeError(f"Ticketmaster API returned HTTP {response.status_code}")

            try:
                payload = response.json()
            except ValueError as exc:
                raise RuntimeError(f"Ticketmaster API returned invalid JSON: {exc}") from None
            if not isinstance(payload, dict):
                raise TypeError(
                    f"Expected a Ticketmaster response object, got {type(payload).__name__}"
                )

            embedded = payload.get("_embedded", {})
            if not isinstance(embedded, dict):
                raise TypeError("Ticketmaster _embedded value is not an object")

            page_records = embedded.get("events", [])
            if not isinstance(page_records, list):
                raise TypeError("Ticketmaster events value is not a list")

            for record in page_records:
                event_id = record.get("id") if isinstance(record, dict) else None
                if start_date is not None and isinstance(event_id, str) and event_id:
                    if event_id in seen_event_ids:
                        continue
                    seen_event_ids.add(event_id)
                records.append(record)

            page = payload.get("page", {})
            if not isinstance(page, dict):
                raise TypeError("Ticketmaster page value is not an object")

            current_page = page.get("number", requested_page)
            total_pages = page.get("totalPages", 1)

            if not isinstance(current_page, int) or not isinstance(total_pages, int):
                raise TypeError("Ticketmaster page numbers must be integers")

            if current_page + 1 >= total_pages:
                break

            if requested_page == MAX_PAGES - 1:
                logger.warning(
                    "Stopped after %d pages because of Ticketmaster's deep-paging limit",
                    MAX_PAGES,
                )

    logger.info("Received %d event(s) across paginated responses", len(records))
    return records


def parse_records(records: list[Any]) -> tuple[list[TicketmasterEvent], int]:
    """Validate raw records, returning the good ones and a rejected count.

    One malformed record must not lose the whole batch, so invalid rows are
    counted and skipped. `Any` is deliberate: this is the boundary, and the
    source can send anything.
    """
    parsed: list[TicketmasterEvent] = []
    rejected = 0
    for record in records:
        try:
            parsed.append(TicketmasterEvent.model_validate(record))
        except ValidationError as exc:
            rejected += 1
            # A JSON list can hold a scalar, and .get on one would raise here
            # and lose the batch this loop exists to save.
            identifier = (
                record.get("id", "<no eventid>") if isinstance(record, dict) else repr(record)[:40]
            )
            logger.warning("Rejected record %s: %s", identifier, exc.error_count())
    logger.info("Parsed %d record(s), rejected %d", len(parsed), rejected)
    return parsed, rejected
