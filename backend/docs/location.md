# Location Autocomplete — Backend documentation

## Overview

Address search-as-you-type, used when creating an event and when filtering events by location. The backend proxies 
OpenStreetMap's Nominatim geocoding service rather than exposing it directly — this keeps API keys and rate-limit 
handling server-side, and lets the frontend call one simple endpoint instead of dealing with Nominatim's request format 
and usage policy directly.

## API

| Method / path | Auth | What it does |
|---|---|---|
| `GET` `/api/locations/suggest` | Public | Address suggestions for a search query |

## Request / response

**`GET /api/locations/suggest?q=Weteringschans`**

Returns up to 5 suggestions, deduplicated by label:
```json
[
  {
    "id": "123456",
    "label": "Weteringschans, Amsterdam, North Holland, Netherlands",
    "street": "Weteringschans",
    "houseNumber": "6",
    "postalCode": "1017SG",
    "latitude": 52.3612,
    "longitude": 4.8828,
    "cityName": "Amsterdam",
    "province": "North Holland"
  }
]
```

City resolution falls back through Nominatim's own hierarchy — city, then town, then village — since not every address
has a `city` field specifically.

## Known limitation

Nominatim doesn't reliably match partial words mid-string (e.g. searching "Wetering" may not surface "Weteringschans") 
— this is a limitation of the upstream service, not something the backend works around.

## Rate limiting

Nominatim's usage policy caps requests at 1 per second. The backend sets a `User-Agent` header identifying the app, 
as required by that policy. Rapid, unthrottled typing on the frontend (a request per keystroke) can trigger `503` 
responses from upstream — the frontend is expected to debounce input before calling this endpoint.

## Errors

| Status | When |
|---|---|
| `503` | Nominatim is unreachable or rate-limiting the request |

## Architecture

`LocationController` → `LocationService` (wraps `RestClient` calls to Nominatim) → `NominatimResult` (internal record 
for parsing the upstream response, never exposed directly)

One endpoint only — `/search` with `addressdetails=1` returns everything needed in a single round-trip, so there's no 
separate "lookup by id" endpoint.