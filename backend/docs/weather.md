# Weather Forecast — Backend documentation

## Overview

Live weather forecast for an event's exact start time — not just the day, the specific hour — shown on the event detail
page and used as context for the AI assistant (`docs/event-chat.md`). Backed by Open-Meteo, a free forecasting API 
that requires no API key.

## API

| Method / path | Auth | What it does |
|---|---|---|
| `GET` `/api/weather` | Public | Forecast for a location and time |

## Request / response

**`GET /api/weather?latitude=52.3676&longitude=4.9041&eventTime=2026-09-13T21:00:00%2B02:00`**

```json
{
  "available": true,
  "temperature": 17,
  "condition": "Partly cloudy",
  "precipitationChance": 12,
  "windSpeed": 9
}
```

| Field | Notes |
|---|---|
| `available` | `false` if no forecast exists for the requested time (see below) |
| `temperature` | Degrees Celsius |
| `condition` | Human-readable, translated from Open-Meteo's numeric weather code |
| `precipitationChance` | Percent |
| `windSpeed` | Kilometers per hour |

When `available` is `false`, the other fields are `null` — this isn't an error, it's an expected, valid response for 
an event too far in the future for a forecast to exist yet.

## Forecast horizon

Open-Meteo provides hourly forecasts up to 16 days ahead. Requesting weather for an event further out than that 
returns `available: false` rather than a `404` or `503` — there's nothing wrong with the request, the data simply 
doesn't exist yet.

## Time matching

The requested `eventTime` is converted to the event's local timezone (Europe/Amsterdam) and rounded to the nearest hour
before being matched against Open-Meteo's hourly data — the API returns one value per hour, not a continuous range.

## Errors

| Status | When |
|---|---|
| `400` | Missing or invalid `latitude`/`longitude`/`eventTime` |
| `503` | Open-Meteo is unreachable |

## Architecture

`WeatherController` → `WeatherService` (wraps `RestClient` calls to Open-Meteo) → `OpenMeteoResult` (internal record for 
parsing the upstream response)

Weather codes are mapped to short human-readable text (e.g. `0` → "Clear sky", `61` → "Rain") entirely on the backend — 
the frontend never has to know Open-Meteo's numeric code scheme.