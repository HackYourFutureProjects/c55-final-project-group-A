"""Fetch raw Ticketmaster event records and validate them.

Authentication uses TICKETMASTER_API_KEY without including it in log messages.
"""

import logging
from typing import Any

import requests
from pydantic import ValidationError

from .models import Posting

logger = logging.getLogger(__name__)

REQUEST_TIMEOUT_SECONDS = 30


def fetch_raw(url: str, api_key: str) -> list[Any]:
    """Call Ticketmaster and return its raw event records."""
    logger.info("Fetching Ticketmaster events from %s", url)

    try:
        response = requests.get(
            url,
            params={"apikey": api_key},
            timeout=REQUEST_TIMEOUT_SECONDS,
        )
    except requests.RequestException as exc:
        # Do not include the request URL because it contains the API key.
        raise RuntimeError(f"Ticketmaster API request failed: {type(exc).__name__}") from None

    if not response.ok:
        raise RuntimeError(f"Ticketmaster API returned HTTP {response.status_code}")

    payload = response.json()
    if not isinstance(payload, dict):
        raise TypeError(f"Expected a Ticketmaster response object, got {type(payload).__name__}")

    embedded = payload.get("_embedded", {})
    if not isinstance(embedded, dict):
        raise TypeError("Ticketmaster _embedded value is not an object")

    records = embedded.get("events", [])
    if not isinstance(records, list):
        raise TypeError("Ticketmaster events value is not a list")

    logger.info("Received %d event(s)", len(records))
    return records


def parse_records(records: list[Any]) -> tuple[list[Posting], int]:
    """Validate raw records, returning the good ones and a rejected count.

    One malformed record must not lose the whole batch, so invalid rows are
    counted and skipped. `Any` is deliberate: this is the boundary, and the
    source can send anything.
    """
    parsed: list[Posting] = []
    rejected = 0
    for record in records:
        try:
            parsed.append(Posting.model_validate(record))
        except ValidationError as exc:
            rejected += 1
            # A JSON list can hold a scalar, and .get on one would raise here
            # and lose the batch this loop exists to save.
            identifier = (
                record.get("slug", "<no slug>") if isinstance(record, dict) else repr(record)[:40]
            )
            logger.warning("Rejected record %s: %s", identifier, exc.error_count())
    logger.info("Parsed %d record(s), rejected %d", len(parsed), rejected)
    return parsed, rejected
