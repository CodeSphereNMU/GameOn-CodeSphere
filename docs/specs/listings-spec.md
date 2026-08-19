# Listings Specification

## Owner

**Lihlumelo Mgijima**

## Overview

The Listings module allows users to create, browse, join, and manage sports game listings. It handles the full lifecycle from listing creation through session confirmation.

---

## Use Cases

### A100 — Create Game Listing

| Field | Detail |
|-------|--------|
| **Actor** | Registered User (Host) |
| **Description** | User creates a new game listing specifying sport, format, date, time, location, and player capacity. |
| **Preconditions** | User is logged in; user has at least one sport on their profile. |
| **Postconditions** | A new listing is created with status "Open"; user is assigned as host. |
| **Triggers** | User clicks "Create Listing" and submits the form. |

**Basic Flow:**
1. User navigates to the Create Listing page.
2. User selects sport and format.
3. User enters title, description, location, date, time.
4. System validates input and checks for scheduling conflicts.
5. System creates the listing and redirects to listing detail page.

**Alternative Flows:**
- 5a. Validation fails → system displays error messages.

---

### A200 — Browse Listings

| Field | Detail |
|-------|--------|
| **Actor** | Registered User |
| **Description** | User browses available game listings with optional filters (sport, date, status). |
| **Preconditions** | User is logged in. |
| **Postconditions** | User sees a filtered list of open listings. |
| **Triggers** | User navigates to the Listings page. |

**Basic Flow:**
1. User navigates to Browse Listings page.
2. System displays all open listings (paginated).
3. User optionally applies filters (sport, date, location).
4. System refreshes the list based on filters.

---

### A300 — Send Join Request

| Field | Detail |
|-------|--------|
| **Actor** | Registered User |
| **Description** | User sends a request to join an open game listing. |
| **Preconditions** | User is logged in; listing is open; user is not already a joiner or the host. |
| **Postconditions** | A join request (status: Pending) is created; host is notified. |
| **Triggers** | User clicks "Join" on a listing detail page. |

**Basic Flow:**
1. User views a listing detail page.
2. User clicks "Request to Join."
3. System validates eligibility (not host, not already joined, listing not full).
4. System creates a GameJoiner record with status "Pending."
5. System sends notification to host.

**Alternative Flows:**
- 3a. Listing is full → system displays "Listing Full" message.
- 3b. User already joined → system displays "Already Requested" message.

---

### A400 — Leave Game Listing

| Field | Detail |
|-------|--------|
| **Actor** | Registered User (Joiner) |
| **Description** | User removes themselves from a game listing they previously joined. |
| **Preconditions** | User is logged in; user is an approved joiner; session has not started. |
| **Postconditions** | GameJoiner record is removed; host is notified; slot opens up. |
| **Triggers** | User clicks "Leave" on the listing or lobby page. |

**Basic Flow:**
1. User views their joined listing.
2. User clicks "Leave Game."
3. System confirms the action.
4. System removes the joiner record and notifies the host.

---

### A500 — Hide Expired Listings

| Field | Detail |
|-------|--------|
| **Actor** | System (Automated) |
| **Description** | System automatically hides listings whose game date/time has passed. |
| **Preconditions** | Listings exist with past game_date and game_time. |
| **Postconditions** | Expired listings are marked with status "Expired" and excluded from browse results. |
| **Triggers** | Scheduled job or on-demand during browse query. |

**Basic Flow:**
1. System identifies listings where game_date + game_time < current datetime.
2. System updates their status to "Expired."
3. Expired listings no longer appear in browse results.

---

### A600 — Send Game Reminders

| Field | Detail |
|-------|--------|
| **Actor** | Registered User (Host) |
| **Description** | Host sends a reminder notification to all confirmed joiners. |
| **Preconditions** | User is the host; session is within 24 hours; session is confirmed. |
| **Postconditions** | All confirmed joiners receive a notification. |
| **Triggers** | Host clicks "Send Reminder." |

**Basic Flow:**
1. Host views their listing (confirmed session).
2. Host clicks "Send Reminder."
3. System validates session is within 24 hours.
4. System creates a notification for each approved joiner.

**Alternative Flows:**
- 3a. Session is more than 24 hours away → system rejects with message.

---

### A700 — Confirm Session

| Field | Detail |
|-------|--------|
| **Actor** | Registered User (Host) |
| **Description** | Host confirms the session, locking in the player list. |
| **Preconditions** | User is the host; minimum players reached; listing status is "Open" or "Full." |
| **Postconditions** | Session record is created; listing status changes to "Confirmed"; joiners are notified. |
| **Triggers** | Host clicks "Confirm Session." |

**Basic Flow:**
1. Host views their listing with enough approved joiners.
2. Host clicks "Confirm Session."
3. System creates a Session record linked to the listing.
4. System updates listing status to "Confirmed."
5. System notifies all approved joiners.

**Alternative Flows:**
- 2a. Minimum players not reached → system displays warning.
