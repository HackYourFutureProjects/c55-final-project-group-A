# Feedback — Backend documentation

## Overview

A simple feedback form, open to anyone — about the app in general, or about a specific event. Submission is anonymous 
by default; name and email are optional, only needed if the person wants a reply. Admins review submissions from a 
dedicated dashboard.

## API

| Method / path | Auth | What it does |
|---|---|---|
| `POST` `/api/feedback` | Public | Submits feedback |
| `GET` `/api/admin/feedback` | Admin | Paginated list of all feedback |
| `PATCH` `/api/admin/feedback/{id}` | Admin | Marks feedback as reviewed / not reviewed |

## Submit feedback

**`POST /api/feedback`**
```json
{
  "topic": "event",
  "eventTitle": "Amsterdam Music Night",
  "rating": 4,
  "message": "Great vibe, but the queue was too long",
  "senderName": "Anouk de Vries",
  "senderEmail": "anouk@example.com"
}
```

| Field | Required | Notes |
|---|---|---|
| `topic` | Yes | `app` or `event` |
| `eventTitle` | No | Free text, only relevant when `topic` is `event` — not linked to a specific event record |
| `rating` | Yes | 1–5 |
| `message` | No | Free text |
| `senderName` / `senderEmail` | No | Only needed if the person wants a reply |

Returns `201` with no body — this is a submit-and-forget form, nothing needs to be shown back to the user beyond a 
confirmation message on the frontend.

`eventTitle` is deliberately free text rather than a foreign key to a specific event: the form doesn't have an event 
picker, and a hard link would mean feedback disappears if that event is later removed — the feedback itself stays 
historically valid regardless.

## Admin review

**`GET /api/admin/feedback`**

| Query param | Default | Notes |
|---|---|---|
| `page` | 0 | Zero-based |
| `size` | 9 | 1–100 |

Returns feedback ordered newest first, each with `id`, `topic`, `eventTitle`, `rating`, `message`, `senderName`, 
`senderEmail`, `isReviewed`, `createdAt`.

**`PATCH /api/admin/feedback/{id}`**
```json
{ "isReviewed": true }
```
Toggles the reviewed flag. `PATCH` rather than `PUT`, since this changes one field, not the whole record.

## Notifications

When feedback is submitted, a background job (`NotificationFeedbackScanService`, running every ~10 seconds) 
creates a `NEW_FEEDBACK` notification for admins — this is the only notification type admins receive; 
see `docs/notifications.md`.

## Errors

| Status | When |
|---|---|
| `400` | Invalid request body (rating out of range, missing `topic`) |
| `404` | Feedback id not found (on `PATCH`) |

## Architecture

`FeedbackController` (public) / `AdminFeedbackController` (admin) → `FeedbackService` → `FeedbackRepository`

Split into two controllers under the same `Feedback` tag, rather than one controller with mixed access levels — 
matches the project's convention of admin-only operations living under `/api/admin/...`, so the access level is visible 
from the path itself.