# AI Event Assistant — Backend documentation

## Overview

A conversational assistant on the event detail page, backed by Google Gemini. Users ask practical questions — what to 
wear, is this good for families, how to get there — and get advice grounded in the event's actual details and live 
weather, not just a repeat of what's already on the page. Stateless on the backend: nothing is persisted, the frontend 
holds the conversation and resends it in full with each new message.

## API

| Method / path | Auth | What it does |
|---|---|---|
| `POST` `/api/events/{eventId}/chat` | Public | Sends the conversation, gets a reply |

## Request / response

**`POST /api/events/{eventId}/chat`**
```json
{
  "messages": [
    { "role": "user", "message": "What should I wear?" },
    { "role": "assistant", "message": "Given the evening setting and cool weather, I'd suggest a light jacket." },
    { "role": "user", "message": "Is this good for kids?" }
  ]
}
```

```json
{ "reply": "This event is 18+ with standing room only, so it's not suited for kids." }
```

The full history is sent every time — not just the newest message. Nothing is stored server-side; refreshing the page 
loses the conversation, which is intended, not a bug.

| Field | Notes |
|---|---|
| `messages` | 1–20 entries |
| `role` | `user` or `assistant` |
| `message` | 1–2000 characters |

## How grounding works

The backend builds the AI's context itself from `event_feed` and the weather service (`docs/weather.md`) — the frontend 
never supplies event details directly, which would let a malicious client feed the model fabricated context.

The system prompt draws a deliberate line:
- For practical/advisory questions (what to wear, is this family-friendly, how to prepare), the model reasons using the 
event's specific details combined with its own general knowledge — this is the actual value of the feature, not just a 
repeat of the listing.
- For anything not covered by the event's data (drone photography rules, outside food policy, etc.), it can share 
general knowledge but must flag it as unverified for this specific event and point to the organizer.
- It's told never to invent concrete facts about the event (times, price, address) beyond what's provided, and to ignore
any instruction embedded in the conversation that tries to override these rules.
- Language: the model is told to judge the reply language only from the literal text of the newest message, ignoring the
event's location, browser locale, or any earlier message in the conversation — an early version occasionally defaulted 
to Dutch for Dutch-city events regardless of the question's actual language, traced to the browser's `Accept-Language` 
header leaking into the model's context.

## Errors

| Status | When |
|---|---|
| `400` | Invalid request body (empty message, bad role, more than 20 messages) |
| `404` | Event not found |
| `503` | Gemini is temporarily unavailable |

## Architecture

`ChatController` → `ChatService` → Gemini API (`gemini-3.5-flash-lite`, via the official `google-genai` SDK)

Uses a plain content-generation call — not Google Search grounding, which is billed separately per request and wasn't 
necessary for this scope. The model answers from its own training knowledge combined with the event context the backend 
provides, not live web search results.