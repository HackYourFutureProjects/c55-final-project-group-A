"""Tests for the complete Ticketmaster ingestion flow."""

from src.ingestion import pipeline

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


def test_pipeline_lands_original_raw_events_including_rejected(monkeypatch):
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

    def fake_fetch_raw(url, api_key):
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

    landed = pipeline.run("2026-08-16")

    assert landed == 2
    assert captured == {
        "account": "teststorage",
        "path": "mohammed/events/ingest_date=2026-08-16/data.json",
        "records": RAW_EVENTS,
        "container": "dev",
    }
