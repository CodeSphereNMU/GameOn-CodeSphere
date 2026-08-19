# Social / Posts Specification

## Owner

**Zane Griesel**

## Overview

The Social module provides a community feed where users can create posts, interact via likes and comments, and view leaderboards. It also includes the admin report viewing functionality.

---

## Use Cases

### B100 — Create Posts

| Field | Detail |
|-------|--------|
| **Actor** | Registered User |
| **Description** | User creates a new post containing text and/or an image to share with the community. |
| **Preconditions** | User is logged in. |
| **Postconditions** | A new post is created and visible on the feed. |
| **Triggers** | User clicks "Create Post" and submits the form. |

**Basic Flow:**
1. User navigates to the Social feed or "Create Post" page.
2. User enters post content (text and/or image).
3. User submits the post.
4. System validates content (not empty, image within size limit).
5. System saves the post and displays it on the feed.

**Alternative Flows:**
- 4a. Content is empty → system displays validation error.
- 4b. Image exceeds 5 MB → system rejects upload.

---

### B200 — Manage Posts

| Field | Detail |
|-------|--------|
| **Actor** | Registered User (Post Author) |
| **Description** | Author can edit or delete their own posts. |
| **Preconditions** | User is logged in; user is the author of the post. |
| **Postconditions** | Post is updated or removed from the system. |
| **Triggers** | User clicks "Edit" or "Delete" on their own post. |

**Basic Flow (Edit):**
1. User clicks "Edit" on their post.
2. System displays editable form with existing content.
3. User modifies content and submits.
4. System validates and updates the post.

**Basic Flow (Delete):**
1. User clicks "Delete" on their post.
2. System prompts for confirmation.
3. User confirms deletion.
4. System removes the post and associated likes/comments.

---

### B300 — Browse Posts

| Field | Detail |
|-------|--------|
| **Actor** | Registered User |
| **Description** | User browses the community feed showing posts from all users (or followed users). |
| **Preconditions** | User is logged in. |
| **Postconditions** | User views a paginated list of posts. |
| **Triggers** | User navigates to the Social / Feed page. |

**Basic Flow:**
1. User navigates to the feed.
2. System retrieves posts ordered by most recent.
3. User can like, comment, or report posts inline.
4. User can click a post for detail view.

**Interactions:**
- Like: Toggle like on/off for a post.
- Comment: Add a text comment to a post.
- Report: Flag a post for review (creates a Report record).

---

### B400 — View Reports

| Field | Detail |
|-------|--------|
| **Actor** | Admin User |
| **Description** | Admin views submitted reports (user reports and post reports) and takes action. |
| **Preconditions** | User is logged in with admin privileges. |
| **Postconditions** | Admin reviews and updates report status. |
| **Triggers** | Admin navigates to the Reports page. |

**Basic Flow:**
1. Admin navigates to the Admin / Reports page.
2. System displays pending reports (filterable by type: User, Post).
3. Admin reviews report details.
4. Admin marks report as "Reviewed" or "Dismissed."
5. System updates report status.

---

### B500 — View Leaderboards

| Field | Detail |
|-------|--------|
| **Actor** | Registered User |
| **Description** | User views the leaderboard rankings based on match results, filtered by sport. |
| **Preconditions** | User is logged in; match results exist. |
| **Postconditions** | User sees ranked list of players by sport. |
| **Triggers** | User navigates to the Leaderboard page. |

**Basic Flow:**
1. User navigates to the Leaderboard page.
2. System displays overall leaderboard (all sports).
3. User optionally filters by a specific sport.
4. System displays ranked players (wins, draws, losses, points).

**Ranking Logic:**
- Win = 3 points
- Draw = 1 point
- Loss = 0 points
- Ranked by total points descending, then wins descending.
