# Design Document — A300: Send Join Request

## Overview

This design covers the backend service, DAO, controller, and frontend form required for a player to submit a join request to an existing game listing. The implementation follows the established GameOn architecture: Controller → Service → DAO with transactional coordination, DTOs for API contracts, and vanilla HTML/CSS/JS for the frontend.

No database migration is required — the `join_request` table and all supporting indexes/constraints were created in V3.

## Architecture

```
Frontend (listing-detail.html + listingDetail.js)
  → fetch() → POST /api/game-listings/{id}/join-requests
    → JoinRequestController
      → JoinRequestService (validation + transactional insert)
        → JoinRequestDao (insert)
        → GameListingDao (findById, hasSchedulingConflict)
        → GameJoinerDao (isAcceptedJoiner)
        → InvitationDao (findPendingInvitation)
        → SportDao (userHasSport)
        → SportFormatDao (findById)
        → PositionDao (positionBelongsToFormat)
        → NotificationDao (insertBatch)
```

## Components and Interfaces

### JoinRequestController

Handles HTTP routing for join request endpoints. Registered in `JavalinConfig`.

```java
public class JoinRequestController {
    private final JoinRequestService joinRequestService;

    public JoinRequestController(JoinRequestService joinRequestService) { ... }

    public void register(Javalin app) {
        app.post("/api/game-listings/{id}/join-requests", this::createJoinRequest);
    }

    private void createJoinRequest(Context ctx) {
        // 1. Check authentication (ctx.sessionAttribute("userId"))
        // 2. Parse path param {id} as listing ID
        // 3. Parse JSON body as JoinRequestRequest DTO
        // 4. Call service.createJoinRequest(userId, listingId, request)
        // 5. Return 201 with ApiResponse.success(response)
    }
}
```

### JoinRequestService

Coordinates all validation and the transactional insert. Accepts an injectable `Clock` for testability (same pattern as `GameListingService`).

```java
public class JoinRequestService {
    private static final long LOCK_IN_HOURS = 2;
    private static final Set<String> VALID_TEAMS = Set.of("A", "B");

    // Constructor dependencies:
    // DataSource, GameListingDao, GameJoinerDao, JoinRequestDao,
    // InvitationDao, SportDao, SportFormatDao, PositionDao,
    // NotificationDao, Clock

    public JoinRequestResponse createJoinRequest(long userId, long listingId, JoinRequestRequest request) {
        // Validation order:
        // 1. Team validation (required, A or B)
        // 2. Fetch listing (404 if not found)
        // 3. Creator check (cannot join own listing)
        // 4. Status check (must be OPEN)
        // 5. Lock-in check (current time < start - 2 hours)
        // 6. Fetch format (for position rules)
        // 7. Position validation (format-dependent)
        //
        // Transactional section (conn.setAutoCommit(false)):
        // 8. Invitation lookup (InvitationDao.findPendingInvitationId using txn connection)
        // 9. Sport on profile check (bypassed if step 8 found a PENDING invitation)
        // 10. Scheduling conflict check
        // 11. Already-accepted check (game_joiner with ACCEPTED)
        // 12. Already-pending check (join_request with PENDING)
        // 13. Insert join_request (status=PENDING, invitation_id from step 8)
        // 14. Insert notification for listing creator (type="join_request", recipient=creator)
        // conn.commit()
    }
}
```

**Design Decision — Validation Order:** Non-transactional checks (team format, listing existence, status, lock-in, positions) run before opening a transaction to fail fast. Checks requiring transactional consistency (invitation lookup, sport eligibility, scheduling conflict, duplicate detection) run inside the transaction to prevent race conditions.

**Design Decision — No Capacity Check at Submission:** Team capacity is intentionally not checked when a join request is submitted. An otherwise eligible user may request either team even when it is full. The request remains PENDING and does not reserve a place or increase capacity. Capacity is enforced when the creator accepts a request (C500).

