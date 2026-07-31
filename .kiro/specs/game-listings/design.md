# Game Listings - Design (A100: Create Game Listing)

## Architecture

```
Frontend (create-listing.html + createListing.js)
  → fetch() → /api/users/me/sports, /api/sports/{id}/formats,
              /api/formats/{id}/positions, /api/users/me/friends,
              POST /api/game-listings
    → GameListingController (+ lookup controllers)
      → GameListingService (validation + transactional insert)
        → GameListingDao, GameJoinerDao, NotificationDao
```

## Database Tables Used (Existing — No Schema Changes)

### dbo.game_listing
| Column | Type | Notes |
|--------|------|-------|
| game_listing_id | bigint IDENTITY PK | |
| date | datetime2(7) | Combined date+time |
| is_completed | bit NOT NULL | false for new listings |
| is_private | bit NOT NULL | true=private, false=public |
| location | varchar(255) | |
| skill_level | varchar(255) | Beginner/Intermediate/Advanced |
| creator_id | bigint FK→users | From session userId |
| format_id | bigint FK→sport_format | |

### dbo.game_joiner
| Column | Type | Notes |
|--------|------|-------|
| game_listing_id | bigint (composite PK) | |
| user_id | bigint (composite PK) | |
| team | varchar(255) | Creator gets 'A' |
| status | varchar(255) | 'accepted' for creator |
| position_id | bigint (nullable FK) | Required when has_positions=true; NULL for non-positional formats |
| format_id | bigint (nullable) | Same as listing format |
| alternate_format_position | varchar(255) | Second preferred position (name/id as string) |

### dbo.notification
| Column | Type | Notes |
|--------|------|-------|
| notification_id | bigint IDENTITY PK | |
| is_read | bit | false for new |
| text | varchar(255) | Invitation message |
| type_of_notification | varchar(255) | 'game_invitation' |
| recipient_id | bigint FK→users | Invited friend |

**Note:** The notification table has no `game_listing_id` column. Invitation notifications will encode the listing ID in the text field (e.g., "You've been invited to Basketball 3v3 at Lorraine Court"). No schema migration needed for A100 — a migration to add a reference column can be added in a later use case if needed.

### Supporting lookup tables
- dbo.sport (sport_id, sport_name)
- dbo.sport_format (format_id, format_name, has_positions, no_players, sport_id)
- dbo.position (position_id, position_name)
- dbo.format_position (format_id, position_id)
- dbo.user_sport_profile (sport_id, user_id, skill_level, wins, losses)
- dbo.follow (followed_user_id, follower_user_id)

## API Design

### POST /api/game-listings
Request body:
```json
{
  "sportId": 3,
  "formatId": 7,
  "skillLevel": "Intermediate",
  "date": "2026-08-15",
  "time": "14:00",
  "location": "University Fields",
  "isPrivate": false,
  "anyPosition": false,
  "positionId": 5,
  "alternatePositionId": 8,
  "invitedFriendIds": [2, 4, 7]
}
```

Response (201):
```json
{
  "success": true,
  "data": {
    "gameListingId": 42,
    "sportName": "Basketball",
    "formatName": "3v3",
    "skillLevel": "Intermediate",
    "date": "2026-08-15T14:00:00",
    "location": "University Fields",
    "isPrivate": false,
    "capacity": 6,
    "invitedCount": 3
  }
}
```

### GET /api/users/me/sports
Response:
```json
{
  "success": true,
  "data": [
    { "sportId": 1, "sportName": "Football", "skillLevel": "Intermediate" },
    { "sportId": 3, "sportName": "Basketball", "skillLevel": "Advanced" }
  ]
}
```

### GET /api/sports/{sportId}/formats
Response:
```json
{
  "success": true,
  "data": [
    { "formatId": 7, "formatName": "3v3", "hasPositions": false, "noPlayers": 6 },
    { "formatId": 8, "formatName": "5v5", "hasPositions": true, "noPlayers": 10 }
  ]
}
```

### GET /api/formats/{formatId}/positions
Response:
```json
{
  "success": true,
  "data": [
    { "positionId": 1, "positionName": "Goalkeeper" },
    { "positionId": 2, "positionName": "Defender" }
  ]
}
```

### GET /api/users/me/friends
Response:
```json
{
  "success": true,
  "data": [
    { "userId": 2, "username": "Zane" },
    { "userId": 4, "username": "Gerard" }
  ]
}
```

## Key Classes

| Class | Package | Responsibility |
|-------|---------|---------------|
| GameListingController | controller | POST /api/game-listings |
| SportController | controller | GET sports, formats, positions lookups |
| FriendController | controller | GET /api/users/me/friends |
| UserSportController | controller | GET /api/users/me/sports |
| GameListingService | service | Validation (including 3-hour lead time, provisional) + transactional creation |
| GameListingDao | dao | Insert game_listing, check scheduling conflict (DATEDIFF_BIG SECOND < 7200) |
| GameJoinerDao | dao | Insert creator as participant |
| SportDao | dao | Find sports by user |
| SportFormatDao | dao | Find formats by sport |
| PositionDao | dao | Find positions by format |
| FollowDao | dao | Find mutual followers |
| NotificationDao | dao | Insert invitation notifications |
| GameListing | model | Domain entity |
| GameJoiner | model | Domain entity |
| Sport | model | Domain entity |
| SportFormat | model | Domain entity |
| Position | model | Domain entity |
| CreateListingRequest | dto | Incoming request payload |

## Transaction Strategy

```java
try (Connection conn = getConnection()) {
    conn.setAutoCommit(false);
    try {
        long listingId = insertGameListing(conn, ...);
        insertCreatorAsJoiner(conn, listingId, ...);
        insertNotifications(conn, listingId, ...);
        conn.commit();
    } catch (Exception e) {
        conn.rollback();
        throw e;
    }
}
```

The service method receives a DataSource, obtains one connection, and passes it to DAO methods that accept a Connection parameter (overloaded or separate from the standard methods).

## Frontend Flow

Single-page multi-step form (`create-listing.html`):
- Step 1: Privacy, Sport, Format, Skill Level, Date, Time, Location
- Step 2: Position selection (conditional on has_positions)
- Step 3: Invite friends (optional; displays "[N] selected · [remaining] spaces available"; invitation limit = no_players - 1)
- Step 4: Confirm summary + Create Listing button

### Capacity Display Rules
- Listing cards and confirmation summary show: `accepted participants / sport_format.no_players`
- The creator is the first accepted participant, so a newly created listing displays `1/[no_players]`
- Pending invitation notifications do NOT increase the participant count
- Invited users count only after they accept and are inserted into game_joiner with status='accepted'

JavaScript handles step navigation, API calls for dropdowns, and final submission.
