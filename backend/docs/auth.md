# Authentication — Backend documentation

## Overview

Registration, login, and logout using opaque session tokens — not JWT. A session token is a 32-byte random value; 
only its SHA-256 hash is stored in the database, so a leaked database never exposes usable tokens. 
The token lives in an `HttpOnly`, `Secure` cookie, so it's never accessible to frontend JavaScript.

## API

| Method / path | Auth | What it does |
|---|---|---|
| `POST` `/api/auth/register` | Public | Creates a new account, starts a session |
| `POST` `/api/auth/login` | Public | Validates credentials, starts a session |
| `DELETE` `/api/auth/logout` | Public | Ends the current session |

## Request / response shape

**`POST /api/auth/register`**
```json
{ "name": "Anouk de Vries", "email": "anouk@example.com", "password": "..." }
```

**`POST /api/auth/login`**
```json
{ "email": "anouk@example.com", "password": "..." }
```

Both return `AuthResponse` on success — `userId`, `role`, `name`, `email`, `createdAt` — 
with the session cookie set on the response. Neither endpoint returns the token itself in 
the body; it only ever travels as an `HttpOnly` cookie.

**`DELETE /api/auth/logout`** — no body. Works even if the session is already expired or 
invalid (`permitAll`, not `authenticated`), so a stale session on the frontend can always be cleared cleanly.

## Session mechanics

- Token: 32 bytes from `SecureRandom`, base64-encoded
- Stored: SHA-256 hash only, in the `sessions` table
- Lifetime: 2 hours from creation (`access_expires_at`)
- Cookie: `session_access_token`, `HttpOnly`, `Secure`, `SameSite=Lax`, path `/`
- Every authenticated request is checked against `SessionAuthFilter`, which hashes the incoming cookie 
value and looks it up — no session found or expired means `401`

## Errors

| Status | When |
|---|---|
| `409` | Email already registered |
| `401` | Wrong email/password on login |
| `400` | Invalid request body (missing fields, malformed email) |

## Architecture

`AuthController` → `AuthService` → `UserRepository` + `SessionRepository`

`AuthService.createSessionAndBuildResult(user)` is the single place a session gets created — reused by register, 
login, Google sign-in, and password reset, so every entry point produces an identical session/cookie/response shape.

## Password storage

Passwords are hashed with Spring Security's `PasswordEncoder` before storage — plaintext passwords are never persisted 
or logged. `password_hash` is nullable at the schema level, since Google-authenticated accounts don't have one 
(see `docs/auth-google.md`).