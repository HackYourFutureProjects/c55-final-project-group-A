# Password Recovery & Change — Backend documentation

## Overview

Two separate flows for two different situations. Forgot-password/reset is for a user who is signed out and doesn't 
remember their password — it works through a time-limited link sent by email. Change-password is for a user who is 
already signed in and knows their current password — it's a direct action, no email involved.

## API

| Method / path | Auth | What it does |
|---|---|---|
| `POST` `/api/auth/forgot-password` | Public | Sends a reset link by email, if the account exists |
| `GET` `/api/auth/reset-password/validate` | Public | Checks whether a reset token is still usable |
| `POST` `/api/auth/reset-password` | Public | Sets a new password using a valid token; signs the user in |
| `PUT` `/api/auth/password` | Authenticated | Changes the password for the currently signed-in user |

## Forgot password / reset flow

**`POST /api/auth/forgot-password`**
```json
{ "email": "anouk@example.com" }
```
Always returns `204`, regardless of whether the email is registered, or whether the rate limit has already been hit. 
The only observable difference between "no such account," "already rate-limited," and "email sent" is whether a message 
actually arrives — never the API response. This is deliberate: revealing the difference would let an attacker use this 
endpoint to discover which emails have accounts (account enumeration).

Rate limit: 2 requests per account per 24 hours.

If the account exists and the limit isn't hit, a reset token is generated, stored hashed (same principle as session 
tokens — the raw token is never persisted), and emailed as a link:
```
{frontend}/reset-password?token=<raw-token>
```
The token expires after 15 minutes and can only be used once (`used_at` is set on consumption).

**`GET /api/auth/reset-password/validate?token=...`**
```json
{ "valid": true }
```
Lets the frontend check the link before showing the reset form, instead of letting the user fill it out only to find 
out the link expired.

**`POST /api/auth/reset-password`**
```json
{ "token": "...", "newPassword": "..." }
```
On success, returns the same `AuthResponse` shape as register/login/Google sign-in, with the session cookie already 
set — the user is signed in automatically, no separate login step needed. Before that: the password is updated, the 
token is marked used, and **every existing session for that account is invalidated** — if the account was compromised 
and someone else was signed in, this signs them out too.

Errors: `400` if the token is invalid, expired, or already used.

## Change password (signed-in user)

**`PUT /api/auth/password`**
```json
{ "currentPassword": "...", "newPassword": "..." }
```
Requires the current password to match. On success (`204`), all *other* active sessions for the account are invalidated
— but the session making this request stays alive, so the user isn't logged out mid-action. This is different from the 
reset flow above: reset doesn't know which session (if any) is "the real user," so it clears all of them; 
change-password does know, so it only clears the others.

Errors: `400`/`401` if the current password is wrong.

## Email delivery

Sent via Gmail SMTP (an app password, not the full OAuth flow) rather than a third-party transactional email provider 
— providers like Resend or Mailgun require verifying a sending domain to deliver outside a sandbox, which wasn't 
practical for a subdomain we don't control the DNS for. Sending as a Gmail address sidesteps that.

## Architecture

`PasswordResetController` → `PasswordResetService` → `PasswordResetTokenRepository` / `EmailService`

`PUT /api/auth/password` lives in `AuthController` / `AuthService` directly, alongside register/login/logout — it's a 
small, self-contained addition to the existing account-credentials logic, not part of the token-based reset flow.