### JoinRequestDao

New DAO class extending `BaseDao`. Handles inserts and queries against the `join_request` table.

```java
public class JoinRequestDao extends BaseDao {

    public JoinRequestDao(DataSource dataSource) { super(dataSource); }

    /**
     * Inserts a PENDING join request. Returns the generated join_request_id.
     * Uses the provided connection for transactional consistency.
     */
    public long insert(Connection conn, JoinRequest joinRequest) throws SQLException { ... }

    /**
     * Checks whether the user already has a PENDING join_request for the given listing.
     * Runs within the caller's transaction.
     */
    public boolean hasPendingRequest(Connection conn, long gameListingId, long userId) throws SQLException { ... }
}
```

### GameJoinerDao — New Method

```java
/**
 * Checks whether a user is already an ACCEPTED game_joiner on the given listing.
 * Uses the provided connection for transactional consistency.
 */
public boolean isAcceptedJoiner(Connection conn, long gameListingId, long userId) throws SQLException { ... }
```

### InvitationDao — New Method

```java
/**
 * Finds the PENDING invitation for a given listing and user.
 * Returns the invitation_id if one exists, or null otherwise.
 * Uses the provided connection for transactional consistency.
 */
public Long findPendingInvitationId(Connection conn, long gameListingId, long userId) throws SQLException { ... }
```

## Data Models

### JoinRequest (model)

```java
public class JoinRequest {
    private long joinRequestId;
    private long gameListingId;
    private long userId;
    private long formatId;
    private String team;          // "A" or "B"
    private Long positionId;      // nullable
    private Long alternatePositionId; // nullable
    private Long invitationId;    // nullable — set when player was invited
    private String status;        // PENDING, ACCEPTED, REJECTED, WITHDRAWN, EXPIRED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### JoinRequestRequest (DTO — request body)

```java
public class JoinRequestRequest {
    private String team;             // required: "A" or "B"
    private Long positionId;         // optional
    private Long alternatePositionId; // optional
}
```

### JoinRequestResponse (DTO — response body)

```java
public class JoinRequestResponse {
    private long joinRequestId;
    private long gameListingId;
    private String team;
    private Long positionId;
    private Long alternatePositionId;
    private String status;           // "PENDING"
    private boolean invitationLinked; // true if invitation_id is non-null
}
```

## Transaction Strategy

```java
// Pre-transaction validation (fail fast, no DB connection held):
// - team format, listing existence, creator check, status, lock-in, positions

conn = dataSource.getConnection();
conn.setAutoCommit(false);
try {
    // 1. Invitation lookup (InvitationDao.findPendingInvitationId — uses txn connection)
    // 2. Sport on profile check (bypassed if step 1 found a PENDING invitation)
    // 3. Check scheduling conflict (reuses GameListingDao.hasSchedulingConflict)
    // 4. Check not already accepted (GameJoinerDao.isAcceptedJoiner)
    // 5. Check no pending request (JoinRequestDao.hasPendingRequest)
    // 6. Insert join_request row (PENDING, format_id from listing, invitation_id from step 1)
    // 7. Insert creator notification (type="join_request", game_listing_id, recipient=creator)
    conn.commit();
} catch (ApiException e) {
    conn.rollback();
    throw e;
} catch (Exception e) {
    conn.rollback();
    throw new RuntimeException("Failed to create join request", e);
}
```

**Design Decision — Invitation Lookup Inside Transaction:** The invitation lookup uses the active transaction connection and runs as the first transactional step. Its result serves two purposes: (1) determining whether the sport-profile requirement is bypassed, and (2) setting `invitation_id` on the new join_request. Performing the lookup within the transaction ensures the invitation status is consistent with the subsequent insert. The composite FK on `join_request` verifies the invitation/listing/user relationship but does not verify that the invitation still has PENDING status, so the application-level check remains necessary.

## API Design

### POST /api/game-listings/{id}/join-requests

**Request:**
```json
{
  "team": "A",
  "positionId": 5,
  "alternatePositionId": 8
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "joinRequestId": 17,
    "gameListingId": 42,
    "team": "A",
    "positionId": 5,
    "alternatePositionId": 8,
    "status": "PENDING",
    "invitationLinked": true
  }
}
```

**Error Response (400):**
```json
{
  "success": false,
  "error": "You already have a pending request for this listing"
}
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system — essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Valid join requests produce PENDING rows with correct fields

