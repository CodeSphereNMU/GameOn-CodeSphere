# Database Design

## Purpose

This document describes the database schema for GameOn-CodeSphere, hosted on Microsoft SQL Server as `GameOnDb`.

---

## Entity-Relationship Overview

The system comprises the following core entities:

- User
- UserSportProfile
- Sport
- SportFormat
- FormatPosition
- Position
- GameListing
- GameJoiner
- Session
- MatchResult
- Post
- Comment
- Like
- Follow
- Notification
- Report

---

## Entity Definitions

### User

| Column | Type | Constraints |
|--------|------|-------------|
| user_id | INT | PK, IDENTITY |
| username | VARCHAR(50) | UNIQUE, NOT NULL |
| email | VARCHAR(100) | UNIQUE, NOT NULL |
| password_hash | VARCHAR(255) | NOT NULL |
| first_name | VARCHAR(50) | NOT NULL |
| last_name | VARCHAR(50) | NOT NULL |
| bio | VARCHAR(500) | NULL |
| profile_image_url | VARCHAR(255) | NULL |
| created_at | DATETIME | DEFAULT GETDATE() |
| is_active | BIT | DEFAULT 1 |

### Sport

| Column | Type | Constraints |
|--------|------|-------------|
| sport_id | INT | PK, IDENTITY |
| sport_name | VARCHAR(50) | UNIQUE, NOT NULL |
| description | VARCHAR(255) | NULL |

### SportFormat

| Column | Type | Constraints |
|--------|------|-------------|
| format_id | INT | PK, IDENTITY |
| sport_id | INT | FK → Sport |
| format_name | VARCHAR(50) | NOT NULL |
| max_players | INT | NOT NULL |
| min_players | INT | NOT NULL |

### Position

| Column | Type | Constraints |
|--------|------|-------------|
| position_id | INT | PK, IDENTITY |
| position_name | VARCHAR(50) | NOT NULL |

### FormatPosition

| Column | Type | Constraints |
|--------|------|-------------|
| format_position_id | INT | PK, IDENTITY |
| format_id | INT | FK → SportFormat |
| position_id | INT | FK → Position |

### UserSportProfile

| Column | Type | Constraints |
|--------|------|-------------|
| user_sport_id | INT | PK, IDENTITY |
| user_id | INT | FK → User |
| sport_id | INT | FK → Sport |
| skill_level | VARCHAR(20) | NULL (e.g., Beginner, Intermediate, Advanced) |
| preferred_position_id | INT | FK → Position, NULL |

### GameListing

| Column | Type | Constraints |
|--------|------|-------------|
| listing_id | INT | PK, IDENTITY |
| host_user_id | INT | FK → User |
| sport_id | INT | FK → Sport |
| format_id | INT | FK → SportFormat |
| title | VARCHAR(100) | NOT NULL |
| description | VARCHAR(500) | NULL |
| location | VARCHAR(200) | NOT NULL |
| game_date | DATE | NOT NULL |
| game_time | TIME | NOT NULL |
| max_players | INT | NOT NULL |
| status | VARCHAR(20) | DEFAULT 'Open' (Open, Full, Confirmed, Expired, Cancelled) |
| created_at | DATETIME | DEFAULT GETDATE() |

### GameJoiner

| Column | Type | Constraints |
|--------|------|-------------|
| joiner_id | INT | PK, IDENTITY |
| listing_id | INT | FK → GameListing |
| user_id | INT | FK → User |
| status | VARCHAR(20) | DEFAULT 'Pending' (Pending, Approved, Rejected) |
| requested_at | DATETIME | DEFAULT GETDATE() |

### Session

| Column | Type | Constraints |
|--------|------|-------------|
| session_id | INT | PK, IDENTITY |
| listing_id | INT | FK → GameListing |
| confirmed_at | DATETIME | DEFAULT GETDATE() |
| status | VARCHAR(20) | DEFAULT 'Scheduled' (Scheduled, Completed, Cancelled) |

