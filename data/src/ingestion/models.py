"""Validation models for Ticketmaster event data."""

from datetime import date, datetime

from pydantic import BaseModel, Field


class EventStart(BaseModel):
    """Ticketmaster start-date information."""

    local_date: date | None = Field(default=None, alias="localDate")
    date_time: datetime | None = Field(default=None, alias="dateTime")

    model_config = {"populate_by_name": True, "extra": "allow"}


class EventDates(BaseModel):
    """Ticketmaster event-date information."""

    start: EventStart | None = None

    model_config = {"extra": "allow"}


class TicketmasterEvent(BaseModel):
    """One event returned by the Ticketmaster Discovery API."""

    id: str
    name: str
    type: str | None = None
    url: str | None = None
    locale: str | None = None
    dates: EventDates | None = None

    model_config = {"populate_by_name": True, "extra": "allow"}
