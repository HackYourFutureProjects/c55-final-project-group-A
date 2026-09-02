"""Tests for the normalized price enrichment contract."""

from datetime import UTC, datetime
from decimal import Decimal

import pytest
from pydantic import ValidationError

from src.ingestion.price_models import PriceEnrichmentRecord

EXTRACTED_AT = datetime(2026, 9, 1, 12, 0, tzinfo=UTC)


def test_successful_known_price_is_valid():
    record = PriceEnrichmentRecord(
        provider="universe",
        listing_key="example-event-ABC123",
        normalized_source_url=("https://www.universe.com/events/example-event-ABC123"),
        external_event_ids=["event-1", "event-2"],
        price_min=Decimal("12.50"),
        price_max=Decimal("42.00"),
        currency="EUR",
        is_price_known=True,
        age_limit="18+",
        extraction_status="success",
        extraction_method="universe_graphql",
        extracted_at=EXTRACTED_AT,
        raw_payload={"data": {"event": {"minPrice": 12.5}}},
    )

    assert record.price_min == Decimal("12.50")
    assert record.price_max == Decimal("42.00")
    assert record.external_event_ids == ["event-1", "event-2"]


def test_successful_unknown_price_preserves_nulls():
    record = PriceEnrichmentRecord(
        provider="ticketmaster",
        listing_key="123456789",
        normalized_source_url=("https://www.ticketmaster.nl/event/example/123456789"),
        external_event_ids=["event-1"],
        extraction_status="success",
        extraction_method="ticketmaster_ticketselection",
        extracted_at=EXTRACTED_AT,
    )

    assert record.is_price_known is False
    assert record.price_min is None
    assert record.price_max is None
    assert record.currency is None


def test_failed_request_can_be_stored_for_monitoring():
    record = PriceEnrichmentRecord(
        provider="ticketmaster",
        listing_key="123456789",
        normalized_source_url=("https://www.ticketmaster.nl/event/example/123456789"),
        external_event_ids=["event-1"],
        extraction_status="failed",
        extraction_method="ticketmaster_ticketselection",
        error_code="http_429",
        extracted_at=EXTRACTED_AT,
    )

    assert record.error_code == "http_429"
    assert record.is_price_known is False


def test_unknown_price_cannot_silently_contain_zero():
    with pytest.raises(
        ValidationError,
        match="unknown prices cannot contain price values",
    ):
        PriceEnrichmentRecord(
            provider="universe",
            listing_key="example-event-ABC123",
            normalized_source_url=("https://www.universe.com/events/example-event-ABC123"),
            external_event_ids=["event-1"],
            price_min=Decimal(0),
            price_max=Decimal(0),
            is_price_known=False,
            extraction_status="success",
            extraction_method="universe_graphql",
            extracted_at=EXTRACTED_AT,
        )


def test_known_price_requires_currency():
    with pytest.raises(
        ValidationError,
        match="known prices require",
    ):
        PriceEnrichmentRecord(
            provider="universe",
            listing_key="example-event-ABC123",
            normalized_source_url=("https://www.universe.com/events/example-event-ABC123"),
            external_event_ids=["event-1"],
            price_min=Decimal(10),
            price_max=Decimal(20),
            is_price_known=True,
            extraction_status="success",
            extraction_method="universe_graphql",
            extracted_at=EXTRACTED_AT,
        )


def test_minimum_price_cannot_exceed_maximum_price():
    with pytest.raises(
        ValidationError,
        match="price_min cannot exceed price_max",
    ):
        PriceEnrichmentRecord(
            provider="ticketmaster",
            listing_key="123456789",
            normalized_source_url=("https://www.ticketmaster.nl/event/example/123456789"),
            external_event_ids=["event-1"],
            price_min=Decimal(20),
            price_max=Decimal(10),
            currency="EUR",
            is_price_known=True,
            extraction_status="success",
            extraction_method="ticketmaster_ticketselection",
            extracted_at=EXTRACTED_AT,
        )
