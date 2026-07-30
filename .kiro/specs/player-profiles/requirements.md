# Player Profiles & Sports - Requirements

## Overview

After registration, players build their profile by adding sports with skill levels. Profiles display a player's information, sports, stats, and are viewable by other users. This covers use cases D200 (Manage User Profile), D300 (Add Sport), and D400 (View User Profile / Follow/Unfollow).

## Functional Requirements

### REQ-PROF-1: View Own Profile

**As a** registered user,  
**I want to** view my own profile,  
**so that** I can see how my profile appears and review my information.

**Acceptance Criteria:**
- Profile displays: username, sports with skill levels, win/loss stats per sport, follower/following counts.
- User can access their profile from the navigation.
- Profile shows the user's posts (links to post detail or list).

### REQ-PROF-2: Update Username

**As a** registered user,  
**I want to** change my username,  
**so that** I can update my display identity.

**Acceptance Criteria:**
- New username must meet the same validation rules (3-20 chars, unique, alphanumeric/underscore).
- System checks for duplicates before saving.
- On conflict, show clear error message.
- On success, username is updated everywhere.

### REQ-PROF-3: Add Sport to Profile

**As a** registered user,  
**I want to** add a new sport to my profile,  
**so that** I can participate in listings for that sport.

**Acceptance Criteria:**
- System displays sports not already on the user's profile.
- User selects a sport and a skill level (Beginner, Intermediate, Advanced).
- On confirmation, the sport+skill entry is added to `UserSportProfile`.
- The sport appears on the user's profile.
- The user can now create/join listings for this sport.

### REQ-PROF-4: Remove Sport from Profile

**As a** registered user,  
**I want to** remove a sport from my profile,  
**so that** I no longer appear in listings for that sport.

**Acceptance Criteria:**
- User cannot remove their last sport (at least one must remain).
- System confirms the removal before proceeding.
- On removal, the sport entry is deleted from `UserSportProfile`.
- User can no longer create/join listings for the removed sport.
- **Open question:** What happens to active listings for a removed sport?

### REQ-PROF-5: Update Skill Level

**As a** registered user,  
**I want to** update my skill level for a sport,  
**so that** I'm matched with appropriate players.

**Acceptance Criteria:**
- User selects an existing sport on their profile.
- User chooses a new skill level.
- System updates the `UserSportProfile` entry.

### REQ-PROF-6: View Another User's Profile

**As a** registered user,  
**I want to** view another player's profile,  
**so that** I can see their sports, stats, and posts.

**Acceptance Criteria:**
- Profile displays: username, sports with skill levels, win/loss stats, follower/following counts, recent posts.
- Shows a Follow/Unfollow button based on current relationship.

### REQ-PROF-7: Follow/Unfollow

**As a** registered user,  
**I want to** follow or unfollow another player,  
**so that** I can stay connected with players I like.

**Acceptance Criteria:**
- Clicking Follow adds an entry to the `Follow` table.
- Clicking Unfollow removes the entry.
- Follower/following counts update on both profiles.
- The followed user receives a notification (deferred to notifications feature).
- A user cannot follow themselves.

## Non-Functional Requirements

- Profile retrieval should be fast (single query or minimal joins).
- Sport list and skill levels should be data-driven (from `Sport` table, not hardcoded).

## Unresolved Questions

1. **Profile images/avatars:** Not explicitly in FSSB data model. Deferred or add later?
2. **What happens to active listings when a user removes a sport?** Auto-leave? Block removal?
3. **Win/loss calculation:** Derived from MatchResult on-the-fly, or cached in `UserSportProfile`?
4. **Public vs private profiles:** Not mentioned in FSSB. Assume all profiles are public?
