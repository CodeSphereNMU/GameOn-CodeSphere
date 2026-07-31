# Game Listings - Requirements (A100: Create Game Listing)

## Overview

Game Listings are the core feature of GameOn. This spec covers use case A100 — Create Game Listing. A listing represents an upcoming sports session that needs players. The creator specifies the sport, format, skill level, date/time, location, privacy, preferred positions, and optionally invites friends.

## Locked Business Decisions (A100)

### Listing Lifecycle (A100 scope only)
- No `status` or `created_at` columns are added.
- Use the existing `is_completed` column. A new listing has `is_completed = false`.
- A creator may have multiple incomplete listings.
- A new listing is rejected only if the creator has an existing incomplete listing whose start time is less than 2 hours from the proposed start time (scheduling conflict).
- Exactly 2 hours apart is allowed (not a conflict).
- Completed listings never cause a conflict.
- Confirmation, expiry, cancellation, and completion belong to later use cases.

### Date and Time
- Keep the single `datetime2` column in the database.
- Frontend collects date and time separately.
- Backend combines and validates: the resulting datetime must be in the future.
- **Provisional (awaiting group confirmation):** A listing must be created at least 3 hours before its scheduled start time. Exactly 3 hours ahead is allowed. This is defined as `MINIMUM_LISTING_LEAD_TIME_HOURS = 3` and can be changed when the group decides.
- This lead-time rule is separate from the 2-hour scheduling conflict between a creator's own listings.

### Sports
- User must be logged in and have at least one sport in `user_sport_profile`.
- `GET /api/users/me/sports` returns only sports in the authenticated user's profile.
- If the user has no sports, UI prevents creation and explains they must add a sport first.

### Formats and Capacity
- Formats filtered by selected sport.
- Capacity comes from `sport_format.no_players` (not manually entered).
- Creator occupies one player space.
- Maximum invitations = `no_players - 1`.
- Display counter on listing cards and confirmation: `accepted participants / no_players` (e.g. `1/22` for a new 11v11 listing).
- The creator counts as the first accepted participant (always 1 at creation time).
- Pending invitations do not increase the participant count; only accepted game_joiner rows count.

### Positions
- Only shown when `sport_format.has_positions = true`.
- Positions from `format_position` for the chosen format.
- When positions apply, the user must explicitly select one of:
  - At least one valid position linked to the selected format; or
  - "Any Position" (an explicit user preference, not a database record).
- Choosing nothing is invalid and rejected by both frontend and backend.
- "Any Position" is not stored in the `position` or `format_position` tables — no fake IDs.
- The request DTO includes an explicit `anyPosition` boolean:
  - `anyPosition = true` with `positionId = null` → valid "Any Position" preference.
  - `anyPosition = false` (or null/omitted) with `positionId = null` → rejected.
  - `anyPosition = true` with a non-null `positionId` → rejected (mutually exclusive).
- NULL `game_joiner.position_id` for a positional format = explicitly selected "Any Position."
- NULL `game_joiner.position_id` for a non-positional format = position not applicable.
- The backend prevents an omitted selection from being silently treated as "Any."
- Creator may select up to 2 preferred positions (second is optional).
- Two specific positions: `position_id` = first preference, `alternate_format_position` = second (varchar per existing schema).
- Backend validates positions belong to format, are distinct, and do not exceed 2.
- Any position data submitted for a non-positional format is silently ignored (nulled out).
- Step skipped entirely for formats without positions.

### Creator Participation
- Creator is the first participant and occupies one capacity slot.
- A `game_joiner` row is created with `status = 'accepted'` and appropriate position values.

### Friends and Invitations
- "Friends" = mutual followers (each user follows the other).
- Inviting is optional (0 invitations allowed).
- Invitations do NOT insert into `game_joiner`; they create notifications only.
- Notification uses the existing `notification` table.
- Prevent: duplicate friend IDs, self-invitation, non-friends, exceeding capacity.

### Privacy
- Existing `is_private` bit: Public = false, Private = true.
- UI defaults to Public.
- No discovery/visibility filtering in A100 (belongs to Browse Listings).

### Skill Level
- Stored in existing `skill_level` column.
- Valid values: Beginner, Intermediate, Advanced.
- Backend validates the value.

### Listing Title
- No title column. Display as derived: sport_name + format_name.

### Transaction
- Creating `game_listing`, creator `game_joiner`, and invitation notifications must be atomic (single SQL transaction, rollback on any failure).

## Functional Requirements

### REQ-LIST-1: Create Game Listing

**As a** player with at least one sport on their profile,
**I want to** create a game listing for that sport,
**so that** other players can find and join my game.

**Acceptance Criteria:**
1. User selects: privacy (public/private), sport (from their profile), format, skill level, date, time, location.
2. If format has positions, user selects up to 2 preferred positions (or "Any Position").
3. User may optionally invite mutual friends (capacity-aware).
4. Confirmation screen shows summary; user submits.
5. System validates all inputs (see validation list below).
6. On success: listing created (`is_completed = false`), creator added as accepted game_joiner, notifications sent to invited friends.
7. On failure: appropriate error returned; nothing persisted (transaction rollback).

### Validation Rules (POST /api/game-listings)
- Authenticated session required.
- User has at least one registered sport.
- Selected sport belongs to user's profile.
- Selected format belongs to selected sport.
- Valid skill level (Beginner/Intermediate/Advanced).
- Non-blank location.
- Future date/time.
- Minimum 3 hours before start time (provisional).
- No scheduling conflict (creator has no incomplete listing within 2 hours of proposed time).
- Valid position choices (if applicable).
- Valid mutual-friend invitation IDs.
- Invitation count ≤ `no_players - 1`.

## API Endpoints (A100)

| Method | Path | Purpose |
|--------|------|---------|
| GET | /api/users/me/sports | User's registered sports |
| GET | /api/sports/{sportId}/formats | Formats for a sport |
| GET | /api/formats/{formatId}/positions | Positions for a format |
| GET | /api/users/me/friends | Mutual followers |
| POST | /api/game-listings | Create a new listing |
