"""Tests for deterministic Ticketmaster price extraction."""

from datetime import UTC, datetime
from decimal import Decimal

import requests

from src.ingestion.ticketmaster_prices import (
    REQUEST_TIMEOUT_SECONDS,
    extract_ticketmaster_price,
    fetch_ticketmaster_price,
)

EXTRACTED_AT = datetime(2026, 9, 1, 12, 0, tzinfo=UTC)
LISTING_KEY = "1234567890"
SOURCE_URL = "https://www.ticketmaster.nl/event/example-event/1234567890"


def ticket_type(
    *,
    face_value,
    service_fee=0,
    upsell_fee=0,
    currency="EUR",
):
    return {
        "locked": False,
        "prices": [
            {
                "faceValue": face_value,
                "serviceFeeChargesValue": service_fee,
                "upsellFeeChargesValue": upsell_fee,
            }
        ],
        "ticketPriceComponents": [
            f"{face_value} {currency} TICKET",
        ],
    }


def test_extracts_total_price_including_fees():
    payload = {
        "ticketTypes": [
            ticket_type(
                face_value=9.15,
                service_fee=1.35,
            )
        ]
    }

    price_min, price_max, currency = extract_ticketmaster_price(payload)

    assert price_min == Decimal("10.50")
    assert price_max == Decimal("10.50")
    assert currency == "EUR"


def test_extracts_price_range_across_ticket_types():
    payload = {
        "ticketTypes": [
            ticket_type(face_value=10),
            ticket_type(face_value=25, service_fee=2.5),
        ]
    }

    price_min, price_max, currency = extract_ticketmaster_price(payload)

    assert price_min == Decimal(10)
    assert price_max == Decimal("27.5")
    assert currency == "EUR"


def test_explicit_zero_price_is_preserved_as_known():
    payload = {
        "ticketTypes": [
            ticket_type(face_value=0),
        ]
    }

    price_min, price_max, currency = extract_ticketmaster_price(payload)

    assert price_min == Decimal(0)
    assert price_max == Decimal(0)
    assert currency == "EUR"


def test_missing_price_is_not_interpreted_as_free():
    payload = {
        "ticketTypes": [
            {
                "locked": False,
                "prices": [{}],
                "ticketPriceComponents": [],
            }
        ]
    }

    assert extract_ticketmaster_price(payload) == (None, None, None)


def test_fetch_returns_normalized_success_record():
    payload = {
        "ticketTypes": [
            ticket_type(face_value=9.15, service_fee=1.35),
        ]
    }
    captured = {}

    class FakeResponse:
        ok = True
        status_code = 200

        def json(self):
            return payload

    class FakeSession:
        def get(self, url, headers, timeout):
            captured["url"] = url
            captured["headers"] = headers
            captured["timeout"] = timeout
            return FakeResponse()

    record = fetch_ticketmaster_price(
        listing_key=LISTING_KEY,
        normalized_source_url=SOURCE_URL,
        external_event_ids=["event-1"],
        session=FakeSession(),
        extracted_at=EXTRACTED_AT,
    )

    assert record.extraction_status == "success"
    assert record.is_price_known is True
    assert record.price_min == Decimal("10.50")
    assert record.price_max == Decimal("10.50")
    assert record.currency == "EUR"
    assert record.raw_payload == payload
    assert captured["url"].endswith(f"/{LISTING_KEY}")
    assert captured["headers"]["Referer"] == SOURCE_URL
    assert captured["timeout"] == REQUEST_TIMEOUT_SECONDS


def test_http_error_becomes_failed_record():
    class FakeResponse:
        ok = False
        status_code = 429

    class FakeSession:
        def get(self, url, headers, timeout):
            return FakeResponse()

    record = fetch_ticketmaster_price(
        listing_key=LISTING_KEY,
        normalized_source_url=SOURCE_URL,
        external_event_ids=["event-1"],
        session=FakeSession(),
        extracted_at=EXTRACTED_AT,
    )

    assert record.extraction_status == "failed"
    assert record.is_price_known is False
    assert record.error_code == "http_429"


def test_request_exception_becomes_failed_record():
    class FakeSession:
        def get(self, url, headers, timeout):
            raise requests.Timeout("request timed out")

    record = fetch_ticketmaster_price(
        listing_key=LISTING_KEY,
        normalized_source_url=SOURCE_URL,
        external_event_ids=["event-1"],
        session=FakeSession(),
        extracted_at=EXTRACTED_AT,
    )

    assert record.extraction_status == "failed"
    assert record.error_code == "request_timeout"


def test_invalid_json_becomes_failed_record():
    class FakeResponse:
        ok = True
        status_code = 200

        def json(self):
            raise ValueError("invalid JSON")

    class FakeSession:
        def get(self, url, headers, timeout):
            return FakeResponse()

    record = fetch_ticketmaster_price(
        listing_key=LISTING_KEY,
        normalized_source_url=SOURCE_URL,
        external_event_ids=["event-1"],
        session=FakeSession(),
        extracted_at=EXTRACTED_AT,
    )

    assert record.extraction_status == "failed"
    assert record.error_code == "invalid_json"


def test_currency_parser_ignores_three_letter_product_labels():
    payload = {
        "ticketTypes": [
            {
                "locked": False,
                "prices": [
                    {
                        "faceValue": 174,
                        "serviceFeeChargesValue": 7.1,
                        "upsellFeeChargesValue": 0,
                    }
                ],
                "ticketPriceComponents": [
                    "34.00 EUR TICKET",
                    "140.00 EUR VIP Nation",
                    "7.10 EUR FEE",
                ],
            }
        ]
    }

    price_min, price_max, currency = extract_ticketmaster_price(payload)

    assert price_min == Decimal("181.1")
    assert price_max == Decimal("181.1")
    assert currency == "EUR"


def test_currency_parser_supports_component_ranges():
    payload = {
        "ticketTypes": [
            {
                "locked": False,
                "prices": [
                    {
                        "faceValue": 53,
                        "serviceFeeChargesValue": 6.47,
                        "upsellFeeChargesValue": 0,
                    },
                    {
                        "faceValue": 93,
                        "serviceFeeChargesValue": 11.35,
                        "upsellFeeChargesValue": 0,
                    },
                ],
                "ticketPriceComponents": [
                    "53.00 - 93.00 EUR TICKET",
                    "6.47 - 11.35 EUR FEE",
                ],
            }
        ]
    }

    price_min, price_max, currency = extract_ticketmaster_price(payload)

    assert price_min == Decimal("59.47")
    assert price_max == Decimal("104.35")
    assert currency == "EUR"
