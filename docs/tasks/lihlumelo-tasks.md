# Lihlumelo Mgijima — Task Tracker

## Module

Listings (A100–A700)

## Specification

See [../specs/listings-spec.md](../specs/listings-spec.md)

---

## Task Checklist

| Status | ID | Use Case | Notes |
|--------|----|----------|-------|
| ⬜ Not Started | A100 | Create Game Listing | |
| ⬜ Not Started | A200 | Browse Listings | |
| ⬜ Not Started | A300 | Send Join Request | |
| ⬜ Not Started | A400 | Leave Game Listing | |
| ⬜ Not Started | A500 | Hide Expired Listings | |
| ⬜ Not Started | A600 | Send Game Reminders | |
| ⬜ Not Started | A700 | Confirm Session | |

### Status Legend

| Icon | Meaning |
|------|---------|
| ⬜ | Not Started |
| 🟡 | In Progress |
| ✅ | Complete |
| 🔴 | Blocked |

---

## Detailed Breakdown

### A100 — Create Game Listing

- [ ] Frontend: Create Listing page (HTML/CSS/JS)
- [ ] Backend: `POST /api/listings` endpoint
- [ ] Backend: Listing service + validation logic
- [ ] Database: Verify GameListing table schema
- [ ] Integration: Connect frontend form to API
- [ ] Testing: Manual test end-to-end

### A200 — Browse Listings

- [ ] Frontend: Browse Listings page with filter UI
- [ ] Backend: `GET /api/listings` with query params
- [ ] Backend: Pagination logic
- [ ] Frontend: Render listing cards dynamically
- [ ] Testing: Manual test filters and pagination

### A300 — Send Join Request

- [ ] Frontend: "Join" button on listing detail page
- [ ] Backend: `POST /api/listings/{id}/join` endpoint
- [ ] Backend: Eligibility validation (not host, not full, not duplicate)
- [ ] Backend: Trigger notification to host
- [ ] Testing: Manual test join flow

### A400 — Leave Game Listing

- [ ] Frontend: "Leave" button on joined listing
- [ ] Backend: `DELETE /api/listings/{id}/leave` endpoint
- [ ] Backend: Remove GameJoiner record + notify host
- [ ] Testing: Manual test leave flow

### A500 — Hide Expired Listings

- [ ] Backend: Scheduled task or query filter for expired listings
- [ ] Backend: Update status to "Expired"
- [ ] Frontend: Ensure expired listings excluded from browse
- [ ] Testing: Verify expired listings hidden

### A600 — Send Game Reminders

- [ ] Frontend: "Send Reminder" button (host only, within 24hr)
- [ ] Backend: `POST /api/listings/{id}/remind` endpoint
- [ ] Backend: 24-hour validation + notification creation
- [ ] Testing: Manual test reminder flow

### A700 — Confirm Session

- [ ] Frontend: "Confirm Session" button (host only)
- [ ] Backend: `POST /api/listings/{id}/confirm` endpoint
- [ ] Backend: Create Session record, update listing status
- [ ] Backend: Notify approved joiners
- [ ] Testing: Manual test confirmation flow

---

## Progress Log

| Date | Update |
|------|--------|
| | |
