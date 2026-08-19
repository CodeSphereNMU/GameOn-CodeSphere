# Database

## Overview

GameOn-CodeSphere uses **Microsoft SQL Server** with a database named **`GameOnDb`**. The database runs **locally/offline** on each developer's machine — there is no hosted or shared database server. This folder will contain SQL scripts for schema creation, seed data, and migrations.

---

## Full Schema Documentation

See [../docs/database-design.md](../docs/database-design.md) for the complete design document with relationships and indexing strategy.

---

## Entity List

The database consists of **16 entities**:

| # | Entity | Purpose |
|---|--------|---------|
| 1 | User | Registered platform users |
| 2 | Sport | Available sports (Football, Basketball, etc.) |
| 3 | SportFormat | Format variants per sport (5-a-side, 11-a-side) |
| 4 | Position | Playing positions (Goalkeeper, Striker, etc.) |
| 5 | FormatPosition | Links formats to their valid positions |
| 6 | UserSportProfile | User's sport preferences (sport, skill, position) |
| 7 | GameListing | A scheduled game session created by a host |
| 8 | GameJoiner | Join requests from users to a listing |
| 9 | Session | A confirmed game session ready for play |
| 10 | MatchResult | Recorded outcome of a completed session |
| 11 | Post | Social feed posts by users |
| 12 | Comment | Comments on posts |
| 13 | Like | Likes on posts |
| 14 | Follow | User-to-user follow relationships |
| 15 | Notification | System notifications to users |
| 16 | Report | User/post reports for admin review |

---

## Entity Attributes

### User

| Column | Type | Notes |
|--------|------|-------|
| user_id | INT | PK, IDENTITY |
| username | VARCHAR(50) | UNIQUE, NOT NULL |
| email | VARCHAR(100) | UNIQUE, NOT NULL |
| password_hash | VARCHAR(255) | NOT NULL |
| first_name | VARCHAR(50) | NOT NULL |
| last_name | VARCHAR(50) | NOT NULL |
| bio | VARCHAR(500) | Nullable |
| profile_image_url | VARCHAR(255) | Nullable |
| created_at | DATETIME | DEFAULT GETDATE() |
| is_active | BIT | DEFAULT 1 |

### Sport

| Column | Type | Notes |
|--------|------|-------|
| sport_id | INT | PK, IDENTITY |
| sport_name | VARCHAR(50) | UNIQUE, NOT NULL |
| description | VARCHAR(255) | Nullable |

### SportFormat

| Column | Type | Notes |
|--------|------|-------|
| format_id | INT | PK, IDENTITY |
| sport_id | INT | FK → Sport |
| format_name | VARCHAR(50) | NOT NULL |
| max_players | INT | NOT NULL |
| min_players | INT | NOT NULL |

### Position

| Column | Type | Notes |
|--------|------|-------|
| position_id | INT | PK, IDENTITY |
| position_name | VARCHAR(50) | NOT NULL |

### FormatPosition

| Column | Type | Notes |
|--------|------|-------|
| format_position_id | INT | PK, IDENTITY |
| format_id | INT | FK → SportFormat |
| position_id | INT | FK → Position |

### UserSportProfile

| Column | Type | Notes |
|--------|------|-------|
| user_sport_id | INT | PK, IDENTITY |
| user_id | INT | FK → User |
| sport_id | INT | FK → Sport |
| skill_level | VARCHAR(20) | Nullable (Beginner, Intermediate, Advanced) |
| preferred_position_id | INT | FK → Position, Nullable |

### GameListing

| Column | Type | Notes |
|--------|------|-------|
| listing_id | INT | PK, IDENTITY |
| host_user_id | INT | FK → User |
| sport_id | INT | FK → Sport |
| format_id | INT | FK → SportFormat |
| title | VARCHAR(100) | NOT NULL |
| description | VARCHAR(500) | Nullable |
| location | VARCHAR(200) | NOT NULL |
| game_date | DATE | NOT NULL |
| game_time | TIME | NOT NULL |
| max_players | INT | NOT NULL |
| status | VARCHAR(20) | DEFAULT 'Open' |
| created_at | DATETIME | DEFAULT GETDATE() |

**Status values:** Open, Full, Confirmed, Expired, Cancelled

