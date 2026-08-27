# Requirements

## Purpose

This document captures the functional and non-functional requirements for GameOn-CodeSphere — a sports match booking platform with social features.

---

## Functional Requirements

### Listings (Owner: Lihlumelo Mgijima)

| ID | Requirement | Priority |
|----|-------------|----------|
| A100 | Create Game Listing | High |
| A200 | Browse Listings | High |
| A300 | Send Join Request | High |
| A400 | Leave Game Listing | Medium |
| A500 | Hide Expired Listings | Medium |
| A600 | Send Game Reminders | Low |
| A700 | Confirm Session | Medium |

### Social / Posts (Owner: Zane Griesel)

| ID | Requirement | Priority |
|----|-------------|----------|
| B100 | Create Posts | High |
| B200 | Manage Posts | High |
| B300 | Browse Posts | High |
| B400 | View Reports | Medium |
| B500 | View Leaderboards | Low |

### Match Results (Owner: Gerard Mc Loughlin)

| ID | Requirement | Priority |
|----|-------------|----------|
| C100 | Record Match Result | High |
| C200 | Update Match Result | High |
| C300 | Manage Game Listing | High |
| C400 | View Match Results | Medium |
| C500 | View Join Requests | Medium |

### User Management (Owner: Robert Lloyd)

| ID | Requirement | Priority |
|----|-------------|----------|
| D100 | Register User | High |
| D200 | Manage User Profile | High |
| D300 | Add Sport | Medium |
| D400 | View User Profile (Follow/Unfollow) | Medium |
| D500 | View Notifications | Medium |
| D600 | Report User | Low |
| D700 | Report Post | Low |

---

## Non-Functional Requirements

| Category | Requirement |
|----------|-------------|
| Performance | Pages must load within 3 seconds on standard broadband |
| Security | Passwords stored as salted hashes; SQL injection prevention via parameterised queries |
| Usability | Responsive design supporting desktop and mobile viewports |
| Availability | System available during university operating hours (07:00–22:00) |
| Compatibility | Support latest versions of Chrome, Firefox, Edge |
| Data Integrity | All database operations wrapped in transactions where appropriate |

---

## Assumptions

- Users have a valid email address for registration.
- A single sport can have multiple formats (e.g., Football: 5-a-side, 11-a-side).
- The platform is used within a university or local community context.

---

## Out of Scope

- Payment processing
- Native mobile applications
- Real-time chat or messaging