*For any* valid combination of team (A or B), optional position IDs belonging to the format, and an eligible player (has sport or has invitation, no conflict, no pending request, not already accepted), the service SHALL return a response with status "PENDING", the selected team, the provided positions, and the listing's format_id.

**Validates: Requirements 1.1, 1.2, 5.3**

### Property 2: Creator self-join rejection

*For any* listing and the user who created it, submitting a join request SHALL result in an ApiException with status 400 and message "Cannot join your own listing".

**Validates: Requirements 2.3**

### Property 3: Non-OPEN listing rejection

*For any* listing whose status is not OPEN (CONFIRMED, CANCELLED_INSUFFICIENT_PLAYERS, CANCELLED_BY_CREATOR, COMPLETED), submitting a join request SHALL result in an ApiException with status 400.

**Validates: Requirements 2.4**

### Property 4: Lock-in enforcement

*For any* listing where the current time is at or past 2 hours before the start time, submitting a join request SHALL result in an ApiException with status 400. Conversely, for any listing where the current time is more than 2 hours before start, the lock-in check SHALL pass.

**Validates: Requirements 2.5**

### Property 5: Sport eligibility enforcement

*For any* player who does not have the listing's sport on their profile AND does not have a PENDING invitation for the listing, submitting a join request SHALL result in an ApiException with status 400. Conversely, a player with a PENDING invitation SHALL pass the sport check regardless of their sport profile.

**Validates: Requirements 2.6**

### Property 6: Scheduling conflict enforcement

*For any* player with an existing session whose conflict zone overlaps with the target listing's conflict zone, submitting a join request SHALL result in an ApiException with status 400.

**Validates: Requirements 3.1, 3.2**

### Property 7: Invalid team rejection

*For any* team value that is not exactly "A" or "B" (including null, empty string, lowercase, multi-character), the service SHALL reject with ApiException status 400.

**Validates: Requirements 4.1**

### Property 8: Position format scoping

*For any* positional format, if position_id is not provided the service SHALL reject with ApiException status 400. For any position_id that does not belong to that format, the service SHALL reject with ApiException status 400. Similarly, for any alternate_position_id not belonging to the format or equal to position_id, the service SHALL reject.

**Validates: Requirements 4.2, 4.3, 4.4, 4.6, 4.7, 4.8**

### Property 9: Non-positional format ignores positions

*For any* non-positional format, regardless of what position values are submitted, the stored join_request SHALL have NULL position_id and NULL alternate_position_id.

**Validates: Requirements 4.5**

### Property 10: Invitation linking

*For any* player with a PENDING invitation for the listing, the created join_request SHALL have invitation_id set to that invitation's ID. For any player without a PENDING invitation, invitation_id SHALL be NULL.

**Validates: Requirements 1.4**

### Property 11: Creator notification on successful request

*For any* successfully created join request, exactly one notification SHALL be created within the same transaction with recipient_id equal to the listing creator's user_id, type_of_notification "join_request", and game_listing_id matching the listing. If the transaction rolls back, the notification SHALL not be persisted. The requester SHALL NOT receive a notification.

**Validates: Requirements 1.3, 5.1**

## Error Handling

