# Notifications — Backend documentation

## Overview

Logged-in users have a notification inbox in the app. When something relevant happens an event they care about is
canceled, an admin replies to their comment, and so on the backend creates a row they can fetch from the API. **There
is no email or push in this version**; the frontend polls the inbox and shows an unread count.

---

## Notification types

| Type              | Who receives it                                       | When it is created                                |
|-------------------|-------------------------------------------------------|---------------------------------------------------|
| `EVENT_CANCELLED` | Users who **saved or marked Going** on that event     | Admin cancels a published app event               |
| `EVENT_UPDATED`   | Users who **saved or marked Going** on that event     | Admin updates a published app event               |
| `EVENT_REMINDER`  | Users who marked **Going only** (Saved is not enough) | Scheduled job, ~24h before event start            |
| `COMMENT_REPLY`   | The user who wrote the comment                        | Admin creates the **first** reply on that comment |
| `NEW_FEEDBACK`    | Admin                                                 | Someone submits feedback via `POST /api/feedback` |

**Important:** a regular user does **not** get every notification type. They only get rows that apply to them — e.g.
event alerts only for events they saved or are going to, comment reply only on their own comment. Admin is the only one
who gets `NEW_FEEDBACK`.

Admin is treated like any other user for event notifications: they only get cancel/update/reminder if they also saved or
marked Going on that event.

---

## Who gets what (examples)

| User           | Saved "Jazz Night" | Going to "Rock Fest" | Commented on an event | Gets                                                         |
|----------------|--------------------|----------------------|-----------------------|--------------------------------------------------------------|
| Regular user A | yes                | no                   | no                    | cancel + update on Jazz Night; **no** reminder for Rock Fest |
| Regular user B | no                 | yes                  | no                    | cancel + update + reminder on Rock Fest                      |
| Regular user C | no                 | no                   | yes (on TM event)     | comment reply only when admin replies                        |
| Admin          | no                 | no                   | —                     | new feedback only (unless they also saved/going somewhere)   |

---

## Backend flow

### 1. Something happens (write path)

Business action runs first (cancel event, submit feedback, etc.). In the same request, the backend inserts a row into *
*`notification_outbox`** with:

- `type` (e.g. `EVENT_CANCELLED`)
- `resource_id` (event id, comment id, feedback id)
- `payload` JSON (`eventTitle`, `linkPath`)

The HTTP response succeeds .

**Exception:** `EVENT_REMINDER` does not use the outbox. A scheduled job writes directly to `notifications`.

### 2. Outbox workflow (background)

Every ~10 seconds:

1. Read pending outbox rows (batch of 10)
2. Resolve recipients and insert into **`notifications`**
3. Set `processed_at` on the outbox row

Copy (`title`, `body`) is built in Java when processing.

### 3. User reads inbox (read path)

**All endpoints require authentication.**

| Method | Path                              | What it does                                        |
|--------|-----------------------------------|-----------------------------------------------------|
| GET    | `/api/notifications`              | Paginated list (`read`, `readAt`, `linkPath`, etc.) |
| GET    | `/api/notifications/unread-count` | Count where `readAt` is null                        |
| POST   | `/api/notifications/{id}/open`    | Mark one as read, return it                         |
| POST   | `/api/notifications/read-all`     | Mark all as read for that user                      |

Queries are scoped to `user_id = current user`. Non-admins also filter out `NEW_FEEDBACK` on read (extra safety).
Opening another user’s notification returns 404.

---

## Triggers (where outbox is enqueued)

| Action                        | Service                                      | Notification      |
|-------------------------------|----------------------------------------------|-------------------|
| Admin cancels event           | `AdminEventService.cancel`                   | `EVENT_CANCELLED` |
| Admin updates published event | `AdminEventService.updateEvent`              | `EVENT_UPDATED`   |
| Admin first reply on comment  | `AdminEventCommentService.createAdminReply`  | `COMMENT_REPLY`   |
| Public feedback submit        | `FeedbackService.submitFeedback`             | `NEW_FEEDBACK`    |
| ~24h before start             | `NotificationReminderService` (every 15 min) | `EVENT_REMINDER`  |

When event **start time** changes, existing `EVENT_REMINDER` rows for that event are deleted so a new reminder can be
scheduled for the new time.

---

## Recipients for event notifications

send to each interested user uses **Saved UNION Going**:

- `event_attendees` (Going)
- `saved_events` (Saved)

When an event is canceled or updated, the backend creates a notification for each user who saved or marked Going on that
event.

For Reminders, it only notifies users who marked Going.

---

## API response shape (relevant fields)

Each notification includes:

- `type`, `title`, `body`, `resourceId`
- `linkPath` — e.g. `/events/{id}` or `/admin/messages` for feedback
- `read` — `true` / `false`
- `readAt` — timestamp when opened, or `null` if unread

---

## Out of scope (known, kept simple on purpose)

- **Ticketmaster cancel/update** — no scan of external API; only admin actions on events in our app feed enqueue
  cancel/update.
- **Skip notifying admin on their own comment reply** — not implemented(admin will notify it too)
- **Poison outbox row** — one failing entry can retry in the same batch; per-row error handling deferred.

---

## Simple end-to-end example

1. User marks **Going** on event `E`.
2. Admin cancels `E` → outbox row `EVENT_CANCELLED`.
3. ~10s later → `notifications` row for that user.
4. User calls `GET /api/notifications/unread-count` → `1`.
5. User calls `POST /api/notifications/{id}/open` → `read: true`, `readAt` set, Frontend navigates via `linkPath`.

