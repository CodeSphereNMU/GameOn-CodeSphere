# Player Profiles & Sports - Design

## Architecture

```
Frontend (profile.html, add-sport.html)
  → fetch() /api/profiles/*, /api/sports/*
    → ProfileController, SportController
      → ProfileService, SportService
        → UserDao, UserSportProfileDao, SportDao, FollowDao
```

## Database Tables

### [Sport]
| Column | Type | Notes |
|--------|------|-------|
| sportId | INT IDENTITY(1,1) PK | |
| sportName | NVARCHAR(50) UNIQUE | e.g., "Soccer", "Basketball" |
| noPlayers | INT | Default total players for the sport |

### [SportFormat]
| Column | Type | Notes |
|--------|------|-------|
| formatId | INT IDENTITY(1,1) PK | |
| sportId | INT FK → Sport | |
| formatName | NVARCHAR(50) | e.g., "5-a-side", "11-a-side" |
| noPlayers | INT | Players needed for this format |
| hasPositions | BIT | Whether this format uses positions |

### [UserSportProfile]
| Column | Type | Notes |
|--------|------|-------|
| userId | INT FK → [User] | Composite PK |
| sportId | INT FK → Sport | Composite PK |
| skillLevel | NVARCHAR(20) | 'Beginner', 'Intermediate', 'Advanced' |
| wins | INT DEFAULT 0 | |
| losses | INT DEFAULT 0 | |

### [Follow]
| Column | Type | Notes |
|--------|------|-------|
| followerUserId | INT FK → [User] | Composite PK |
| followedUserId | INT FK → [User] | Composite PK |
| createdAt | DATETIME2 | |

## API Endpoints

### GET /api/profiles/{userId}
Returns a user's public profile with sports and stats.

### PUT /api/profiles/me
Updates the current user's username.

### GET /api/profiles/me/sports
Returns the current user's sport entries.

### POST /api/profiles/me/sports
Adds a sport to the current user's profile.
```json
{ "sportId": 1, "skillLevel": "Intermediate" }
```

### DELETE /api/profiles/me/sports/{sportId}
Removes a sport from the current user's profile.

### PUT /api/profiles/me/sports/{sportId}
Updates skill level for an existing sport.
```json
{ "skillLevel": "Advanced" }
```

### POST /api/profiles/{userId}/follow
Follows the specified user.

### DELETE /api/profiles/{userId}/follow
Unfollows the specified user.

### GET /api/sports
Returns all available sports (for selection UI).

## Key Classes

| Class | Package | Responsibility |
|-------|---------|---------------|
| ProfileController | controller | Profile routes |
| SportController | controller | Sport catalog routes |
| ProfileService | service | Profile logic, follow/unfollow |
| UserSportProfileDao | dao | CRUD on UserSportProfile |
| SportDao | dao | Read-only access to Sport table |
| FollowDao | dao | Follow/unfollow operations |
| UserSportProfile | model | Domain entity |
| Sport | model | Domain entity |
| Follow | model | Domain entity |

## Seed Data

The `Sport` and `SportFormat` tables should be pre-populated via a migration with common sports (Soccer, Basketball, Cricket, Tennis, Rugby, Hockey, etc.) and their formats. This is configuration data, not user data.