| Condition | HTTP Status | Error Message |
|-----------|-------------|---------------|
| Not authenticated | 401 | "Login required" |
| Invalid listing ID format | 400 | "Invalid listing ID" |
| Listing not found | 404 | "Listing not found" |
| Player is creator | 400 | "Cannot join your own listing" |
| Listing not OPEN | 400 | "Listing is not open for join requests" |
| Past lock-in | 400 | "Listing has passed lock-in and is no longer accepting requests" |
| Sport not on profile and no invitation | 400 | "Selected sport is not on your profile" |
| Invalid team | 400 | "Team selection is required (A or B)" |
| Missing position (positional format) | 400 | "A position selection is required for this format" |
| Invalid position | 400 | "Selected position does not belong to the chosen format" |
| Invalid alternate position | 400 | "Selected alternate position does not belong to the chosen format" |
| Duplicate positions | 400 | "First and second position preferences must be different" |
| Alt without primary | 400 | "Alternate position requires a primary position selection" |
| Scheduling conflict | 400 | "Scheduling conflict: the proposed session overlaps with an existing session and its travel buffer" |
| Already accepted | 400 | "You are already a participant in this listing" |
| Already pending | 400 | "You already have a pending request for this listing" |
| Unexpected error | 500 | "An unexpected error occurred. Please try again later." |

All errors roll back any open transaction. The central `ApiException` handler in `JavalinConfig` formats them into the standard `{ success: false, error: "..." }` shape.

## Testing Strategy

### Unit Tests (JoinRequestServiceTest)

Test the service layer in isolation using fake DAOs and an injectable Clock. Focus on:

- Each validation rule individually (one test per error path)
- Happy path with various valid inputs
- Lock-in boundary (exactly 2 hours = rejected, 2 hours + 1 second = allowed)
- Position validation for positional vs non-positional formats
- Invitation linking logic
- Creator notification creation on success
- No notification persisted when transaction rolls back
- Transaction rollback on failure (verify no side effects)

**Framework:** JUnit 5 with fake DAO implementations (following existing GameListingServiceTest patterns).

### Integration Tests (Future)

- End-to-end test with real database verifying the full request lifecycle
- Verify the filtered unique index prevents concurrent duplicate PENDING requests
- Verify composite FK enforcement on invalid format_id or invitation_id

## Frontend Design

### Location

The join request form is added to the existing listing detail page (`frontend/pages/listing-detail.html`). It appears below the roster section when the authenticated user is eligible to join.

### UI Components

1. **Join Request Section** — visible when user is not the creator and not already accepted
2. **Team Selection** — two radio buttons or cards showing "Team A (X/Y filled)" and "Team B (X/Y filled)"
3. **Position Selection** — conditional on `has_positions`; primary dropdown + optional alternate dropdown
4. **Submit Button** — "Send Join Request", disabled during loading
5. **Status Display** — shows "Request Pending" badge if user already has a PENDING request
6. **Error Display** — inline error message area below the form

### JavaScript (`listingDetail.js` — additions)

```javascript
// On page load (after fetching listing detail):
// 1. Check if current user is creator → hide join form
// 2. Check if user is already accepted → hide join form, show "You're in" badge
// 3. Check if user has pending request → show "Request Pending" status
// 4. Otherwise → show join form with team counts and positions

// On form submit:
// 1. Gather team + optional positions
// 2. POST /api/game-listings/{id}/join-requests
// 3. On 201 → show success, hide form, show pending badge
// 4. On error → show error message
```

### Listing Detail Response Enhancement

The existing `ListingDetailDto` must be extended with four user-specific display fields so the frontend can determine what to show without inferring status from usernames:

- `formatId` (long) — needed to fetch positions for the join form via `GET /api/formats/{formatId}/positions`
- `isCreator` (boolean) — true if the authenticated user is the listing creator
- `isAcceptedParticipant` (boolean) — true if the user is an ACCEPTED game_joiner on the listing
- `hasPendingRequest` (boolean) — true if the user has a PENDING join_request for the listing

These fields control frontend display logic only. They do not replace authoritative validation in `JoinRequestService`.
