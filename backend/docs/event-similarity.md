# Event similarity — Backend documentation

## Overview

Content-based recommendations for the event detail page: find other events that look related by comparing categories,
city, time of day, weekday, and price type.

- No AI, collaborative filtering, or personal data.
- App-created and external events both participate. Ranking uses skinny rows from `events` and
  `analytics.external_events` (id, city, start, categories, price). Card fields for the five winners
  are loaded from **`event_feed`**.
- Similarity is scored in SQL; the numeric score is used only for ranking and is **not** returned in the API response.

## API

| Item          | Value                                                                                               |
|---------------|-----------------------------------------------------------------------------------------------------|
| Method / path | `GET /api/events/{eventId}/similar`                                                                 |
| Auth          | Public                                                                                              |
| Path param    | `eventId` — UUID of the source event                                                                |
| Query params  | None (limit fixed at 5)                                                                             |
| Success       | `200` — JSON array of `EventSummaryResponse`                                                        |
| Empty result  | `200` with `[]`                                                                                     |
| Not found     | `404` if no **published** source (app `events` + address, or mart row with the same canonical UUID) |
| Bad request   | `400` if `eventId` is not a valid UUID                                                              |

## Architecture

EventSimilarityController → EventSimilarityService.findSimilarEvents(eventId, 5) → EventSimilarityRepository
(skinny rank, then `event_feed` for five cards) → EventSummaryResponse (score discarded)

## Eligibility

**Source:** must be published (same identity as `event_feed`: app event with an address, or Ticketmaster canonical
UUID).

**Candidates:** different id, published, not cancelled, still active
(`end_at > now()`, or null `end_at` and `start_at > now()`).

## Scoring (max 100)

| Signal      | Max | Rule                                              |
|-------------|-----|---------------------------------------------------|
| Categories  | 55  | Jaccard: \|A∩B\| / \|A∪B\| × 55 (0 if both empty) |
| City        | 20  | `lower(trim(city_name))` equal                    |
| Time of day | 15  | Same Amsterdam time bucket                        |
| Weekday     | 7   | Same ISO weekday in Amsterdam                     |
| Price type  | 3   | Same FREE / PAID / UNKNOWN bucket                 |

No minimum score — weak matches can appear if few better ones exist.

### Time buckets (`Europe/Amsterdam` on `start_at`)

- **MORNING** `[06:00, 12:00)`
- **AFTERNOON** `[12:00, 18:00)`
- **EVENING** everything else (`18:00`–`05:59`)

### Price buckets

- `NULL` → UNKNOWN · `0` → FREE · else → PAID

## Ranking

1. similarity score DESC
2. `start_at` ASC
3. id ASC

Then `LIMIT 5`.

## Scoring examples

| Candidate                 | Cat | City | Time | Weekday | Price | Result |
|---------------------------|-----|------|------|---------|-------|--------|
| All signals match         | 55  | 20   | 15   | 7       | 3     | 100    |
| Categories + city + price | 55  | 20   | 0    | 0       | 3     | 78     |
| City + time only          | 0   | 20   | 15   | 0       | 0     | 35     |
| Price type only           | 0   | 0    | 0    | 0       | 3     | 3      |
