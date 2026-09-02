"""Tests for deterministic Universe price extraction."""

from datetime import UTC, datetime
from decimal import Decimal

import requests

from src.ingestion.universe_prices import (
    GRAPHQL_URL,
    REQUEST_TIMEOUT_SECONDS,
    fetch_universe_price,
)

EXTRACTED_AT = datetime(2026, 9, 1, 12, 0, tzinfo=UTC)
LISTING_KEY = "example-event-ABC123"
SOURCE_URL = f"https://www.universe.com/events/{LISTING_KEY}"


def successful_payload(
    *,
    price_min=12.5,
    price_max=42,
    currency="EUR",
    age_limit="",
):
    return {
        "data": {
            "event": {
                "id": "universe-event-id",
                "minPrice": price_min,
                "maxPrice": price_max,
                "transactionCurrency": currency,
                "ageLimit": age_limit,
            }
        }
    }


def fetch_with_payload(payload):
    captured = {}

    class FakeResponse:
        ok = True
        status_code = 200

        def json(self):
            return payload

    class FakeSession:
        def post(self, url, json, headers, timeout):
            captured["url"] = url
            captured["json"] = json
            captured["headers"] = headers
            captured["timeout"] = timeout
            return FakeResponse()

    record = fetch_universe_price(
        listing_key=LISTING_KEY,
        normalized_source_url=SOURCE_URL,
        external_event_ids=["event-1", "event-2"],
        session=FakeSession(),
        extracted_at=EXTRACTED_AT,
    )

    return record, captured


def test_fetch_returns_normalized_success_record():
    payload = successful_payload()

    record, captured = fetch_with_payload(payload)

    assert record.extraction_status == "success"
    assert record.is_price_known is True
    assert record.price_min == Decimal("12.5")
    assert record.price_max == Decimal(42)
    assert record.currency == "EUR"
    assert record.raw_payload == payload
    assert captured["url"] == GRAPHQL_URL
    assert captured["json"]["variables"] == {"id": LISTING_KEY}
    assert captured["headers"]["Referer"] == SOURCE_URL
    assert captured["timeout"] == REQUEST_TIMEOUT_SECONDS


def test_zero_price_and_age_limit_are_preserved():
    record, _ = fetch_with_payload(
        successful_payload(
            price_min=0,
            price_max=0,
            currency=" eur ",
            age_limit="18+",
        )
    )

    assert record.is_price_known is True
    assert record.price_min == Decimal(0)
    assert record.price_max == Decimal(0)
    assert record.currency == "EUR"
    assert record.age_limit == "18+"


def test_missing_price_is_successful_but_unknown():
    record, _ = fetch_with_payload(
        successful_payload(
            price_min=None,
            price_max=None,
            currency=None,
        )
    )

    assert record.extraction_status == "success"
    assert record.is_price_known is False
    assert record.price_min is None
    assert record.price_max is None
    assert record.currency is None
    assert record.error_code == "price_unavailable"


def test_graphql_error_becomes_failed_record():
    payload = {
        "errors": [
            {"message": "Event is unavailable"},
        ]
    }

    record, _ = fetch_with_payload(payload)

    assert record.extraction_status == "failed"
    assert record.error_code == "graphql_error"
    assert record.raw_payload == payload


def test_invalid_price_range_becomes_failed_record():
    record, _ = fetch_with_payload(
        successful_payload(
            price_min=50,
            price_max=10,
        )
    )

    assert record.extraction_status == "failed"
    assert record.error_code == "invalid_price_range"


def test_request_exception_becomes_failed_record():
    class FakeSession:
        def post(self, url, json, headers, timeout):
            raise requests.Timeout("request timed out")

    record = fetch_universe_price(
        listing_key=LISTING_KEY,
        normalized_source_url=SOURCE_URL,
        external_event_ids=["event-1"],
        session=FakeSession(),
        extracted_at=EXTRACTED_AT,
    )

    assert record.extraction_status == "failed"
    assert record.error_code == "request_timeout"
