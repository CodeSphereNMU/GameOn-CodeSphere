# Game Listings - Requirements (A100: Create Game Listing)

## Overview

Game Listings are the core feature of GameOn. This spec covers use case A100 — Create Game Listing. A listing represents an upcoming sports session that needs players. The creator specifies the sport, format, skill level, date/time, location, privacy, team, preferred positions, and optionally invites friends.

## Schema Status

The current database has V1 (schema) and V2 (seeds) applied. V3 has been written and is pending review/application. The code in this branch targets the V3 schema. The application will not function against GameOnDB until V3 is applied.

## Confirmed Business Rules (A100)

### Listing Lifecycle
- A new listing is created with `status = 'OPEN'`.
- Valid lifecycle statuses: OPEN, CONFIRMED, CANCELLED_INSUFFICIENT_PLAYERS, CANCELLED_BY_CREATOR, COMPLETED.
- Confirmation, expiry, cancellation, and completion logic belong to later use cases.
- The `is_completed` column has been replaced by the `status` column (V3 migration).

### Multiple Listings
- A user may create and participate in multiple upcoming listings.
- There is no one-active-listing restriction.
- The only constraint is scheduling: session windows and travel buffers must not overlap.

### Session Duration and End Time
- The creator enters only the session start date and time.
- Game On automatically calculates end_time = start + sport_format.duration_minutes.
- `end_time` is persisted in game_listing for immutability (unaffected by later format duration changes).
- The UI displays the session window (e.g. "18:00–19:00").
- The 60-minute travel buffer is not included in the displayed session window.

### Scheduling Conflicts
- A 60-minute travel buffer applies after the calculated session end time.
- A user may not create a listing if the new session conflicts with any listing where they are an accepted participant (creator or joiner).
- Conflict = overlap between `[session_start, session_end + 60 min]` zones of two sessions.
- Equality at the boundary is allowed (not a conflict).
- Only listings with status OPEN or CONFIRMED are considered.
- Cancelled and completed listings are excluded.
- The scheduling check is performed transactionally during creation.

### Minimum Lead Time
- A listing must be created at least 3 hours before its scheduled start time.
- Exactly 3 hours ahead is allowed.

### Sports
- User must be logged in and have at least one sport in `user_sport_profile`.
- If the user has no sports, UI prevents creation and explains they must add a sport first.

### Formats and Capacity
- Formats filtered by selected sport.
- Capacity derived from `sport_format.no_players` (not manually entered).
- `sport_format.duration_minutes` provides the session duration.
- Creator occupies one player space on their selected team.
- Each team has a fixed capacity of `no_players / 2`.

### Creator Team Selection
- The creator must select Team A or Team B.
- The selection is stored in the creator's `game_joiner` record.
- The creator's team cannot be changed after publication.

### Positions
- Only shown when `sport_format.has_positions = true`.
- Positional format rules:
  - At least one specific position is required, OR "Any Position" selected alone.
  - "Any Position" is mutually exclusive with specific positions.
  - A second specific position is optional.
  - Both positions must be valid for the selected format.
  - Duplicate position selections are rejected.
- Non-positional formats: position input is silently ignored/stored as null.
- These rules apply to both the creator and future join-request applicants.
- "Any Position" is represented as NULL position_id in game_joiner for a positional format.

### Creator Participation
- Creator is the first participant with `status = 'ACCEPTED'` (uppercase).
- A `game_joiner` row is created with team, positions, and `join_request_id = NULL`.

### Friends and Invitations
- "Friends" = mutual followers (each user follows the other).
- Inviting is optional (0 invitations allowed).
- Invitations are courtesy invitations — they do not reserve capacity.
- No capacity-based invitation limit. The creator may invite more people than available places.
- Prevent: duplicate friend IDs, self-invitation, non-friends.
- A PENDING invitation record is inserted into the `invitation` table for each invitee.
- A corresponding notification is inserted for each invitee.
- Both invitation records and notifications are part of the atomic transaction.
- Post-creation invitations are deferred and must not be implemented.

### Privacy
- Existing `is_private` bit: Public = false, Private = true.
- UI defaults to Public.
- Privacy cannot be changed after publication.

### Skill Level
- Valid values: Beginner, Intermediate, Advanced.
- Backend validates the value.

### Transaction
- Creating `game_listing`, creator `game_joiner`, invitation records, and notification records must be atomic.

## Functional Requirements

### REQ-LIST-1: Create Game Listing

**As a** player with at least one sport on their profile,
**I want to** create a game listing for that sport,
**so that** other players can find and join my game.

**Acceptance Criteria:**
1. User selects: privacy, sport, format, skill level, date, time, location, team.
2. If format has positions, user selects position preference(s) or "Any Position".
3. User may optionally invite mutual friends (no capacity limit).
4. Confirmation screen shows summary with session window; user submits.
5. System validates all inputs and scheduling conflicts.
6. On success: listing created (status=OPEN, end_time calculated), creator added as ACCEPTED game_joiner on selected team, PENDING invitations inserted, notifications sent.
7. On failure: appropriate error returned; nothing persisted (transaction rollback).

### Validation Rules (POST /api/game-listings)
- Authenticated session required.
- User has at least one registered sport.
- Selected sport belongs to user's profile.
- Selected format belongs to selected sport.
- Valid skill level (Beginner/Intermediate/Advanced).
- Valid team selection (A or B).
- Non-blank location.
- Future date/time.
- Minimum 3 hours before start time.
- No scheduling conflict (session + 60-min buffer overlap with any OPEN/CONFIRMED listing where user is accepted).
- Valid position choices (if applicable).
- Valid mutual-friend invitation IDs.
- No duplicate invitees or self-invitation.

## API Endpoints (A100)

| Method | Path | Purpose |
|--------|------|---------|
| GET | /api/users/me/sports | User's registered sports |
| GET | /api/sports/{sportId}/formats | Formats for a sport (includes durationMinutes) |
| GET | /api/formats/{formatId}/positions | Positions for a format |
| GET | /api/users/me/friends | Mutual followers |
| POST | /api/game-listings | Create a new listing |
