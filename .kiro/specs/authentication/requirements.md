# Authentication & Account Access - Requirements

## Overview

Users must be able to register for a new account, log in, log out, and have their identity maintained across requests via server-side sessions. This covers use cases D100 (Register User) and the shared Login/Logout feature.

## Implementation Status

The authentication spec covers both the login/session foundation (implemented) and D100 — Register user (not implemented). These are separated below.

### Login and Session Foundation (Implemented)

| Component | Status |
|-----------|--------|
| `POST /api/auth/login` | Implemented |
| `GET /api/auth/me` | Implemented |
| Session creation (Javalin/Jetty in-memory) | Implemented |
| Login page (`index.html`) | Implemented |
| `login.js` | Implemented |
| Dashboard with session check | Implemented |
| AuthService (login logic) | Implemented |
| AuthServiceTest (unit tests) | Implemented and passing |
| User model and UserDao | Implemented |

### D100 — Register user (Not Implemented)

| Component | Status |
|-----------|--------|
| `POST /api/auth/register` | Not implemented |
| Registration page (`register.html`) | Not implemented (Sign Up link is broken) |
| Registration sport selection | Not implemented |

### Other Pending Authentication Work

| Component | Status |
|-----------|--------|
| `POST /api/auth/logout` | Not implemented |
| Global authentication middleware | Not implemented |
| Session expiry | Not decided or configured |
| Public-route policy | Not decided |

The `users` table was established in V1. It uses `BIGINT` identifiers, `varchar(255)` for username and password, and has a unique constraint on `username`.

## Functional Requirements

### REQ-AUTH-1: User Registration

**As a** visitor,
**I want to** create a new account with a unique username and password,
**so that** I can access GameOn features.

**Status:** Not implemented.

**Acceptance Criteria (confirmed by FSSB):**
- User provides: username, password, confirm password.
- Username must be unique (case-insensitive).
- Password and confirm password must match.
- On success, a new user account is created and stored in the database.
- On failure, clear error messages are shown (e.g., "Username is already taken").

**Pending decisions (do not implement until group confirms):**
- Username length and character restrictions (proposed: 3-20 chars, alphanumeric + underscores).
- Password minimum length and complexity rules (proposed: at least 8 characters).
- Whether registration automatically starts a session (logs the user in) or requires a separate login step.
- Where the user is redirected after successful registration.

### REQ-AUTH-2: Sport Selection During Registration

**As a** newly registered user,
**I want to** select at least one sport and skill level,
**so that** I can participate in listings.

**Status:** Not implemented.

**Acceptance Criteria (confirmed by FSSB):**
- The FSSB registration flow includes a sport selection step.
- System displays the five supported sports: Padel, Tennis, Basketball, Rugby, Football.
- For each selected sport, user chooses a skill level (Beginner, Intermediate, Advanced).
- The FSSB states the user selects a sport and skill level during registration (D100 steps 6-9).

**Pending decisions (do not implement until group confirms):**
- Whether sport selection is mandatory immediately after registration or can be deferred.
- Whether the user is blocked from accessing any features until at least one sport is selected.
- Whether the user can skip sport selection and add sports later from their profile.

### REQ-AUTH-3: User Login

**As a** registered user,
**I want to** log in with my username and password,
**so that** I can access my account.

**Status:** Implemented. Unit tested. Manually verified.

**Acceptance Criteria (confirmed by FSSB):**
- User provides username and password.
- System validates credentials against stored password (plain text comparison).
- On success, a session is created and the user gains access to the application.
- On failure, the system does not reveal which field (username or password) was incorrect.

**Implementation notes:**
- AuthService.login() validates input, looks up user by username, compares password.
- Returns the same error message for unknown username and wrong password.
- Username is trimmed before lookup.
- Session attribute `userId` is set on success.
- Login page is the application root (`index.html`), not a separate `login.html`.

### REQ-AUTH-4: User Logout

**As a** logged-in user,
**I want to** log out,
**so that** my session is terminated and my account is secured.

**Status:** Not implemented.

**Acceptance Criteria (confirmed by FSSB):**
- User clicks logout.
- The user's session is invalidated.
- User can no longer access protected resources without logging in again.

### REQ-AUTH-5: Session Persistence

**As a** logged-in user,
**I want** my session to persist across page navigations,
**so that** I don't have to log in on every page.

**Status:** Partially implemented. Sessions work via Javalin/Jetty in-memory sessions. No expiry configured.

**Acceptance Criteria (general expectation):**
- Session persists until logout or expiry.
- Expired sessions require the user to log in again.

**Current implementation:** Javalin's built-in Jetty sessions with `ctx.sessionAttribute()`. Sessions live in server memory and are lost on restart.

**Pending decisions (do not implement until group confirms):**
- Session expiry duration.
- Cookie configuration (HttpOnly, Secure flags, SameSite).
- Whether to move to a database-backed session table.

### REQ-AUTH-6: Route Protection

**As the** system,
**I want to** protect API routes that require authentication,
**so that** unauthenticated users cannot access protected resources.

**Status:** Not implemented as global middleware. Individual controllers (e.g., GameListingController) perform ad-hoc session checks.

**Acceptance Criteria (general expectation):**
- Requests without a valid session to protected endpoints receive a 401 response.
- The authenticated user's identity is available to controllers for downstream logic.

**Pending decisions (do not implement until group confirms):**
- Which specific API routes are public vs protected.
- Whether listings can be browsed publicly without authentication.
- Whether the health endpoint remains public (likely yes).

## Non-Functional Requirements

- Passwords are stored as plain text (university project requirement; hashing is prohibited).
- No plaintext passwords in API responses or log output.

## Unresolved Questions

These are tracked in `docs/unresolved-questions.md`:

1. **Session storage:** Currently using Jetty in-memory sessions. DB table alternative remains an option.
2. **Session expiry duration:** Not configured.
3. **Registration auto-login:** Does registration automatically start a session?
4. **Sport selection timing:** Mandatory immediately or deferrable?
5. **Password requirements:** Minimum length? Complexity rules?
6. **Username restrictions:** Character set? Length limits?
7. **Public routes:** Which endpoints work without authentication?

Out of scope: Email/password recovery, "Remember me" functionality.
