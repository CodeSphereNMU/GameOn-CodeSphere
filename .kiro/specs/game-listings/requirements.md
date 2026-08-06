# Game Listings - Requirements (A100: Create Game Listing)

## Implementation Status

| Aspect | Status |
|--------|--------|
| Backend (service, DAO, controller) | Implemented |
| Frontend (4-step wizard) | Implemented |
| V3 migration | Applied to GameOnDB |
| Unit tests (GameListingServiceTest) | Implemented and passing |
| Manual end-to-end testing | Manually verified |
| Evidence capture | Pending |

Known checkpoint: commit `1657f27`.

### Known Frontend Gaps (do not affect A100 correctness)
- Confirmation screen does not display selected positions (they are stored correctly).
- `createListing.js` displays the same position error message in two different scenarios.
- Dashboard lacks a visible "Create Listing" link.

## Overview

Game Listings are the core feature of GameOn. This spec covers use case A100 — Create Game Listing. A listing represents an upcoming sports session that needs players. The creator specifies the sport, format, skill level, date/time, location, privacy, team, preferred positions, and optionally invites friends.

## Schema Status

V1 (schema) and V2 (seed data) were applied before development of this feature. V3 was then created to align the schema with the confirmed Game On rules. V3 was subsequently reviewed and successfully applied to GameOnDB. The application code targets this post-V3 schema.

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
- Conflict checking accounts for an existing session's complete format-derived duration.
- A 60-minute travel buffer is added after that existing session's end time.
- A proposed session starting before the end of that buffer conflicts.
- A proposed session starting exactly when the buffer ends is allowed (boundary equality is not a conflict).
- This rule applies to both creators and accepted participants when evaluating session conflicts.
- Only listings with status OPEN or CONFIRMED are considered.
- Cancelled and completed listings are excluded.
- The scheduling check is performed transactionally during creation.

### Creation Lead Time (Confirmed)
- A listing must be created at least 3 hours before its scheduled start time.
- A listing starting less than 3 hours after creation is invalid.
- Exactly 3 hours ahead is allowed.
- This rule is confirmed, implemented, and tested.

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

### Invitation Lifecycle (Confirmed Rules for Later Use Cases)
- An invitation is a courtesy invitation only.
- It does not automatically accept the invited user.
- It does not reserve capacity.
- The invited user must still submit a join request (use case A300).
- The creator must approve that request before the invited user becomes an accepted participant (use case C500).
- An invited user's join request is placed at the front of the request queue with an `Invited` tag.
- Invitation priority does not bypass eligibility, capacity, scheduling-conflict, position, or creator-approval rules.
- A user may be invited to a sport not currently on their profile.
- Before requesting to join, the invited user must add the relevant sport to their profile.
- The invitation provides access to the relevant listing but does not automatically add the sport to the profile.
- Invitations expire at lock-in.
- Invitations do not become `game_joiner` or accepted-participant records unless the creator approves the resulting join request.

### Lock-in (Confirmed Rules for Phase 4)
- Lock-in occurs 2 hours before the listing's start time.
- If the listing is full at lock-in, it is confirmed (status → CONFIRMED).
- If the listing is underfilled at lock-in, it is cancelled for insufficient players (status → CANCELLED_INSUFFICIENT_PLAYERS).
- Accepted participants receive the appropriate confirmation or cancellation notification.
- Invitations expire at lock-in.
- There is no attendance-tracking stage after lock-in.
- No withdrawal, new requests, cancellation, or edits are allowed after lock-in.

Note: The 3-hour creation lead time and 2-hour lock-in are separate rules with different purposes.

### Join Requests (Confirmed Rules for A300/C500)
- Rejected users may submit another join request.
- Pending requests do not generate a notification merely because they remain pending.
- Accepted participants count towards the format-derived capacity.

### Browsing (Confirmed Rule)
- Users may browse listings only for sports on their profiles.

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
- Minimum 3 hours before start time (confirmed rule).
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
