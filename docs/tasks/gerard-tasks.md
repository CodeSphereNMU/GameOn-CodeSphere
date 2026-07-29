# Gerard Mc Loughlin — Task Tracker

## Module

Match Results (C100–C500)

## Specification

See [../specs/match-results-spec.md](../specs/match-results-spec.md)

---

## Task Checklist

| Status | ID | Use Case | Notes |
|--------|----|----------|-------|
| ⬜ Not Started | C100 | Record Match Result | |
| ⬜ Not Started | C200 | Update Match Result | |
| ⬜ Not Started | C300 | Manage Game Listing | |
| ⬜ Not Started | C400 | View Match Results | |
| ⬜ Not Started | C500 | View Join Requests | |

### Status Legend

| Icon | Meaning |
|------|---------|
| ⬜ | Not Started |
| 🟡 | In Progress |
| ✅ | Complete |
| 🔴 | Blocked |

---

## Detailed Breakdown

### C100 — Record Match Result

- [ ] Frontend: Record Result page/form (scores, outcome)
- [ ] Backend: `POST /api/sessions/{sessionId}/results` endpoint
- [ ] Backend: Validation (host only, session completed, no existing result)
- [ ] Backend: Leaderboard recalculation on new result
- [ ] Database: Verify MatchResult table schema
- [ ] Integration: Connect frontend form to API
- [ ] Testing: Manual test result recording

### C200 — Update Match Result

- [ ] Frontend: "Edit Result" button on existing result
- [ ] Backend: `PUT /api/sessions/{sessionId}/results` endpoint
- [ ] Backend: 48-hour window validation
- [ ] Backend: Leaderboard recalculation on update
- [ ] Testing: Manual test edit within and outside window

### C300 — Manage Game Listing

- [ ] Frontend: Edit Listing form (host view)
- [ ] Frontend: Cancel Listing button with confirmation
- [ ] Backend: `PUT /api/listings/{id}` endpoint
- [ ] Backend: `DELETE /api/listings/{id}` (cancel) endpoint
- [ ] Backend: 1-hour cancellation window validation
- [ ] Backend: Notify joiners on cancellation
- [ ] Testing: Manual test edit and cancel flows (including 1-hour block)

### C400 — View Match Results

- [ ] Frontend: Match History page (list of past sessions)
- [ ] Frontend: Result detail view (scores, participants)
- [ ] Backend: `GET /api/sessions/{sessionId}/results` endpoint
- [ ] Backend: `GET /api/users/{id}/results` endpoint
- [ ] Testing: Manual test result viewing

### C500 — View Join Requests

- [ ] Frontend: Join Requests section on listing detail (host view)
- [ ] Frontend: Approve/Reject buttons per request
- [ ] Backend: `GET /api/listings/{id}/joiners` endpoint
- [ ] Backend: `PUT /api/listings/{id}/joiners/{joinerId}` endpoint
- [ ] Backend: Send notification to joiner on approval/rejection
- [ ] Testing: Manual test approve and reject flows

---

## Progress Log

| Date | Update |
|------|--------|
| | |
