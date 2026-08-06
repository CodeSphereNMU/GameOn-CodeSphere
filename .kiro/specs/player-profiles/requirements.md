# Player Profiles & Sports - Requirements

## Overview

After registration, players build their profile by adding sports with skill levels. Profiles display a player's information, sports, stats, and are viewable by other users. This covers use cases D200 (Manage User Profile), D300 (Add Sport to Profile), and D400 (View User Profile / Follow-Unfollow).

## Implementation Status

| Component | Status |
|-----------|--------|
| Schema: `users`, `sport`, `sport_format`, `position`, `format_position`, `user_sport_profile`, `follow` | Schema-supported (V1) |
| Seed data: 5 sports, 15 formats, 25 positions, format-position links | Schema-supported (V2) |
| `Sport` model | Implemented |
| `SportFormat` model | Implemented |
| `Position` model | Implemented |
| `SportDao` (findAll, findBySportId) | Implemented |
| `SportFormatDao` | Implemented |
| `PositionDao` | Implemented |
| `FollowDao` (findMutualFollowerIds) | Implemented |
| `UserSportController` (GET /api/users/me/sports) | Implemented |
| `SportController` (formats, positions) | Implemented |
| `FriendController` (GET /api/users/me/friends) | Implemented |
| Profile viewing page | Not implemented |
| Profile editing (username change) | Not implemented |
| Add/remove sport from profile page | Not implemented |
| Follow/unfollow UI | Not implemented |
| ProfileService | Not implemented |
| SportService (add/remove/update sports) | Not implemented |
| ProfileController (profile routes) | Not implemented |
| Unit tests for profile/sport services | Not implemented |

Note: Several DAOs and controllers listed above were built to support A100 (Create Game Listing) and are reusable by the Player Profiles use cases. They are not "Player Profiles implementation" — they are shared foundation.

## Supported Sports

The five confirmed sports (seeded in V2):
- Padel
- Tennis
- Basketball
- Rugby
- Football

Do not reference Soccer, Cricket, Hockey, or Badminton. These were considered during early design and rejected.

## Functional Requirements

### REQ-PROF-1: View Own Profile

**As a** registered user,
**I want to** view my own profile,
**so that** I can see how my profile appears and review my information.

**Acceptance Criteria:**
- Profile displays: username, sports with skill levels, win/loss stats per sport, follower/following counts.
- User can access their profile from the navigation.
- Profile shows the user's posts (links to post detail or list) — deferred until Posts are implemented.

### REQ-PROF-2: Update Username

**As a** registered user,
**I want to** change my username,
**so that** I can update my display identity.

**Acceptance Criteria:**
- New username must meet the same validation rules as registration (pending group decision).
- System checks for duplicates before saving.
- On conflict, show clear error message.
- On success, username is updated.

### REQ-PROF-3: Add Sport to Profile

**As a** registered user,
**I want to** add a new sport to my profile,
**so that** I can participate in listings for that sport.

**Acceptance Criteria:**
- System displays the five supported sports not already on the user's profile.
- User selects a sport and a skill level (Beginner, Intermediate, Advanced).
- On confirmation, a row is added to `user_sport_profile`.
- The sport appears on the user's profile.
- The user can now create/join listings for this sport.

### REQ-PROF-4: Remove Sport from Profile

**As a** registered user,
**I want to** remove a sport from my profile,
**so that** I no longer appear in listings for that sport.

**Acceptance Criteria:**
- System confirms the removal before proceeding.
- On removal, the row is deleted from `user_sport_profile`.
- User can no longer create/join listings for the removed sport.

**Unresolved:**
- Whether removal should be blocked while the user has a pending join request or accepted place in an upcoming/confirmed listing for that sport. This was proposed as a resolution in earlier documentation but was not supplied as a confirmed business rule. It requires a group decision.
- Whether a user must retain at least one sport on their profile. Do not assume either answer.

### REQ-PROF-5: Update Skill Level

**As a** registered user,
**I want to** update my skill level for a sport,
**so that** I'm matched with appropriate players.

**Acceptance Criteria:**
- User selects an existing sport on their profile.
- User chooses a new skill level (Beginner, Intermediate, Advanced).
- System updates the `user_sport_profile` row.

### REQ-PROF-6: View Another User's Profile

**As a** registered user,
**I want to** view another player's profile,
**so that** I can see their sports, stats, and posts.

**Acceptance Criteria:**
- Profile displays: username, sports with skill levels, win/loss stats, follower/following counts, recent posts (when posts are implemented).
- Shows a Follow/Unfollow button based on current relationship.

**Unresolved:** Who may view another user's full profile. Do not assume full public visibility.

### REQ-PROF-7: Follow/Unfollow

**As a** registered user,
**I want to** follow or unfollow another player,
**so that** I can stay connected with players I like.

**Acceptance Criteria:**
- Clicking Follow adds a row to the `follow` table.
- Clicking Unfollow removes the row.
- Follower/following counts update on both profiles.
- The followed user receives a notification (deferred to notifications feature).
- A user cannot follow themselves.
- Mutual follows establish "friend" status (used for game invitations).

## Non-Functional Requirements

- Profile retrieval should be fast (single query or minimal joins).
- Sport list and skill levels are data-driven from the `sport` table, not hardcoded.
- The `sport` table does not have a `noPlayers` column. Capacity belongs to `sport_format.no_players`.

## Unresolved Questions

1. **Last-sport rule:** Can a user remove their last sport? Not decided.
2. **Profile visibility:** Who may view another user's full profile? Not decided.
3. **Win/loss calculation:** Derived from MatchResult on-the-fly, or cached in `user_sport_profile`? Not decided.
4. **Profile images/avatars:** Not in FSSB data model. Deferred unless time allows.
