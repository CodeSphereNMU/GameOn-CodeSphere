# Player Profiles & Sports - Design

## Architecture

```
Frontend (profile.html, add-sport.html — not yet created)
  → fetch() /api/profiles/*, /api/sports/*, /api/users/me/sports
    → ProfileController (not yet created), SportController, UserSportController
      → ProfileService (not yet created), SportService (not yet created)
        → UserDao, SportDao, FollowDao, UserSportProfileDao (to be created)
```

## Database Tables (Existing — V1 + V2)

### dbo.sport
| Column | Type | Notes |
|--------|------|-------|
| sport_id | BIGINT IDENTITY(1,1) PK | |
| sport_name | VARCHAR(255) NOT NULL | Seeded: Padel, Tennis, Basketball, Rugby, Football |

There is no `no_players` column on `sport`. Capacity belongs to `sport_format.no_players`.

### dbo.sport_format
| Column | Type | Notes |
|--------|------|-------|
| format_id | BIGINT IDENTITY(1,1) PK | |
| format_name | VARCHAR(255) | e.g., "5v5", "Doubles" |
| has_positions | BIT NOT NULL | Whether this format uses positions |
| no_players | INT | Total capacity (both teams) |
| duration_minutes | INT NOT NULL | Session duration (added in V3) |
| sport_id | BIGINT FK → sport | |

### dbo.user_sport_profile
| Column | Type | Notes |
|--------|------|-------|
| sport_id | BIGINT FK → sport | Composite PK |
| user_id | BIGINT FK → users | Composite PK |
| skill_level | VARCHAR(255) | 'Beginner', 'Intermediate', 'Advanced' |
| wins | INT NULL | |
| losses | INT NULL | |

### dbo.follow
| Column | Type | Notes |
|--------|------|-------|
| followed_user_id | BIGINT FK → users | Composite PK |
| follower_user_id | BIGINT FK → users | Composite PK |

## Existing Reusable Components

These were built to support A100 (Create Game Listing) and can be reused:

| Component | What it provides |
|-----------|-----------------|
| `SportDao` | `findAll()`, sport queries |
| `SportFormatDao` | Format queries by sport |
| `PositionDao` | Position queries by format |
| `FollowDao` | `findMutualFollowerIds(long userId)` |
| `UserSportController` | `GET /api/users/me/sports` — returns user's sports |
| `SportController` | `GET /api/sports/{id}/formats`, `GET /api/formats/{id}/positions` |
| `FriendController` | `GET /api/users/me/friends` — mutual followers |

## API Endpoints (Planned)

### Profile Management
| Method | Path | Purpose | Status |
|--------|------|---------|--------|
| GET | /api/profiles/{userId} | View a user's profile | Planned |
| PUT | /api/profiles/me | Update username | Planned |

### Sport Management
| Method | Path | Purpose | Status |
|--------|------|---------|--------|
| GET | /api/users/me/sports | User's sport entries | Implemented |
| POST | /api/profiles/me/sports | Add sport to profile | Planned |
| DELETE | /api/profiles/me/sports/{sportId} | Remove sport | Planned |
| PUT | /api/profiles/me/sports/{sportId} | Update skill level | Planned |

### Follow
| Method | Path | Purpose | Status |
|--------|------|---------|--------|
| POST | /api/profiles/{userId}/follow | Follow user | Planned |
| DELETE | /api/profiles/{userId}/follow | Unfollow user | Planned |

### Sport Catalogue (reused from A100)
| Method | Path | Purpose | Status |
|--------|------|---------|--------|
| GET | /api/sports/{sportId}/formats | Formats for a sport | Implemented |
| GET | /api/formats/{formatId}/positions | Positions for a format | Implemented |
| GET | /api/users/me/friends | Mutual followers | Implemented |

## Components to Create

| Class | Package | Responsibility |
|-------|---------|---------------|
| ProfileController | controller | Profile routes (view, update username, follow/unfollow) |
| ProfileService | service | Profile logic, username update, follow/unfollow |
| SportService | service | Add/remove/update sport on profile, validation |
| UserSportProfileDao | dao | CRUD on user_sport_profile |

## Design Notes

- Use `long` for all identifiers (BIGINT in database).
- The `sport` table has no `noPlayers` column. Do not reference `Sport.noPlayers`.
- Sport removal behaviour when the user has active listings is unresolved (see `docs/unresolved-questions.md`).
- Win/loss tracking approach (cached vs derived) is unresolved.
- Username validation rules depend on the same group decision as registration (see authentication spec).
