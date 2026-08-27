# Match Results Specification

## Owner

**Gerard Mc Loughlin**

## Overview

The Match Results module enables hosts to record and update game outcomes after a session is completed. It also covers the host's ability to manage their listings and view join requests.

---

## Use Cases

### C100 — Record Match Result

| Field | Detail |
|-------|--------|
| **Actor** | Registered User (Host) |
| **Description** | Host records the match result for a completed session including scores and outcome. |
| **Preconditions** | User is the host; session exists and game date/time has passed; no result recorded yet. |
| **Postconditions** | A MatchResult record is created; leaderboard is updated. |
| **Triggers** | Host clicks "Record Result" on a completed session. |

**Basic Flow:**
1. Host navigates to their completed session.
2. Host clicks "Record Result."
3. System displays result entry form (scores, winner, individual stats).
4. Host enters result data and submits.
5. System validates and saves the match result.
6. System recalculates leaderboard rankings.

**Alternative Flows:**
- 3a. Session not yet past → system blocks with "Session not completed" message.
- 5a. Validation fails → system displays error messages.

---

### C200 — Update Match Result

| Field | Detail |
|-------|--------|
| **Actor** | Registered User (Host) |
| **Description** | Host updates a previously recorded match result within the allowed window. |
| **Preconditions** | User is the host; result exists; within 48 hours of original recording. |
| **Postconditions** | MatchResult record is updated; leaderboard is recalculated. |
| **Triggers** | Host clicks "Edit Result" on an existing result. |

**Basic Flow:**
1. Host views the recorded match result.
2. Host clicks "Edit Result."
3. System displays pre-filled result form.
4. Host modifies data and submits.
5. System validates the 48-hour window and data.
6. System updates the record and recalculates leaderboard.

**Alternative Flows:**
- 5a. 48-hour window expired → system blocks edit with message.

---

### C300 — Manage Game Listing

| Field | Detail |
|-------|--------|
| **Actor** | Registered User (Host) |
| **Description** | Host can edit or cancel their own game listing. |
| **Preconditions** | User is the host; listing is not in "Confirmed" or "Expired" state; cancellation is not within 1 hour of match time. |
| **Postconditions** | Listing is updated or cancelled; joiners notified if cancelled. |
| **Triggers** | Host clicks "Edit" or "Cancel" on their listing. |

**Basic Flow (Edit):**
1. Host views their listing.
2. Host clicks "Edit Listing."
3. System displays editable form with current details.
4. Host makes changes and submits.
5. System validates and updates listing.

**Basic Flow (Cancel):**
1. Host clicks "Cancel Listing."
2. System checks that current time is more than 1 hour before the match time.
3. System prompts for confirmation.
4. Host confirms.
5. System sets listing status to "Cancelled."
6. System notifies all approved joiners.

**Alternative Flows:**
- 2a. Current time is within 1 hour of match time → system blocks cancellation with "Cannot cancel within 1 hour of match time" message.

---

### C400 — View Match Results

| Field | Detail |
|-------|--------|
| **Actor** | Registered User |
| **Description** | User views match results for sessions they participated in or any public session. |
| **Preconditions** | User is logged in; match results exist. |
| **Postconditions** | User sees result details (scores, participants, outcome). |
| **Triggers** | User navigates to match history or clicks a session. |

**Basic Flow:**
1. User navigates to their Match History page.
2. System displays list of past sessions with results.
3. User clicks a session to view detailed result.
4. System displays full result data.

---

### C500 — View Join Requests

| Field | Detail |
|-------|--------|
| **Actor** | Registered User (Host) |
| **Description** | Host views pending join requests for their listing and approves or rejects them. |
| **Preconditions** | User is the host; listing has pending join requests. |
| **Postconditions** | Join requests are approved or rejected; joiners are notified. |
| **Triggers** | Host views their listing detail or receives a notification. |

**Basic Flow:**
1. Host views their listing detail page.
2. System displays pending join requests.
3. Host clicks "Approve" or "Reject" for each request.
4. System updates GameJoiner status.
5. System sends notification to the requesting user.
