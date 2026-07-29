# Robert Lloyd — Task Tracker

## Module

User Management (D100–D700)

## Specification

See [../specs/user-management-spec.md](../specs/user-management-spec.md)

---

## Task Checklist

| Status | ID | Use Case | Notes |
|--------|----|----------|-------|
| ⬜ Not Started | D100 | Register User | |
| ⬜ Not Started | D200 | Manage User Profile | |
| ⬜ Not Started | D300 | Add Sport | |
| ⬜ Not Started | D400 | View User Profile (Follow/Unfollow) | |
| ⬜ Not Started | D500 | View Notifications | |
| ⬜ Not Started | D600 | Report User | |
| ⬜ Not Started | D700 | Report Post | |

### Status Legend

| Icon | Meaning |
|------|---------|
| ⬜ | Not Started |
| 🟡 | In Progress |
| ✅ | Complete |
| 🔴 | Blocked |

---

## Detailed Breakdown

### D100 — Register User

- [ ] Frontend: Registration page (HTML form)
- [ ] Backend: `POST /api/auth/register` endpoint
- [ ] Backend: Validation (unique email, unique username, password rules)
- [ ] Backend: Password hashing (BCrypt)
- [ ] Database: Verify User table schema
- [ ] Integration: Connect frontend form to API
- [ ] Testing: Manual test registration flow

### D200 — Manage User Profile

- [ ] Frontend: Profile page with "Edit" mode
- [ ] Backend: `GET /api/users/{id}` endpoint
- [ ] Backend: `PUT /api/users/{id}` endpoint
- [ ] Backend: Ownership validation (only own profile)
- [ ] Frontend: Image upload for profile picture
- [ ] Testing: Manual test profile update

### D300 — Add Sport

- [ ] Frontend: "Add Sport" section on profile page
- [ ] Backend: `POST /api/users/{id}/sports` endpoint
- [ ] Backend: Duplicate sport validation
- [ ] Frontend: Sport selector + skill level + position
- [ ] Testing: Manual test adding sport

### D400 — View User Profile (Follow/Unfollow)

- [ ] Frontend: Public profile page (other users)
- [ ] Frontend: Follow/Unfollow toggle button
- [ ] Backend: `POST /api/users/{id}/follow` endpoint
- [ ] Backend: `DELETE /api/users/{id}/follow` endpoint
- [ ] Backend: Self-follow prevention
- [ ] Backend: Notification on new follow
- [ ] Testing: Manual test follow/unfollow

### D500 — View Notifications

- [ ] Frontend: Notifications page/dropdown
- [ ] Backend: `GET /api/notifications` endpoint
- [ ] Backend: `PUT /api/notifications/{id}/read` endpoint
- [ ] Backend: `PUT /api/notifications/read-all` endpoint
- [ ] Frontend: Unread badge count
- [ ] Frontend: Click-through navigation to related item
- [ ] Testing: Manual test notification flow

### D600 — Report User

- [ ] Frontend: "Report" button on other user's profile
- [ ] Frontend: Report form (reason field)
- [ ] Backend: `POST /api/reports` endpoint (with reported_user_id)
- [ ] Backend: Self-report prevention
- [ ] Testing: Manual test user reporting

### D700 — Report Post

- [ ] Frontend: "Report" button on post cards
- [ ] Frontend: Report form (reason field)
- [ ] Backend: `POST /api/reports` endpoint (with reported_post_id)
- [ ] Backend: Own-post report prevention
- [ ] Testing: Manual test post reporting

---

## Progress Log

| Date | Update |
|------|--------|
| | |
