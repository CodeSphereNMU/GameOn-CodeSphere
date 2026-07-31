# Authentication & Account Access - Requirements

## Overview

Users must be able to register for a new account, log in, log out, and have their identity maintained across requests via server-side sessions. This covers use cases D100 (Register User) and the shared Login/Logout feature.

## Functional Requirements

### REQ-AUTH-1: User Registration

**As a** visitor,  
**I want to** create a new account with a unique username and password,  
**so that** I can access GameOn features.

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

**Acceptance Criteria (confirmed by FSSB):**
- The FSSB registration flow includes a sport selection step.
- System displays available sports.
- For each selected sport, user chooses a skill level (Beginner, Intermediate, Advanced).
- The FSSB states the user selects a sport and skill level during registration (D100 step 6-9).

**Pending decisions (do not implement until group confirms):**
- Whether sport selection is mandatory immediately after registration or can be deferred.
- Whether the user is blocked from accessing any features until at least one sport is selected.
- Whether the user can skip sport selection and add sports later from their profile.

### REQ-AUTH-3: User Login

**As a** registered user,  
**I want to** log in with my username and password,  
**so that** I can access my account.

**Acceptance Criteria (confirmed by FSSB):**
- User provides username and password.
- System validates credentials against stored password (plain text comparison).
- On success, a session is created and the user gains access to the application.
- On failure, the system does not reveal which field (username or password) was incorrect.

### REQ-AUTH-4: User Logout

**As a** logged-in user,  
**I want to** log out,  
**so that** my session is terminated and my account is secured.

**Acceptance Criteria (confirmed by FSSB):**
- User clicks logout.
- The user's session is invalidated.
- User can no longer access protected resources without logging in again.

### REQ-AUTH-5: Session Persistence

**As a** logged-in user,  
**I want** my session to persist across page navigations,  
**so that** I don't have to log in on every page.

**Acceptance Criteria (general expectation):**
- Session persists until logout or expiry.
- Expired sessions require the user to log in again.

**Pending decisions (do not implement until group confirms):**
- Session storage approach (Jetty in-memory sessions vs DB table).
- Session expiry duration.
- Cookie configuration (HttpOnly, Secure flags, SameSite).

### REQ-AUTH-6: Route Protection

**As the** system,  
**I want to** protect API routes that require authentication,  
**so that** unauthenticated users cannot access protected resources.

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

1. **Session storage:** Jetty in-memory sessions or custom DB session table?
2. **Session expiry duration:** How long before a session expires?
3. **Registration auto-login:** Does registration automatically start a session?
4. **Sport selection timing:** Mandatory immediately or deferrable?
5. **Password requirements:** Minimum length? Complexity rules?
6. **Username restrictions:** Character set? Length limits?
7. **Public routes:** Which endpoints work without authentication?
8. **"Remember me":** In scope or deferred?
9. **Email/password recovery:** Not in FSSB. Out of scope?
