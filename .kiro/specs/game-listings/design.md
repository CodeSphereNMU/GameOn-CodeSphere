# Game Listings - Design (A100: Create Game Listing)

## Architecture

```
Frontend (create-listing.html + createListing.js)
  → fetch() → /api/users/me/sports, /api/sports/{id}/formats,
              /api/formats/{id}/positions, /api/users/me/friends,
              POST /api/game-listings
    → GameListingController (+ lookup controllers)
      → GameListingService (validation + transactional insert)
        → GameListingDao, GameJoinerDao, InvitationDao, NotificationDao
```

## Database Tables Used

### dbo.game_listing (V3 schema)
| Column | Type | Notes |
|--------|------|-------|
| game_listing_id | bigint IDENTITY PK | |
| date | datetime2(7) | Session start time |
| end_time | datetime2(7) NOT NULL | Calculated: date + duration_minutes |
| status | varchar(50) NOT NULL | CHECK: OPEN, CONFIRMED, CANCELLED_*, COMPLETED |
| is_private | bit NOT NULL | |
| location | varchar(255) | |
| skill_level | varchar(255) | |
| creator_id | bigint FK→users | |
| format_id | bigint FK→sport_format | |

### dbo.sport_format (V3 schema)
| Column | Type | Notes |
|--------|------|-------|
| format_id | bigint IDENTITY PK | |
| format_name | varchar(255) | |
| has_positions | bit NOT NULL | |
| no_players | int | Total capacity (both teams) |
| duration_minutes | int NOT NULL | Game On session duration |
| sport_id | bigint FK→sport | |

### dbo.game_joiner (V3 schema)
| Column | Type | Notes |
|--------|------|-------|
| game_listing_id | bigint (composite PK) | |
| user_id | bigint (composite PK) | |
| team | varchar(10) NOT NULL | CHECK: A, B |
| status | varchar(20) NOT NULL | CHECK: ACCEPTED, WITHDRAWN |
| position_id | bigint (nullable) | First position preference |
| format_id | bigint NOT NULL | Must match listing's format_id |
| alternate_position_id | bigint (nullable) | Second position preference |
| join_request_id | bigint (nullable) | NULL for creator |

**Composite foreign keys on game_joiner:**
- `(game_listing_id, format_id) → game_listing.(game_listing_id, format_id)` — enforces format consistency
- `(format_id, position_id) → format_position.(format_id, position_id)` — primary position belongs to format
- `(format_id, alternate_position_id) → format_position.(format_id, position_id)` — alternate position belongs to format
- `(join_request_id, game_listing_id, user_id) → join_request.(join_request_id, game_listing_id, user_id)` — identity: same listing and user

### dbo.invitation (V3 — new)
| Column | Type | Notes |
|--------|------|-------|
| invitation_id | bigint IDENTITY PK | |
| game_listing_id | bigint FK | |
| invitee_id | bigint FK→users | |
| status | varchar(20) NOT NULL | CHECK: PENDING, ACCEPTED, DECLINED, EXPIRED |
| created_at | datetime2(7) | |
| UNIQUE(game_listing_id, invitee_id) | | One invitation per user per listing |
| UNIQUE(invitation_id, game_listing_id, invitee_id) | | Supports composite FK from join_request |

### dbo.join_request (V3 — new, future use case foundation)
| Column | Type | Notes |
|--------|------|-------|
| join_request_id | bigint IDENTITY PK | |
| game_listing_id | bigint NOT NULL | |
| user_id | bigint FK→users | |
| format_id | bigint NOT NULL | Must match listing's format_id |
| team | varchar(10) NOT NULL | CHECK: A, B |
| position_id | bigint NULL | |
| alternate_position_id | bigint NULL | |
| invitation_id | bigint NULL | Non-null = invited-user request |
| status | varchar(20) NOT NULL | CHECK: PENDING, ACCEPTED, REJECTED, WITHDRAWN, EXPIRED |
| created_at | datetime2(7) | |
| updated_at | datetime2(7) | |
| Filtered unique index | (listing, user) WHERE status='PENDING' | One pending per user per listing |

