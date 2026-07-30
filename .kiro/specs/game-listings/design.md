# Game Listings - Design

## Architecture

```
Frontend (listings.html, listing-detail.html, create-listing.html)
  → fetch() /api/game-listings/*
    → GameListingController, JoinRequestController
      → GameListingService, JoinRequestService
        → GameListingDao, GameJoinerDao
```

## Database Tables

### [GameListing]
| Column | Type | Notes |
|--------|------|-------|
| gameListingId | INT IDENTITY(1,1) PK | |
| creatorId | INT FK → [User] | |
| formatId | INT FK → SportFormat | Determines sport, player count, positions |
| skillLevel | NVARCHAR(20) | 'Beginner', 'Intermediate', 'Advanced' |
| date | DATE | |
| time | TIME | |
| location | NVARCHAR(200) | |
| privacySetting | NVARCHAR(10) | 'public' or 'private' |
| status | NVARCHAR(20) | 'active', 'confirmed', 'completed', 'expired', 'cancelled' |
| createdAt | DATETIME2 | |

### [GameJoiner]
| Column | Type | Notes |
|--------|------|-------|
| userId | INT FK → [User] | Composite PK |
| gameListingId | INT FK → GameListing | Composite PK |
| team | NVARCHAR(10) | 'A' or 'B' |
| positionId | INT FK → Position (nullable) | Assigned position |
| alternativePositionId | INT FK → Position (nullable) | Second preference |
| status | NVARCHAR(20) | 'pending', 'accepted', 'rejected' |
| joinedAt | DATETIME2 | |

### [Position]
| Column | Type | Notes |
|--------|------|-------|
| positionId | INT IDENTITY(1,1) PK | |
| positionName | NVARCHAR(50) | e.g., "Goalkeeper", "Striker" |

### [FormatPosition]
| Column | Type | Notes |
|--------|------|-------|
| formatId | INT FK → SportFormat | Composite PK |
| positionId | INT FK → Position | Composite PK |

## Listing Lifecycle (Proposed)

```
[active] → (creator cancels) → [cancelled]
[active] → (time passes) → [expired]
[active] → (full + 2hrs before) → [confirmed]
[confirmed] → (game played, result recorded) → [completed]
```

**Group must confirm:** What happens if a listing isn't full at the 2-hour mark?

## API Endpoints

### POST /api/game-listings
Create a new listing. Request body:
```json
{
  "formatId": 1,
  "skillLevel": "Intermediate",
  "date": "2026-08-15",
  "time": "14:00",
  "location": "University Fields",
  "privacySetting": "public"
}
```

### GET /api/game-listings
Browse available listings. Query params: `sport`, `skillLevel`, `date`, `page`, `size`.

### GET /api/game-listings/{id}
Full detail including rosters.

### PUT /api/game-listings/{id}
Update listing (creator only).

### DELETE /api/game-listings/{id}
Cancel listing (creator only).

### POST /api/game-listings/{id}/join-requests
Send a join request:
```json
{
  "team": "A",
  "positionId": 3,
  "alternativePositionId": 5
}
```

### GET /api/game-listings/{id}/join-requests
View pending requests (creator only).

### PUT /api/game-listings/{id}/join-requests/{userId}
Accept or reject:
```json
{ "action": "accept" }
```

### DELETE /api/game-listings/{id}/participants/me
Leave the listing (participant).

## Key Classes

| Class | Package | Responsibility |
|-------|---------|---------------|
| GameListingController | controller | Listing CRUD routes |
| JoinRequestController | controller | Join request routes |
| GameListingService | service | Create, validate, lifecycle logic |
| JoinRequestService | service | Request handling, scheduling conflict check |
| GameListingDao | dao | CRUD on GameListing |
| GameJoinerDao | dao | CRUD on GameJoiner (participants + requests) |
| GameListing | model | Domain entity |
| GameJoiner | model | Domain entity |
| CreateListingRequest | dto | Incoming create payload |
| JoinRequestDto | dto | Incoming join request payload |

## Important Validation (GameListingService)

- Creator must have the selected sport on their profile.
- Creator cannot have another active listing.
- Date must be in the future.
- User must have the sport on profile to join.
- Scheduling conflict: check no joined listing within 3 hours.
- Team must not be full.

## Expiry and Confirmation (Implementation Options)

**Option A (Lazy/Query-time):** When fetching listings, check `date + time` against current time and exclude expired ones. Confirmation is checked at access time. No background job needed.

**Option B (Scheduled):** Background thread or timer runs periodically and updates statuses. More accurate timing but more complexity.

**Recommendation:** Start with Option A. Add a scheduled approach later if timing precision matters for notifications.
