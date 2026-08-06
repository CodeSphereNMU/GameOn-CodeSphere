# GameOn - Unresolved Questions

Questions that require group discussion before implementation. These affect requirements or architecture and should not be resolved by a single person.

## Priority: High (Blocks Feature Implementation)

### 1. Session Storage Approach
**Context:** Authentication requires sessions. Two options exist.
**Option A:** Javalin's built-in Jetty sessions (server memory). Simple, but sessions are lost on restart.
**Option B:** Custom `session` table in SQL Server. Survives restarts, more work to implement.
**Current state:** Option A is in use for login. No group decision has been made to keep or change it.
**Affects:** Authentication spec (REQ-AUTH-5).

### 2. Session Expiry Duration
**Context:** How long should a session last before requiring re-login?
**Options:** 24 hours, 7 days, configurable, never (until logout).
**Current state:** No expiry configured. Sessions last until server restart.
**Affects:** Authentication spec (REQ-AUTH-5).

### 3. Password and Username Validation Rules
**Context:** The FSSB does not specify character limits or complexity rules for usernames or passwords.
**Proposed (not confirmed):** Username 3-20 chars alphanumeric+underscore; password min 8 chars.
**Group must decide:** Actual limits before registration implementation.
**Affects:** Authentication spec (REQ-AUTH-1), Player Profiles (REQ-PROF-2).

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

### 7. Position Assignment: Creator or Requester Decides?
**Context:** When a player joins, they select preferred positions. Does the creator assign the final position, or is the preferred position automatically used?
**Option A:** Requester's first preference is auto-assigned if available.
**Option B:** Creator manually assigns from the requester's preferences during accept.
**Affects:** Game Listings (A300, C500).

### 8. Private Listing Access
**Context:** Listings can be public or private. How do users access private listings?
**Option A:** Only via invite notification.
**Option B:** Via a direct link (shareable URL).
**Option C:** Both.
**Affects:** Game Listings (A200 browsing).
**Note:** Only invited users may submit join requests for a private listing. Visibility/access implementation belongs to the Browse Listings use case.

### 9. A500/A600 Mapping — FSSB Contradiction
**Context:** The marked functional specification contains a contradiction:
- One section assigns A500 = Hide Expired Listings and A600 = Send Game Reminders.
- The detailed narratives reverse those mappings.
**Resolution for planning:** The BOC (`BOC2026.xlsx`) assigns A500 = "Hide expired listings" and A600 = "Send game reminders". These official names are used in the roadmap.
**Remaining concern:** If the FSSB detailed narratives describe different behaviour under these IDs, the group should verify that the BOC names match the intended functionality.
**Affects:** Roadmap, task allocation, specification naming.

## Priority: Medium (Should Decide Before Feature is Complete)

### 10. Which Listing Fields May Be Edited Before Lock-in
**Context:** C300 allows editing a listing. Which fields can change?
**Options:** Date/time, location, skill level, privacy? Or only some of these?
**Follow-up:** If date/time changes, must accepted participants' schedules be revalidated?
**Affects:** C300 (Edit Listing).

### 11. Does C300 "Delete Listing" Mean Soft Cancellation?
**Context:** The use-case catalogue mentions deleting a listing. The schema supports `CANCELLED_BY_CREATOR` status.
**Question:** Is deletion a soft cancellation (status change) or a hard delete (row removal)?
**Proposed:** Soft cancellation using `CANCELLED_BY_CREATOR`.
**Affects:** C300 (Manage Listings).

### 12. What Happens to Pending Requests When a Listing Becomes Full
**Context:** When accepted participants fill all capacity, are remaining PENDING join requests automatically rejected, left pending, or expired?
**Affects:** C500 (Manage Join Requests), A300 (Send Join Request).

### 13. Win/Loss Tracking: Derived or Cached?
**Context:** `user_sport_profile` has `wins` and `losses` columns. Should these be:
**Option A:** Updated when a match result is recorded (cached, fast reads, risk of desync).
**Option B:** Calculated on-the-fly from match_result table (always accurate, slower reads).
**Affects:** Player Profiles, Match Results, Leaderboards.

### 14. Leaderboard Calculation and Filtering
**Context:** Leaderboards display win rate. Filters unclear.
**Questions:** Global per sport? Filtered by format? Filtered by skill level? Multiple selection?
**Confirmed partial rule:** Social-feed community filtering is intended to allow multiple selections.
**Affects:** Leaderboards (B500).

### 15. Match Result Disputes
**Context:** Only the listing creator records results. What if participants disagree?
**Current position:** No dispute mechanism for initial implementation. Creator is trusted.
**Affects:** Match Results (C100, C200).