### GameJoiner

| Column | Type | Notes |
|--------|------|-------|
| joiner_id | INT | PK, IDENTITY |
| listing_id | INT | FK → GameListing |
| user_id | INT | FK → User |
| status | VARCHAR(20) | DEFAULT 'Pending' |
| requested_at | DATETIME | DEFAULT GETDATE() |

**Status values:** Pending, Approved, Rejected

### Session

| Column | Type | Notes |
|--------|------|-------|
| session_id | INT | PK, IDENTITY |
| listing_id | INT | FK → GameListing |
| confirmed_at | DATETIME | DEFAULT GETDATE() |
| status | VARCHAR(20) | DEFAULT 'Scheduled' |

**Status values:** Scheduled, Completed, Cancelled

### MatchResult

| Column | Type | Notes |
|--------|------|-------|
| result_id | INT | PK, IDENTITY |
| session_id | INT | FK → Session |
| recorded_by | INT | FK → User (host) |
| result_data | NVARCHAR(MAX) | JSON score/outcome data |
| recorded_at | DATETIME | DEFAULT GETDATE() |
| updated_at | DATETIME | Nullable |

### Post

| Column | Type | Notes |
|--------|------|-------|
| post_id | INT | PK, IDENTITY |
| user_id | INT | FK → User |
| content | NVARCHAR(MAX) | NOT NULL |
| image_url | VARCHAR(255) | Nullable |
| created_at | DATETIME | DEFAULT GETDATE() |
| updated_at | DATETIME | Nullable |

### Comment

| Column | Type | Notes |
|--------|------|-------|
| comment_id | INT | PK, IDENTITY |
| post_id | INT | FK → Post |
| user_id | INT | FK → User |
| content | VARCHAR(500) | NOT NULL |
| created_at | DATETIME | DEFAULT GETDATE() |

### Like

| Column | Type | Notes |
|--------|------|-------|
| like_id | INT | PK, IDENTITY |
| post_id | INT | FK → Post |
| user_id | INT | FK → User |
| created_at | DATETIME | DEFAULT GETDATE() |

**Constraint:** UNIQUE on (post_id, user_id)

### Follow

| Column | Type | Notes |
|--------|------|-------|
| follow_id | INT | PK, IDENTITY |
| follower_id | INT | FK → User |
| following_id | INT | FK → User |
| created_at | DATETIME | DEFAULT GETDATE() |

**Constraints:** UNIQUE on (follower_id, following_id); CHECK follower_id ≠ following_id

### Notification

| Column | Type | Notes |
|--------|------|-------|
| notification_id | INT | PK, IDENTITY |
| user_id | INT | FK → User |
| type | VARCHAR(50) | NOT NULL |
| reference_id | INT | Nullable (polymorphic FK) |
| message | VARCHAR(255) | NOT NULL |
| is_read | BIT | DEFAULT 0 |
| created_at | DATETIME | DEFAULT GETDATE() |

**Type values:** JoinRequest, Reminder, Follow, Like, Comment, Report

### Report

| Column | Type | Notes |
|--------|------|-------|
| report_id | INT | PK, IDENTITY |
| reporter_id | INT | FK → User |
| reported_user_id | INT | FK → User, Nullable |
| reported_post_id | INT | FK → Post, Nullable |
| reason | VARCHAR(500) | NOT NULL |
| status | VARCHAR(20) | DEFAULT 'Pending' |
| created_at | DATETIME | DEFAULT GETDATE() |

**Status values:** Pending, Reviewed, Dismissed

---

## Planned SQL Scripts

| File | Purpose |
|------|---------|
| `schema.sql` | CREATE TABLE statements for all entities |
| `seed-data.sql` | Initial data (sports, formats, positions) |
| `indexes.sql` | Performance indexes |
| `drop-all.sql` | Drop all tables (dev use only) |

---

## Connection Details (Local / Offline)

The database is not hosted remotely. Each team member runs SQL Server locally.

```
Server:   localhost\SQLEXPRESS  (or localhost:1433)
Database: GameOnDb
Auth:     SQL Server Authentication or Windows Authentication
```

**Setup:** Each developer must create `GameOnDb` on their local SQL Server instance and run the schema + seed scripts to initialise the database.
