"""Tests for price enrichment orchestration."""

from datetime import UTC, datetime
from decimal import Decimal

import requests

from src.ingestion import price_enrichment
from src.ingestion.price_models import PriceEnrichmentRecord

EXTRACTED_AT = datetime(2026, 9, 1, 12, 0, tzinfo=UTC)


def test_parses_and_normalizes_ticketmaster_url():
    result = price_enrichment.parse_price_target(
        "https://www.ticketmaster.nl/event/example/1234567890" "?language=en-us"
    )

    assert result == (
        "ticketmaster",
        "1234567890",
        "https://www.ticketmaster.nl/event/example/1234567890",
    )


def test_parses_and_normalizes_universe_url():
    result = price_enrichment.parse_price_target(
        "https://www.universe.com/events/example-event-ABC123" "?ref=ticketmaster"
    )

    assert result == (
        "universe",
        "example-event-ABC123",
        "https://www.universe.com/events/example-event-ABC123",
    )


def test_unsupported_url_is_ignored():
    assert price_enrichment.parse_price_target("https://example.test/events/event-1") is None


def test_build_targets_deduplicates_shared_listing():
    records = [
        {
            "id": "event-1",
            "url": ("https://www.universe.com/events/example-event-ABC123" "?ref=ticketmaster"),
        },
        {
            "id": "event-2",
            "url": ("https://www.universe.com/events/example-event-ABC123" "?different=query"),
        },
        {
            "id": "event-2",
            "url": ("https://www.universe.com/events/example-event-ABC123"),
        },
    ]

    targets = price_enrichment.build_price_targets(records)

    assert len(targets) == 1
    assert targets[0].provider == "universe"
    assert targets[0].listing_key == "example-event-ABC123"
    assert targets[0].external_event_ids == ["event-1", "event-2"]


def test_build_targets_skips_invalid_and_unsupported_records():
    records = [
        "not-an-object",
        42,
        {"id": "missing-url"},
        {"id": "missing-url"},
        {"url": "https://www.universe.com/events/example"},
        {
            "id": "unsupported",
            "url": "https://example.test/events/example",
        },
        {
            "id": "supported",
            "url": ("https://www.ticketmaster.nl/event/example/1234567890"),
        },
    ]

    targets = price_enrichment.build_price_targets(records)

    assert len(targets) == 1
    assert targets[0].listing_key == "1234567890"


def test_enrichment_dispatches_each_unique_listing_once(monkeypatch):
    records = [
        {
            "id": "ticketmaster-event",
            "url": ("https://www.ticketmaster.nl/event/example/1234567890"),
        },
        {
            "id": "universe-occurrence-1",
            "url": ("https://www.universe.com/events/example-event-ABC123"),
        },
        {
            "id": "universe-occurrence-2",
            "url": ("https://www.universe.com/events/example-event-ABC123" "?ref=ticketmaster"),
        },
    ]
    calls = []

    def successful_record(provider, kwargs):
        return PriceEnrichmentRecord(
            provider=provider,
            listing_key=kwargs["listing_key"],
            normalized_source_url=kwargs["normalized_source_url"],
            external_event_ids=kwargs["external_event_ids"],
            price_min=Decimal(10),
            price_max=Decimal(20),
            currency="EUR",
            is_price_known=True,
            extraction_status="success",
            extraction_method=(
                "ticketmaster_ticketselection" if provider == "ticketmaster" else "universe_graphql"
            ),
            extracted_at=kwargs["extracted_at"],
        )

    def fake_ticketmaster(**kwargs):
        calls.append(("ticketmaster", kwargs))
        return successful_record("ticketmaster", kwargs)

    def fake_universe(**kwargs):
        calls.append(("universe", kwargs))
        return successful_record("universe", kwargs)

    monkeypatch.setattr(
        price_enrichment,
        "fetch_ticketmaster_price",
        fake_ticketmaster,
    )
    monkeypatch.setattr(
        price_enrichment,
        "fetch_universe_price",
        fake_universe,
    )

    session = requests.Session()
    enriched = price_enrichment.enrich_event_prices(
        records,
        session=session,
        extracted_at=EXTRACTED_AT,
    )

    assert len(enriched) == 2
    assert [provider for provider, _ in calls] == [
        "ticketmaster",
        "universe",
    ]
    assert calls[0][1]["session"] is session
    assert calls[1][1]["external_event_ids"] == [
        "universe-occurrence-1",
        "universe-occurrence-2",
    ]


def test_enrichment_without_session_uses_worker_path_and_keeps_order(
    monkeypatch,
):
    records = [
        {
            "id": "ticketmaster-event",
            "url": ("https://www.ticketmaster.nl/event/example/1234567890"),
        },
        {
            "id": "universe-event",
            "url": ("https://www.universe.com/events/example-event-ABC123"),
        },
    ]
    calls = []

    def fake_enrich_target(
        target,
        *,
        session,
        extracted_at,
    ):
        calls.append(
            {
                "listing_key": target.listing_key,
                "session": session,
            }
        )
        return PriceEnrichmentRecord(
            provider=target.provider,
            listing_key=target.listing_key,
            normalized_source_url=target.normalized_source_url,
            external_event_ids=target.external_event_ids,
            price_min=Decimal(10),
            price_max=Decimal(20),
            currency="EUR",
            is_price_known=True,
            extraction_status="success",
            extraction_method=(
                "ticketmaster_ticketselection"
                if target.provider == "ticketmaster"
                else "universe_graphql"
            ),
            extracted_at=extracted_at,
        )

    monkeypatch.setattr(
        price_enrichment,
        "_enrich_target",
        fake_enrich_target,
    )

    enriched = price_enrichment.enrich_event_prices(
        records,
        extracted_at=EXTRACTED_AT,
    )

    assert [record.listing_key for record in enriched] == [
        "1234567890",
        "example-event-ABC123",
    ]
    assert len(calls) == 2
    assert all(call["session"] is None for call in calls)