### MatchResult

| Column | Type | Constraints |
|--------|------|-------------|
| result_id | INT | PK, IDENTITY |
| session_id | INT | FK → Session |
| recorded_by | INT | FK → User |
| result_data | NVARCHAR(MAX) | NOT NULL (JSON or structured score data) |
| recorded_at | DATETIME | DEFAULT GETDATE() |
| updated_at | DATETIME | NULL |

### Post

| Column | Type | Constraints |
|--------|------|-------------|
| post_id | INT | PK, IDENTITY |
| user_id | INT | FK → User |
| content | NVARCHAR(MAX) | NOT NULL |
| image_url | VARCHAR(255) | NULL |
| created_at | DATETIME | DEFAULT GETDATE() |
| updated_at | DATETIME | NULL |

### Comment

| Column | Type | Constraints |
|--------|------|-------------|
| comment_id | INT | PK, IDENTITY |
| post_id | INT | FK → Post |
| user_id | INT | FK → User |
| content | VARCHAR(500) | NOT NULL |
| created_at | DATETIME | DEFAULT GETDATE() |

### Like

| Column | Type | Constraints |
|--------|------|-------------|
| like_id | INT | PK, IDENTITY |
| post_id | INT | FK → Post |
| user_id | INT | FK → User |
| created_at | DATETIME | DEFAULT GETDATE() |

*Unique constraint on (post_id, user_id) — a user can like a post only once.*

### Follow

| Column | Type | Constraints |
|--------|------|-------------|
| follow_id | INT | PK, IDENTITY |
| follower_id | INT | FK → User |
| following_id | INT | FK → User |
| created_at | DATETIME | DEFAULT GETDATE() |

*Unique constraint on (follower_id, following_id). A user cannot follow themselves.*

### Notification

| Column | Type | Constraints |
|--------|------|-------------|
| notification_id | INT | PK, IDENTITY |
| user_id | INT | FK → User |
| type | VARCHAR(50) | NOT NULL (e.g., JoinRequest, Reminder, Follow, Like, Comment) |
| reference_id | INT | NULL (polymorphic reference to related entity) |
| message | VARCHAR(255) | NOT NULL |
| is_read | BIT | DEFAULT 0 |
| created_at | DATETIME | DEFAULT GETDATE() |

### Report

| Column | Type | Constraints |
|--------|------|-------------|
| report_id | INT | PK, IDENTITY |
| reporter_id | INT | FK → User |
| reported_user_id | INT | FK → User, NULL |
| reported_post_id | INT | FK → Post, NULL |
| reason | VARCHAR(500) | NOT NULL |
| status | VARCHAR(20) | DEFAULT 'Pending' (Pending, Reviewed, Dismissed) |
| created_at | DATETIME | DEFAULT GETDATE() |

---

## Relationships Summary

```
User 1──∞ UserSportProfile ∞──1 Sport
Sport 1──∞ SportFormat 1──∞ FormatPosition ∞──1 Position
User 1──∞ GameListing ∞──1 SportFormat
GameListing 1──∞ GameJoiner ∞──1 User
GameListing 1──1 Session
Session 1──1 MatchResult
User 1──∞ Post 1──∞ Comment
Post 1──∞ Like
User 1──∞ Follow (as follower)
User 1──∞ Follow (as following)
User 1──∞ Notification
User 1──∞ Report (as reporter)
```

---

## Indexing Strategy

| Table | Indexed Columns | Reason |
|-------|----------------|--------|
| User | email, username | Login lookups |
| GameListing | game_date, status, sport_id | Browse filtering |
| GameJoiner | listing_id, user_id | Join lookups |
| Post | user_id, created_at | Feed queries |
| Notification | user_id, is_read | Notification centre |

---

## Notes

- All timestamps use `DATETIME` with server default `GETDATE()`.
- Soft deletes are preferred over hard deletes where applicable (use `is_active` flags).
- Foreign key constraints enforce referential integrity at the database level.
