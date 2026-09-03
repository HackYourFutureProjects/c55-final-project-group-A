"""Tests for the complete Ticketmaster ingestion flow."""

import logging
from datetime import UTC, date, datetime
from decimal import Decimal

import pytest

from src.ingestion import pipeline
from src.ingestion.price_models import PriceEnrichmentRecord

RAW_EVENTS = [
    {
        "id": "event-1",
        "name": "Example Concert",
        "categorySpecificField": {"value": "preserved"},
    },
    {
        "id": "event-without-name",
        "unknownField": "also preserved",
    },
]


def test_load_config_reads_enabled_price_enrichment_providers(monkeypatch):
    monkeypatch.setenv("SOURCE_API_URL", "https://example.test/events.json")
    monkeypatch.setenv("TICKETMASTER_API_KEY", "test-api-key")
    monkeypatch.setenv("PRICE_ENRICHMENT_PROVIDERS", " universe ")

    config = pipeline.load_config(local=True)

    assert config.price_enrichment_providers == frozenset({"universe"})


def test_pipeline_lands_original_raw_events_including_rejected(monkeypatch, caplog):
    config = pipeline.Config(
        source_api_url="https://example.test/events.json",
        source_name="events",
        ticketmaster_api_key="test-api-key",
        storage_account="teststorage",
        databricks_catalog="test_catalog",
        landing_container="dev",
        landing_prefix="mohammed",
    )
    captured = {}

    def fake_fetch_raw(url, api_key, start_date):
        captured["start_date"] = start_date
        return RAW_EVENTS

    def fake_land_raw_json(*, account, path, records, container):
        captured["account"] = account
        captured["path"] = path
        captured["records"] = records
        captured["container"] = container
        return len(records)

    monkeypatch.setattr(pipeline, "load_config", lambda local=False: config)
    monkeypatch.setattr(pipeline, "fetch_raw", fake_fetch_raw)
    monkeypatch.setattr(pipeline, "land_raw_json", fake_land_raw_json)
    caplog.set_level(logging.INFO, logger="pipeline")

    landed = pipeline.run("2026-08-16")

    assert landed == 2
    assert captured == {
        "start_date": date(2026, 8, 16),
        "account": "teststorage",
        "path": "mohammed/events/ingest_date=2026-08-16/data.json",
        "records": RAW_EVENTS,
        "container": "dev",
    }
    assert (
        "Pipeline started: source=events, run_date=2026-08-16, destination=dev/mohammed"
        in caplog.text
    )
    assert "Pipeline finished: 2 landed, 1 rejected" in caplog.text


def test_pipeline_rejects_an_empty_extraction_before_landing(monkeypatch):
    config = pipeline.Config(
        source_api_url="https://example.test/events.json",
        source_name="events",
        ticketmaster_api_key="test-api-key",
        storage_account="teststorage",
        databricks_catalog="test_catalog",
        landing_container="dev",
        landing_prefix="mohammed",
    )

    monkeypatch.setattr(pipeline, "load_config", lambda local=False: config)
    monkeypatch.setattr(pipeline, "fetch_raw", lambda url, api_key, start_date: [])

    def unexpected_land(**kwargs):
        pytest.fail("an empty extraction must not be landed")

    monkeypatch.setattr(pipeline, "land_raw_json", unexpected_land)

    with pytest.raises(RuntimeError, match="No valid records: 0 received, 0 rejected"):
        pipeline.run("2026-08-16")


def test_pipeline_lands_price_providers_in_separate_folders(
    monkeypatch,
):
    config = pipeline.Config(
        source_api_url="https://example.test/events.json",
        source_name="events",
        ticketmaster_api_key="test-api-key",
        storage_account="teststorage",
        databricks_catalog="test_catalog",
        landing_container="dev",
        landing_prefix="pavel",
    )
    raw_events = [
        {
            "id": "event-1",
            "name": "Example Event",
            "url": ("https://www.ticketmaster.nl/event/" "example-event/1234567890"),
        },
        {
            "id": "event-2",
            "name": "Universe Event",
            "url": ("https://www.universe.com/events/" "example-event-ABC123"),
        },
    ]
    enrichment_record = PriceEnrichmentRecord(
        provider="ticketmaster",
        listing_key="1234567890",
        normalized_source_url=("https://www.ticketmaster.nl/event/" "example-event/1234567890"),
        external_event_ids=["event-1"],
        price_min=Decimal("10.50"),
        price_max=Decimal("20.00"),
        currency="EUR",
        is_price_known=True,
        extraction_status="success",
        extraction_method="ticketmaster_ticketselection",
        extracted_at=datetime(2026, 9, 1, 12, 0, tzinfo=UTC),
    )
    universe_enrichment_record = PriceEnrichmentRecord(
        provider="universe",
        listing_key="example-event-ABC123",
        normalized_source_url=("https://www.universe.com/events/example-event-ABC123"),
        external_event_ids=["event-2"],
        price_min=Decimal("5.00"),
        price_max=Decimal("15.00"),
        currency="EUR",
        is_price_known=True,
        extraction_status="success",
        extraction_method="universe_graphql",
        extracted_at=datetime(2026, 9, 1, 12, 0, tzinfo=UTC),
    )
    writes = []

    def fake_land_raw_json(
        *,
        account,
        path,
        records,
        container,
    ):
        writes.append(
            {
                "account": account,
                "path": path,
                "records": records,
                "container": container,
            }
        )
        return len(records)

    monkeypatch.setattr(pipeline, "load_config", lambda local=False: config)
    monkeypatch.setattr(
        pipeline,
        "fetch_raw",
        lambda url, api_key, start_date: raw_events,
    )

    def fake_enrich_event_prices(records, providers):
        assert records == raw_events
        assert providers == {"ticketmaster", "universe"}
        return [enrichment_record, universe_enrichment_record]

    monkeypatch.setattr(
        pipeline,
        "enrich_event_prices",
        fake_enrich_event_prices,
    )
    monkeypatch.setattr(
        pipeline,
        "land_raw_json",
        fake_land_raw_json,
    )

    landed = pipeline.run("2026-09-01")

    assert landed == 2
    assert [write["path"] for write in writes] == [
        "pavel/events/ingest_date=2026-09-01/data.json",
        ("pavel/enrichment/prices/ticketmaster/" "ingest_date=2026-09-01/data.json"),
        ("pavel/enrichment/prices/universe/" "ingest_date=2026-09-01/data.json"),
    ]
    assert writes[0]["records"] == raw_events
    assert writes[1]["records"][0]["listing_key"] == "1234567890"
    assert writes[1]["records"][0]["price_min"] == "10.50"
    assert writes[1]["records"][0]["currency"] == "EUR"
    assert writes[2]["records"][0]["listing_key"] == "example-event-ABC123"
    assert writes[2]["records"][0]["price_min"] == "5.00"
    assert writes[2]["records"][0]["currency"] == "EUR"
