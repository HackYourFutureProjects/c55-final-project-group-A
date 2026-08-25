"""Validation at the edge: what survives, what is rejected, what is counted."""

from datetime import UTC, date, datetime

import pytest
from pydantic import ValidationError

from src.ingestion.ingest import (
    MAX_PAGES,
    REQUEST_TIMEOUT_SECONDS,
    fetch_raw,
    parse_records,
)
from src.ingestion.models import TicketmasterEvent

GOOD = {
    "id": "event-1",
    "name": "Example Concert",
    "type": "event",
    "url": "https://example.test/events/event-1",
    "locale": "en-us",
    "dates": {
        "start": {
            "localDate": "2026-09-18",
            "dateTime": "2026-09-18T18:00:00Z",
        }
    },
    "seatmap": {
        "staticUrl": "https://example.test/seatmap.jpg",
    },
}


def test_fetch_raw_authenticates_and_extracts_ticketmaster_events(monkeypatch):
    expected_events = [{"id": "event-1", "name": "Example Event"}]
    captured = {}

    class FakeResponse:
        ok = True
        status_code = 200

        def json(self):
            return {"_embedded": {"events": expected_events}}

    def fake_get(url, params, timeout):
        captured["url"] = url
        captured["params"] = params
        captured["timeout"] = timeout
        return FakeResponse()

    monkeypatch.setattr("src.ingestion.ingest.requests.get", fake_get)

    records = fetch_raw(
        url="https://example.test/events.json",
        api_key="test-api-key",
    )

    assert records == expected_events
    assert captured == {
        "url": "https://example.test/events.json",
        "params": {
            "apikey": "test-api-key",
            "size": 200,
            "page": 0,
            "countryCode": "NL",
        },
        "timeout": REQUEST_TIMEOUT_SECONDS,
    }


def test_fetch_raw_combines_ticketmaster_pages(monkeypatch):
    responses = [
        {
            "_embedded": {
                "events": [{"id": "event-1", "name": "First Event"}],
            },
            "page": {"number": 0, "totalPages": 2},
        },
        {
            "_embedded": {
                "events": [{"id": "event-2", "name": "Second Event"}],
            },
            "page": {"number": 1, "totalPages": 2},
        },
    ]
    requested_params = []

    class FakeResponse:
        ok = True
        status_code = 200

        def __init__(self, payload):
            self.payload = payload

        def json(self):
            return self.payload

    def fake_get(url, params, timeout):
        requested_params.append(params)
        return FakeResponse(responses[len(requested_params) - 1])

    monkeypatch.setattr("src.ingestion.ingest.requests.get", fake_get)

    records = fetch_raw(
        url="https://example.test/events.json",
        api_key="test-api-key",
    )

    assert records == [
        {"id": "event-1", "name": "First Event"},
        {"id": "event-2", "name": "Second Event"},
    ]
    assert requested_params == [
        {"apikey": "test-api-key", "size": 200, "page": 0, "countryCode": "NL"},
        {"apikey": "test-api-key", "size": 200, "page": 1, "countryCode": "NL"},
    ]


def test_fetch_raw_stops_at_deep_paging_limit(monkeypatch):
    requested_pages = []

    class FakeResponse:
        ok = True
        status_code = 200

        def __init__(self, page_number):
            self.page_number = page_number

        def json(self):
            return {
                "_embedded": {
                    "events": [
                        {
                            "id": f"event-{self.page_number}",
                            "name": f"Event {self.page_number}",
                        }
                    ]
                },
                "page": {
                    "number": self.page_number,
                    "totalPages": 100,
                },
            }

    def fake_get(url, params, timeout):
        page_number = params["page"]
        requested_pages.append(page_number)
        return FakeResponse(page_number)

    monkeypatch.setattr("src.ingestion.ingest.requests.get", fake_get)

    records = fetch_raw(
        url="https://example.test/events.json",
        api_key="test-api-key",
    )

    assert len(records) == MAX_PAGES
    assert requested_pages == list(range(MAX_PAGES))


def test_fetch_raw_http_error_does_not_expose_api_key(monkeypatch):
    class FakeResponse:
        ok = False
        status_code = 401

    def fake_get(url, params, timeout):
        return FakeResponse()

    monkeypatch.setattr("src.ingestion.ingest.requests.get", fake_get)

    secret_key = "do-not-expose-this-key"

    with pytest.raises(RuntimeError) as exc_info:
        fetch_raw(
            url="https://example.test/events.json",
            api_key=secret_key,
        )

    message = str(exc_info.value)

    assert "401" in message
    assert secret_key not in message


def test_fetch_raw_reports_invalid_json_without_exposing_key(monkeypatch):
    class FakeResponse:
        ok = True
        status_code = 200

        def json(self):
            raise ValueError("invalid response body")

    def fake_get(url, params, timeout):
        return FakeResponse()

    monkeypatch.setattr("src.ingestion.ingest.requests.get", fake_get)

    secret_key = "do-not-expose-this-key"

    with pytest.raises(RuntimeError, match="invalid JSON") as exc_info:
        fetch_raw(
            url="https://example.test/events.json",
            api_key=secret_key,
        )

    assert secret_key not in str(exc_info.value)


def test_good_record_survives():
    parsed, rejected = parse_records([GOOD])

    assert rejected == 0
    assert parsed[0].id == "event-1"
    assert parsed[0].name == "Example Concert"


def test_one_bad_record_does_not_lose_the_batch():
    parsed, rejected = parse_records([GOOD, {"id": "missing-name"}])

    assert len(parsed) == 1
    assert rejected == 1


def test_missing_optional_fields_and_extra_fields_are_allowed():
    raw_event = {
        "id": "minimal-event",
        "name": "TBA Event",
        "unexpectedField": {"value": "kept"},
    }

    parsed, rejected = parse_records([raw_event])

    assert rejected == 0
    assert len(parsed) == 1
    assert parsed[0].dates is None
    assert parsed[0].model_extra == {
        "unexpectedField": {"value": "kept"},
    }


def test_a_scalar_in_the_list_is_rejected_not_fatal():
    parsed, rejected = parse_records([GOOD, "not-a-dict", 42])

    assert len(parsed) == 1
    assert rejected == 2


def test_ticketmaster_dates_are_parsed():
    event = TicketmasterEvent.model_validate(GOOD)

    assert event.dates is not None
    assert event.dates.start is not None
    assert event.dates.start.local_date == date(2026, 9, 18)
    assert event.dates.start.date_time == datetime(
        2026,
        9,
        18,
        18,
        0,
        tzinfo=UTC,
    )


def test_missing_required_name_is_rejected():
    without_name = {key: value for key, value in GOOD.items() if key != "name"}

    with pytest.raises(ValidationError):
        TicketmasterEvent.model_validate(without_name)
