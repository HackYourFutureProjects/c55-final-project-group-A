"""Models for deterministic external event price enrichment."""

from __future__ import annotations

from datetime import datetime
from decimal import Decimal
from typing import Any, Literal

from pydantic import BaseModel, ConfigDict, model_validator


class PriceEnrichmentRecord(BaseModel):
    """One extraction result for one unique external event listing."""

    model_config = ConfigDict(extra="forbid")

    provider: Literal["ticketmaster", "universe"]
    listing_key: str
    normalized_source_url: str
    external_event_ids: list[str]

    price_min: Decimal | None = None
    price_max: Decimal | None = None
    currency: str | None = None
    is_price_known: bool = False

    age_limit: str | None = None

    extraction_status: Literal["success", "failed"]
    extraction_method: Literal[
        "ticketmaster_ticketselection",
        "universe_graphql",
    ]
    error_code: str | None = None
    extracted_at: datetime

    raw_payload: dict[str, Any] | None = None

    @model_validator(mode="after")
    def validate_price_state(self) -> PriceEnrichmentRecord:
        if self.is_price_known:
            if self.price_min is None or self.price_max is None or self.currency is None:
                raise ValueError("known prices require price_min, price_max and currency")

            if self.price_min > self.price_max:
                raise ValueError("price_min cannot exceed price_max")

        elif self.price_min is not None or self.price_max is not None:
            raise ValueError("unknown prices cannot contain price values")

        if self.extraction_status == "failed" and self.is_price_known:
            raise ValueError("a failed extraction cannot contain a known price")

        return self
