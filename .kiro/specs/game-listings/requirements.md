# Game Listings - Requirements

## Overview

Game Listings are the core feature of GameOn. A listing represents an upcoming sports session that needs players. Creators specify the sport, format, skill level, date/time, location, and capacity. Other players browse, filter, and request to join listings. This covers use cases A100 (Create Game Listing), A200 (Browse Listings), A300 (Send Join Request), A400 (Leave Game Listing), A500 (Hide Expired Listings), A600 (Send Game Reminders), A700 (Confirm Session), C300 (Manage Game Listing), and C500 (View Join Requests).

## Functional Requirements

### REQ-LIST-1: Create Game Listing

**As a** player with at least one sport on their profile,  
**I want to** create a game listing for that sport,  
**so that** other players can find and join my game.

**Acceptance Criteria:**
- User selects: sport (from their profile sports), format, skill level, date, time, location, privacy setting (public/private).
- Sport formats define team sizes, number of players needed, and whether positions apply.
- If the format has positions, user may select up to 2 preferred positions.
- User may optionally invite other users during creation.
- **Pending decision:** How "friends" are defined for invitation purposes (proposed: users they follow). See unresolved questions.
- System validates: date must be in the future, user does not already have an active listing.
- On success, listing is created with status "active" and appears in browse results.
- Invited friends receive a notification.

### REQ-LIST-2: Browse Game Listings

**As a** registered user,  
**I want to** browse available game listings,  
**so that** I can find games to join.

**Acceptance Criteria:**
- By default, shows active public listings for sports on the user's profile.
- User can filter by: sport, skill level, date, time.
- Expired, cancelled, or full listings are not shown.
- Private listings are not shown in browse results.
- **Pending decision:** How users gain access to private listings (invite only, direct link, or both). See unresolved questions.
- Each listing card displays: sport, format, skill level, date/time, location, spots remaining, creator.

### REQ-LIST-3: View Listing Detail

**As a** registered user,  
**I want to** view the full details of a listing,  
**so that** I can decide whether to join.

**Acceptance Criteria:**
- Shows all listing information plus current team rosters (Team A, Team B).
- Shows positions assigned if applicable.
- Shows number of spots remaining per team.
- Shows listing status (active, confirmed, completed, expired, cancelled).
- Shows a "Join Team" button for each team if spots are available and user is eligible.

### REQ-LIST-4: Send Join Request

**As a** registered user,  
**I want to** request to join a game listing,  
**so that** I can be added to the game.

**Acceptance Criteria:**
- User selects which team to join.
- If the format has positions, user selects up to 2 preferred positions.
- System validates: user has the required sport on profile, user is not already in this listing, scheduling conflict check (not within 3 hours of another joined listing).
- Join request is stored with status "pending".
- Listing creator receives a notification.

### REQ-LIST-5: View and Manage Join Requests (Creator)

**As a** listing creator,  
**I want to** view join requests for my listing and accept or reject them,  
**so that** I can control who plays in my game.

**Acceptance Criteria:**
- Creator sees a list of pending requests with: requester username, requested team, preferred positions.
- Creator can accept or reject each request individually.
- On accept: user is added to the listing roster with their assigned position (or one of their preferred positions).
- On reject: request status changes to "rejected"; requester is notified.
- Once a team is full, no more requests can be accepted for that team.

### REQ-LIST-6: Leave Game Listing

**As a** participant in a listing,  
**I want to** leave the listing before the game,  
**so that** my spot opens for someone else.

**Acceptance Criteria:**
- User clicks "Leave" on their joined listing.
- User is removed from the roster.
- Listing available spots update.
- Creator is notified of the departure.
- **Open question:** Can a user leave after confirmation (within 2 hours of game time)?

### REQ-LIST-7: Manage Game Listing (Creator)

**As a** listing creator,  
**I want to** update or delete my listing,  
**so that** I can correct details or cancel the game.

**Acceptance Criteria:**
- Creator can update: date, time, location, skill level (within reason).
- Creator can delete/cancel the listing.
- On cancellation: all participants are notified; listing status changes to "cancelled".
- **Open question:** Can the sport or format be changed if players have already joined?

### REQ-LIST-8: Hide Expired Listings (System)

**As the** system,  
**I want to** hide listings whose scheduled time has passed,  
**so that** users only see relevant upcoming games.

**Acceptance Criteria:**
- Listings past their scheduled date/time are not returned in browse queries.
- Expired listings are not deleted; they remain for match result recording and history.
- Implementation: filter by status and date in browse queries (not necessarily a background job).

### REQ-LIST-9: Confirm Session (System)

**As the** system,  
**I want to** confirm a listing 2 hours before the scheduled time,  
**so that** participants are locked in and reminded.

**Acceptance Criteria:**
- When a listing is full AND the current time is within 2 hours of the scheduled time, mark it as "confirmed".
- All participants receive a reminder notification.
- **Pending decision:** Whether participants are "locked in" after confirmation (cannot leave). The FSSB states locking but the practical meaning needs group confirmation. See unresolved questions.
- **Open question:** What if the listing is NOT full 2 hours before? Does it still confirm? Cancel? Stay active?

### REQ-LIST-10: Game Reminders (System)

**As a** participant,  
**I want to** receive a reminder notification 2 hours before my game,  
**so that** I remember to attend.

**Acceptance Criteria:**
- Notification is sent to all participants in a confirmed session.
- Notification includes: sport, location, time.
- **Implementation note:** Can be triggered on app interaction (checking if reminders are due) rather than requiring a background scheduler, depending on group decision.

## Non-Functional Requirements

- Browse queries should be efficient (indexed on sportId, date, status).
- Listing creation should prevent race conditions on the "one active listing" rule.

## Unresolved Questions

1. **Listing lifecycle states and exact transitions:** Active → Confirmed → Completed? What about Cancelled, Expired? Can a listing go from Active to Expired without being Confirmed?
2. **What happens if a listing isn't full 2 hours before?** Confirm anyway? Leave active? Auto-cancel?
3. **Can a user leave after session confirmation?** FSSB says users are "locked in" but what does that mean practically?
4. **Team allocation:** Are teams always "Team A" and "Team B"? Or can names be customised?
5. **Position assignment:** Creator picks final position, or requester's preference is honoured?
6. **Background jobs:** Are time-based events (reminders, expiry, confirmation) triggered by a scheduler, or lazily evaluated on request?
7. **Private listings:** How are they accessed? Direct link? Invite only? Both?
8. **Scheduling conflict:** The 3-hour rule applies to the scheduled start time. What about duration?
