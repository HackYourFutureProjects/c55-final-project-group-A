# Event list & detail — Backend documentation

## Overview

Public read APIs for browsing published events and opening a single event by id.
Both app-created and external events are served from the shared **`event_feed`** view
(via `EventRepository`). Canceled and past published events remain reachable by id
on the detail endpoint; the list endpoint only returns active, published, non-canceled events.

## API

| Method / path               | Auth   | What it does                                  |
|-----------------------------|--------|-----------------------------------------------|
| `GET /api/events`           | Public | Paginated list with optional filters and sort |
| `GET /api/events/{eventId}` | Public | Full public detail for one published event    |

### List — `GET /api/events`

| Item        | Notes                                                                                            |
|-------------|--------------------------------------------------------------------------------------------------|
| Success     | `200` — `EventPageResponse` (`events`, `page`, `size`, `totalElements`, `totalPages`, `hasNext`) |
| Empty       | `200` with `events: []` when nothing matches                                                     |
| Bad request | `400` for invalid params (e.g. incomplete date/location pairs, bad UUID)                         |

**Query params**

| Param                                 | Default          | Notes                                                                |
|---------------------------------------|------------------|----------------------------------------------------------------------|
| `search`                              | —                | Case-insensitive match on title, description, city, category name    |
| `categoryIds`                         | —                | Repeatable; match any selected category                              |
| `dateFrom` / `dateTo`                 | —                | Inclusive `YYYY-MM-DD`; must be provided together                    |
| `latitude` / `longitude` / `radiusKm` | —                | Location circle; all three together                                  |
| `price`                               | —                | `FREE` \| `PAID` \| `UNKNOWN`                                        |
| `timesOfDay`                          | —                | Repeatable: `MORNING` \| `AFTERNOON` \| `EVENING` (Europe/Amsterdam) |
| `sort`                                | `START_TIME_ASC` | Also `POPULARITY_DESC`, `PRICE_ASC`, `PRICE_DESC`                    |
| `page`                                | `0`              | Zero-based                                                           |
| `size`                                | `9`              | 1–100                                                                |

**Item shape:** `EventSummaryResponse` (id, title, categories, schedule, location snippet, price, image, popularity
signals).

### Detail — `GET /api/events/{eventId}`

| Item        | Notes                                    |
|-------------|------------------------------------------|
| Success     | `200` — `EventDetailResponse`            |
| Not found   | `404` if no published event with that id |
| Bad request | `400` if `eventId` is not a valid UUID   |

Includes description, categories, schedule, location, primary image, attendee count, and a calculated `status` (
`UPCOMING` / `ONGOING` / `PAST` / `CANCELLED`).

## Architecture

`EventController` → `EventService` → `EventRepository` (SQL on `event_feed`) → response DTOs

Live OpenAPI: `/api/docs` while the app is running.
