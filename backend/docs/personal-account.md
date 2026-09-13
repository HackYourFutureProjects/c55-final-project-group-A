# Personal Account — Backend documentation

## Overview

Profile management and the two personal event lists — Saved (events you're considering) and Going (events you've 
committed to). Both internal and external (Ticketmaster) events can be saved or marked Going; an identity registry 
resolves either kind to a stable id before it's referenced (see `docs/events.md` for how the two sources are combined).

## API

| Method / path | Auth | What it does |
|---|---|---|
| `GET` `/api/users/me` | Authenticated | Current user's profile |
| `PATCH` `/api/users/me` | Authenticated | Partial profile update |
| `DELETE` `/api/users/me` | Authenticated | Deletes the account |
| `GET` `/api/users/me/saved` | Authenticated | Paginated list of saved events |
| `GET` `/api/users/me/going` | Authenticated | Paginated list of events marked Going |
| `POST` `/api/events/{eventId}/saved` | Authenticated | Save an event |
| `DELETE` `/api/events/{eventId}/saved` | Authenticated | Remove a saved event |
| `POST` `/api/events/{eventId}/going` | Authenticated | Mark an event as Going |
| `DELETE` `/api/events/{eventId}/going` | Authenticated | Unmark Going |

## Profile

**`GET /api/users/me`** — returns `userId`, `role`, `name`, `email`, `location`, `createdAt`.

**`PATCH /api/users/me`** — any subset of `name`, `email`, `location`; fields left out are untouched (`COALESCE` at the 
SQL level, not full replacement). Email uniqueness is checked, excluding the user's own current record.
```json
{ "name": "New Name" }
```

**`DELETE /api/users/me`** — deletes the account and, via cascading foreign keys, everything tied to it (sessions, saved
events, going status, comments).

## Saved & Going

Both lists return the same card shape — `id`, `title`, `imageUrl`, `categories`, `startAt`, `endAt`, `price`, 
address fields, `cancelled` — matching the public event list shape so the frontend can reuse one card component 
everywhere.

**`GET /api/users/me/saved`** / **`GET /api/users/me/going`**

| Query param | Default | Notes |
|---|---|---|
| `page` | 0 | Zero-based |
| `size` | 9 | 1–100 |

```json
{ "events": [], "page": 0, "size": 9, "totalElements": 3, "totalPages": 1, "hasNext": false }
```

**`POST` / `DELETE` `/api/events/{eventId}/saved`** and **`/going`** — no body, `eventId` is the event's id 
(internal or external, both work identically from the caller's side).

Before a save/going action on an external event, its identity is registered (if not already) so the foreign key 
on `saved_events`/`event_attendees` resolves — this is transparent to the caller, no extra step needed on the frontend.

## Errors

| Status | When |
|---|---|
| `404` | User or event not found |
| `409` | New email already in use (on profile update) |
| `400` | Invalid `page`/`size`, malformed request body |

## Architecture

`UserController` / `UserEventController` → `UserService` / `UserEventService` → `UserRepository` / `UserEventRepository`

Reads for Saved/Going join against `event_feed` (the same combined internal + external view used by the public event 
list), not the `events` table directly — this is what lets an external event show up correctly in a personal list with 
full details (image, categories, address) despite not having a row in the app's own events table.