# GameOn - Unresolved Questions

Questions that require group discussion before implementation. These affect requirements or architecture and should not be resolved by a single person.

## Priority: High (Blocks Feature Implementation)

### 1. Session Storage Approach
**Context:** Authentication requires sessions. Two options exist.  
**Option A:** Javalin's built-in Jetty sessions (server memory). Simple, but sessions are lost on restart.  
**Option B:** Custom `[Session]` table in SQL Server. Survives restarts, more work to implement.  
**Affects:** Authentication spec (REQ-AUTH-5).

### 2. Session Expiry Duration
**Context:** How long should a session last before requiring re-login?  
**Options:** 24 hours, 7 days, configurable, never (until logout).  
**Affects:** Authentication spec (REQ-AUTH-5).

### 3. Password and Username Validation Rules
**Context:** The FSSB does not specify character limits or complexity rules for usernames or passwords.  
**Proposed (not confirmed):** Username 3-20 chars alphanumeric+underscore; password min 8 chars.  
**Group must decide:** Actual limits before implementation.  
**Affects:** Authentication spec (REQ-AUTH-1).

### 4. Registration Auto-Login
**Context:** Does successful registration automatically start a session, or must the user log in separately?  
**Proposed:** Auto-login after registration (smoother UX).  
**Affects:** Authentication spec (REQ-AUTH-1).

### 5. Sport Selection Timing
**Context:** FSSB shows sport selection as part of registration (D100 steps 6-9). But is it mandatory immediately, or can a user postpone it?  
**Option A:** Mandatory immediately — user cannot access the app without at least one sport.  
**Option B:** Optional — user can skip and add sports later from their profile.  
**Affects:** Authentication spec (REQ-AUTH-2), Player Profiles.

### 6. Which API Routes Require Authentication
**Context:** Should all endpoints be protected, or can some (e.g., browse listings) work without login?  
**Proposed public routes:** `/api/auth/register`, `/api/auth/login`, `/api/health`.  
**Open question:** Can listings be browsed publicly without authentication?  
**Affects:** Authentication spec (REQ-AUTH-6), Game Listings (REQ-LIST-2).

### 7. Listing Lifecycle: What Happens if Not Full at 2 Hours Before?
**Status:** RESOLVED
**Answer:** An underfilled listing becomes CANCELLED_INSUFFICIENT_PLAYERS at lock-in (2 hours before start). A full listing becomes CONFIRMED. Implementation belongs to Phase 4 (Listing Lifecycle).

### 8. Can Users Leave After Confirmation?
**Status:** RESOLVED
**Answer:** Strict lock. No withdrawal after lock-in (2 hours before start). No new requests, no cancellation, no edits after lock-in.

### 9. Position Assignment: Creator or Requester Decides?
**Context:** When a player joins, they select preferred positions. Does the creator assign the final position, or is the preferred position automatically used?  
**Option A:** Requester's first preference is auto-assigned if available.  
**Option B:** Creator manually assigns from the requester's preferences during accept.  
**Affects:** Game Listings (REQ-LIST-4, REQ-LIST-5).

### 10. How "Friends" Are Defined for Invitations
**Status:** RESOLVED
**Answer:** Mutual follow (Option B). A "friend" exists when user A follows user B AND user B follows user A. This is implemented in `FollowDao.findMutualFollowerIds()`.

### 11. Private Listing Access
**Context:** Listings can be public or private. How do users access private listings?  
**Option A:** Only via invite notification.  
**Option B:** Via a direct link (shareable URL).  
**Option C:** Both.  
**Affects:** Game Listings (REQ-LIST-2).
**Note:** Only invited users may submit join requests for a private listing. Visibility/access implementation belongs to the Browse Listings use case.

## Priority: Medium (Should Decide Before Feature is Complete)

### 12. Active Listing Limit Scope
**Status:** RESOLVED
**Answer:** There is no one-active-listing restriction. A user may create and participate in multiple listings subject only to scheduling conflicts (session + 60-min buffer overlap).

### 13. Scheduling Conflict: Duration Consideration
**Status:** RESOLVED
**Answer:** Scheduling conflicts use format duration to calculate session end time, plus a 60-minute travel buffer. The old "3-hour absolute start-time difference" rule has been replaced. Conflict = overlap between `[start, end + 60 min]` zones.

### 14. Removing a Sport from Profile While in Active Listing
**Status:** RESOLVED
**Answer:** Block removal while the user has a pending join request or accepted place in an upcoming/confirmed listing for that sport. Implementation belongs to Player Profiles use case.

### 15. Win/Loss Tracking: Derived or Cached?
**Context:** `UserSportProfile` has `wins` and `losses` columns. Should these be:  
**Option A:** Updated when a match result is recorded (cached, fast reads, risk of desync).  
**Option B:** Calculated on-the-fly from MatchResult table (always accurate, slower reads).  
**Affects:** Player Profiles, Match Results, Leaderboards.

### 16. Listing Lifecycle States and Transitions
**Status:** PARTIALLY RESOLVED
**Confirmed statuses:** OPEN, CONFIRMED, CANCELLED_INSUFFICIENT_PLAYERS, CANCELLED_BY_CREATOR, COMPLETED (CHECK constraint in V3).
**Confirmed transitions:** OPEN → CONFIRMED (full at lock-in), OPEN → CANCELLED_INSUFFICIENT_PLAYERS (underfilled at lock-in), OPEN → CANCELLED_BY_CREATOR (before lock-in), CONFIRMED → COMPLETED (after session end time).
**Open question:** Exact lock-in automation mechanism (scheduled job, on-demand check) is undecided. Implementation belongs to Phase 4.
**Affects:** Game Listings (REQ-LIST-8, REQ-LIST-9).

## Priority: Lower (Can Defer)

### 17. Profile Images / Avatars
**Context:** Not in FSSB data model. Useful for UX but adds image storage complexity.  
**Proposed:** Defer unless time allows. Use username initials as avatars.  
**Affects:** Player Profiles, Posts.

### 18. Image Storage for Posts
**Context:** FSSB implies posts can have images. Where are images stored?  
**Option A:** Local file system (simple, doesn't scale).  
**Option B:** Base64 in database (bad for large images).  
**Option C:** Cloud storage (out of scope for university project).  
**Proposed:** Defer image upload; text-only posts initially.  
**Affects:** Posts (B100).

### 19. Notification Delivery Mechanism
**Context:** Notifications are mentioned throughout. Are they:  
**Option A:** In-app only (polled via API on page load).  
**Option B:** Real-time (WebSocket push).  
**Proposed:** Option A (in-app polling). Real-time adds significant complexity.  
**Affects:** Notifications (D500), all features that generate notifications.

### 20. Leaderboard Calculation
**Context:** Leaderboards display win rate. Is this global per sport, or filtered by format/skill level?  
**Affects:** Leaderboards (B500).

### 21. Match Result Disputes
**Context:** Only the listing creator records results. What if participants disagree?  
**Current position:** No dispute mechanism for initial implementation. Creator is trusted.  
**Affects:** Match Results (C100, C200).