**Composite foreign keys on join_request:**
- `(game_listing_id, format_id) → game_listing.(game_listing_id, format_id)` — format consistency
- `(format_id, position_id) → format_position.(format_id, position_id)` — position belongs to format
- `(format_id, alternate_position_id) → format_position.(format_id, position_id)` — alternate belongs to format
- `(invitation_id, game_listing_id, user_id) → invitation.(invitation_id, game_listing_id, invitee_id)` — identity: same listing and user

### dbo.notification (V3 enhanced)
| Column | Type | Notes |
|--------|------|-------|
| notification_id | bigint IDENTITY PK | |
| is_read | bit | |
| text | varchar(255) | |
| type_of_notification | varchar(255) | 'game_invitation' |
| recipient_id | bigint FK→users | |
| game_listing_id | bigint NULL FK→game_listing | Links to listing |
| created_at | datetime2(7) | |

## API Design

### POST /api/game-listings
Request:
```json
{
  "sportId": 3,
  "formatId": 7,
  "skillLevel": "Intermediate",
  "date": "2026-08-15",
  "time": "14:00",
  "location": "University Fields",
  "isPrivate": false,
  "team": "A",
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
    "endTime": "2026-08-15T15:00:00",
    "sessionWindow": "14:00–15:00",
    "location": "University Fields",
    "isPrivate": false,
    "capacity": 6,
    "team": "A",
    "invitedCount": 3
  }
}
```

## Transaction Strategy

```java
conn.setAutoCommit(false);
1. Check scheduling conflict (bidirectional session+buffer overlap)
2. Insert game_listing (status=OPEN, end_time calculated)
3. Insert creator game_joiner (ACCEPTED, selected team, positions)
4. Insert PENDING invitation records
5. Insert notification records (with game_listing_id)
conn.commit();
```

## Frontend Flow

4-step wizard (`create-listing.html`):
- Step 1: Privacy, Sport, Format, Skill Level, Date, Time, Location
- Step 2: Team selection (A/B) + Position selection (conditional on has_positions)
- Step 3: Invite friends (no capacity limit; shows "X selected")
- Step 4: Confirm summary with session window (e.g. "14:00–15:00") + Create button

## Future Use Case Foundations (schema only, not implemented)

**V3 Status:** Created and implemented in code, then reviewed and successfully applied to GameOnDB. The earlier pending-review stage was completed before manual end-to-end testing.

- **join_request**: Tracks join request lifecycle. Not used by Create Listing.
- **invitation response processing**: invitation.status transitions handled by future use cases.
- **Participant withdrawal**: game_joiner.status → WITHDRAWN, row updated in place.
- **Lock-in processing**: 2 hours before start, automatic CONFIRMED/CANCELLED transitions.
- **Match results**: Only for COMPLETED listings. Scores non-negative, no winners column.

## Cross-Table Identity (database-enforced)

These relationships are enforced at the database level using composite UNIQUE keys and composite foreign keys:

- **game_joiner → join_request identity:** `FK (join_request_id, game_listing_id, user_id) → join_request (join_request_id, game_listing_id, user_id)`. Prevents a game_joiner row from referencing a join request for a different listing or user.

- **join_request → invitation identity:** `FK (invitation_id, game_listing_id, user_id) → invitation (invitation_id, game_listing_id, invitee_id)`. Prevents a join request from referencing another user's invitation or an invitation for another listing.

- **game_joiner format consistency:** `FK (game_listing_id, format_id) → game_listing (game_listing_id, format_id)`. Prevents format_id from differing from the listing's format.

- **Position format scoping:** Composite FKs `(format_id, position_id)` and `(format_id, alternate_position_id)` → `format_position` ensure both positions belong to the format. Applied to both game_joiner and join_request.
