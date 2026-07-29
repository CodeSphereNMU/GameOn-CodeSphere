# Zane Griesel — Task Tracker

## Module

Social / Posts (B100–B500)

## Specification

See [../specs/social-spec.md](../specs/social-spec.md)

---

## Task Checklist

| Status | ID | Use Case | Notes |
|--------|----|----------|-------|
| ⬜ Not Started | B100 | Create Posts | |
| ⬜ Not Started | B200 | Manage Posts | |
| ⬜ Not Started | B300 | Browse Posts | |
| ⬜ Not Started | B400 | View Reports | |
| ⬜ Not Started | B500 | View Leaderboards | |

### Status Legend

| Icon | Meaning |
|------|---------|
| ⬜ | Not Started |
| 🟡 | In Progress |
| ✅ | Complete |
| 🔴 | Blocked |

---

## Detailed Breakdown

### B100 — Create Posts

- [ ] Frontend: Create Post page/modal (text + image upload)
- [ ] Backend: `POST /api/posts` endpoint
- [ ] Backend: Post service + validation (content not empty, image ≤ 5 MB)
- [ ] Database: Verify Post table schema
- [ ] Integration: Connect frontend form to API
- [ ] Testing: Manual test post creation

### B200 — Manage Posts

- [ ] Frontend: Edit/Delete buttons on own posts
- [ ] Backend: `PUT /api/posts/{id}` endpoint
- [ ] Backend: `DELETE /api/posts/{id}` endpoint
- [ ] Backend: Ownership validation (only author can edit/delete)
- [ ] Backend: Cascade delete likes/comments on post deletion
- [ ] Testing: Manual test edit and delete flows

### B300 — Browse Posts

- [ ] Frontend: Social feed page with post cards
- [ ] Backend: `GET /api/posts` endpoint (paginated, newest first)
- [ ] Frontend: Like button (toggle) with count
- [ ] Frontend: Comment section (add + list)
- [ ] Backend: `POST /api/posts/{id}/like` and `DELETE /api/posts/{id}/like`
- [ ] Backend: `POST /api/posts/{id}/comments` and `GET /api/posts/{id}/comments`
- [ ] Testing: Manual test feed, likes, and comments

### B400 — View Reports

- [ ] Frontend: Admin Reports page (table of pending reports)
- [ ] Backend: `GET /api/reports` endpoint (admin only)
- [ ] Backend: `PUT /api/reports/{id}` to update status
- [ ] Frontend: Filter by type (User / Post)
- [ ] Frontend: Action buttons (Review / Dismiss)
- [ ] Testing: Manual test admin report flow

### B500 — View Leaderboards

- [ ] Frontend: Leaderboard page with sport filter dropdown
- [ ] Backend: `GET /api/leaderboards` and `GET /api/leaderboards/{sportId}`
- [ ] Backend: Ranking calculation (Win=3, Draw=1, Loss=0)
- [ ] Frontend: Display ranked table (player, wins, draws, losses, points)
- [ ] Testing: Manual test leaderboard with sample data

---

## Progress Log

| Date | Update |
|------|--------|
| | |
