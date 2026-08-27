"""Tests for the complete Ticketmaster ingestion flow."""

import logging

import pytest

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
    caplog.set_level(logging.INFO, logger="pipeline")

    landed = pipeline.run("2026-08-16")

    assert landed == 2
    assert captured == {
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
    monkeypatch.setattr(pipeline, "fetch_raw", lambda url, api_key: [])

    def unexpected_land(**kwargs):
        pytest.fail("an empty extraction must not be landed")

    monkeypatch.setattr(pipeline, "land_raw_json", unexpected_land)

    with pytest.raises(RuntimeError, match="No valid records: 0 received, 0 rejected"):
        pipeline.run("2026-08-16")
