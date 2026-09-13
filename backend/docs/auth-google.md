# Google Sign-In — Backend documentation

## Overview

Alternative login using Google OAuth 2.0 / OpenID Connect, alongside the existing email/password flow. Implemented as 
a server-side redirect flow — the entire exchange (authorization code, access token, user info) happens between our 
backend and Google. The frontend needs nothing beyond a plain link; no JS SDK, no token handling on the client.

## API

| Method / path | Auth | What it does |
|---|---|---|
| `GET` `/api/auth/google` | Public | Redirects the browser to Google's consent screen |
| `GET` `/api/auth/google/callback` | Public | Called by Google after sign-in; completes the flow |

Neither endpoint is meant to be called via `fetch`/XHR — both work through full-page browser navigation, since OAuth 
redirects can't be intercepted by client-side JavaScript.

## Flow

1. User clicks a plain link: `<a href="/api/auth/google">Continue with Google</a>`
2. Backend generates a random `state` value, stores it in a short-lived (5 min), `HttpOnly` cookie, and redirects to 
Google's authorization URL
3. User signs in and consents on Google's own page
4. Google redirects back to `/api/auth/google/callback?code=...&state=...`
5. Backend verifies the returned `state` matches the cookie (CSRF protection) — the cookie is cleared immediately after 
this check, whether it matches or not
6. Backend exchanges the authorization `code` for an access token (server-to-server call to Google, using the client 
secret — never exposed to the browser)
7. Backend fetches the user's email and name from Google's userinfo endpoint
8. **`email_verified` is checked before anything else happens.** If Google reports the email as unverified, the flow is 
rejected — an unverified email must never be trusted as a stable identifier, since it could otherwise be used to hijack 
an existing password-based account with the same address
9. Backend finds an existing user by email, or creates a new one (`password_hash` is `null` for Google-only accounts)
10. A normal session is created — the exact same mechanism as `docs/auth.md`, reusing `createSessionAndBuildResult`
11. Browser is redirected back to the frontend, session cookie already set

## Response shape

There's no JSON response from either endpoint — both always redirect. On success, the browser lands back on the 
frontend with the session cookie set; the frontend then calls `GET /api/users/me` (the same call it would make after 
any other login) to get the signed-in user's data.

On failure, the browser is redirected to `/login?error=invalid_state` or `/login?error=google_auth_failed` instead.

## Errors

| Redirect target | When |
|---|---|
| `/login?error=invalid_state` | The `state` cookie is missing or doesn't match — possible CSRF attempt |
| `/login?error=google_auth_failed` | Google's token exchange or userinfo call failed, or the email wasn't verified |
| `/login?error=unexpected_error` | Any other unhandled failure during the flow |

## Architecture

`GoogleAuthController` → `GoogleAuthService` → `AuthService.loginOrRegisterFromGoogle(...)` 
→ `AuthService.createSessionAndBuildResult(...)`

`GoogleAuthService` is the only place that talks to Google directly — token exchange and userinfo calls are both plain 
server-to-server HTTP requests, not a third-party OAuth library.

## Notes

- Google-authenticated accounts (`password_hash = null`) are rejected cleanly (not with a server error) if they ever 
attempt the regular email/password login.