### 16. Exact Lifecycle-Automation Mechanism
**Context:** Lock-in processing (2 hours before start) requires automatic status transitions. How is this triggered?
**Option A:** Scheduled background job (timer thread).
**Option B:** On-demand check triggered by API calls.
**Option C:** Database-level scheduled job.
**Affects:** A500/A600/A700 (Listing Lifecycle).

### 17. Sport Removal Behaviour with Active Listings
**Context:** When a user wants to remove a sport from their profile but has a pending join request or accepted place in an upcoming/confirmed listing for that sport — should removal be blocked?
**Option A:** Block removal while active involvement exists.
**Option B:** Allow removal but withdraw/cancel the user's participation automatically.
**Option C:** Allow removal; existing participation is unaffected until the listing completes.
**Current state:** Not decided. Earlier documentation proposed blocking but this was never confirmed as a business rule.
**Affects:** Player Profiles (REQ-PROF-4).

### 18. Player Profile: Last-Sport Rule
**Context:** Can a user remove their last and only sport from their profile?
**Option A:** Block — at least one sport must remain.
**Option B:** Allow — user simply cannot create/join listings until they add one again.
**Current state:** Not decided. Do not assume either answer.
**Affects:** Player Profiles (REQ-PROF-4).

### 19. Player Profile: Full-Profile Visibility
**Context:** Who may view another user's full profile?
**Option A:** All authenticated users.
**Option B:** Only mutual followers (friends).
**Option C:** Configurable privacy setting.
**Current state:** Not decided.
**Affects:** Player Profiles (REQ-PROF-6), D400.

## Priority: Lower (Can Defer)

### 20. Profile Images / Avatars
**Context:** Not in FSSB data model. Useful for UX but adds image storage complexity.
**Proposed:** Defer unless time allows. Use username initials as avatars.
**Affects:** Player Profiles, Posts.

### 21. Image Storage for Posts
**Context:** FSSB implies posts can have images. Where are images stored?
**Option A:** Local file system (simple, doesn't scale).
**Option B:** Base64 in database (bad for large images).
**Option C:** Cloud storage (out of scope for university project).
**Proposed:** Defer image upload; text-only posts initially.
**Affects:** Posts (B100).

### 22. Notification Delivery Mechanism
**Context:** Notifications are mentioned throughout. Are they:
**Option A:** In-app only (polled via API on page load).
**Option B:** Real-time (WebSocket push).
**Proposed:** Option A (in-app polling). Real-time adds significant complexity.
**Affects:** Notifications (D500), all features that generate notifications.

## Resolved Questions (kept for reference)

### R1. Listing Lifecycle: What Happens if Not Full at Lock-in
**Resolution:** An underfilled listing becomes CANCELLED_INSUFFICIENT_PLAYERS at lock-in (2 hours before start). A full listing becomes CONFIRMED. Implementation belongs to Phase 4 (Listing Lifecycle).

### R2. Can Users Leave After Lock-in
**Resolution:** Strict lock. No withdrawal after lock-in (2 hours before start). No new requests, no cancellation, no edits after lock-in.

### R3. How "Friends" Are Defined for Invitations
**Resolution:** Mutual follow (Option B). A "friend" exists when user A follows user B AND user B follows user A. Implemented in `FollowDao.findMutualFollowerIds()`.

### R4. Active Listing Limit Scope
**Resolution:** No one-active-listing restriction. A user may create and participate in multiple listings subject only to scheduling conflicts (session + 60-min buffer overlap).

### R5. Scheduling Conflict: Duration Consideration
**Resolution:** Scheduling conflicts use format duration to calculate session end time, plus a 60-minute travel buffer. Conflict = overlap between `[start, end + 60 min]` zones. Boundary equality is allowed (not a conflict). Only OPEN/CONFIRMED listings are considered.

### R6. Removing a Sport from Profile While in Active Listing
**Previous status:** Was marked as resolved in earlier documentation.
**Current status:** Reclassified as unresolved (see #17 above). The blocking proposal was never confirmed as a business rule.

### R7. Listing Lifecycle States and Transitions
**Resolution:** Confirmed statuses: OPEN, CONFIRMED, CANCELLED_INSUFFICIENT_PLAYERS, CANCELLED_BY_CREATOR, COMPLETED. Confirmed transitions: OPEN → CONFIRMED (full at lock-in), OPEN → CANCELLED_INSUFFICIENT_PLAYERS (underfilled at lock-in), OPEN → CANCELLED_BY_CREATOR (before lock-in), CONFIRMED → COMPLETED (after session end time). Lock-in automation mechanism remains undecided (see #16).
