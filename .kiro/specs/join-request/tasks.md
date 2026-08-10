# Implementation Plan: A300 — Send Join Request

## Overview

Implements the join request submission flow: a new `JoinRequestDao`, `JoinRequestService`, `JoinRequestController`, associated DTOs and model, unit tests, and frontend form integration on the listing detail page. No database migration is needed — the `join_request` table already exists from V3.

## Tasks

- [x] 1. JoinRequest model and JoinRequestDao
  - [x] 1.1 Create JoinRequest model class
    - Create `backend/src/main/java/com/codesphere/gameon/model/JoinRequest.java`
    - Fields: joinRequestId, gameListingId, userId, formatId, team, positionId, alternatePositionId, invitationId, status, createdAt, updatedAt
    - No-arg constructor, all-arg constructor, getters and setters
    - Follow existing model patterns (GameJoiner, GameListing)
    - _Requirements: 1.1, 5.3_

  - [x] 1.2 Create JoinRequestDao class
    - Create `backend/src/main/java/com/codesphere/gameon/dao/JoinRequestDao.java`
    - Extends `BaseDao`
    - Method: `long insert(Connection conn, JoinRequest joinRequest)` — inserts a PENDING row, returns generated ID
    - Method: `boolean hasPendingRequest(Connection conn, long gameListingId, long userId)` — checks filtered unique index condition
    - Use parameterised SQL with `[dbo].[join_request]` table
    - _Requirements: 1.1, 2.7, 5.1_

  - [x] 1.3 Add new methods to GameJoinerDao
    - Method: `boolean isAcceptedJoiner(Connection conn, long gameListingId, long userId)` — checks if user is already accepted
    - Accepts a Connection parameter for transactional use
    - _Requirements: 2.8_

  - [x] 1.4 Add findPendingInvitationId to InvitationDao
    - Method: `Long findPendingInvitationId(Connection conn, long gameListingId, long userId)` — returns invitation_id if a PENDING invitation exists, else null
    - Uses the caller's transactional connection for consistency
    - _Requirements: 1.4, 2.6_

- [x] 2. JoinRequestService (business logic and validation)
  - [x] 2.1 Create JoinRequestRequest DTO
    - Create `backend/src/main/java/com/codesphere/gameon/dto/JoinRequestRequest.java`
    - Fields: team (String), anyPosition (boolean), positionId (Long), alternatePositionId (Long)
    - When `anyPosition = true`: both positionId and alternatePositionId must be null; positions stored as NULL in database
    - When `anyPosition = false`: positionId is required for positional formats (existing behaviour)
    - _Requirements: 6.4_

  - [x] 2.2 Create JoinRequestResponse DTO
    - Create `backend/src/main/java/com/codesphere/gameon/dto/JoinRequestResponse.java`
    - Fields: joinRequestId, gameListingId, team, positionId, alternatePositionId, status, invitationLinked (boolean)
    - _Requirements: 1.2, 6.2_

  - [x] 2.3 Create JoinRequestService class
    - Create `backend/src/main/java/com/codesphere/gameon/service/JoinRequestService.java`
    - Constructor: DataSource, GameListingDao, GameJoinerDao, JoinRequestDao, InvitationDao, SportDao, SportFormatDao, PositionDao, NotificationDao, Clock
    - Production constructor with Clock.systemDefaultZone() and test constructor with injectable Clock
    - Method: `JoinRequestResponse createJoinRequest(long userId, long listingId, JoinRequestRequest request)`
    - Implement validation order as specified in design:
      - Team validation (A/B required)
      - Listing existence (404 if not found)
      - Creator check (cannot join own)
      - Status check (must be OPEN)
      - Lock-in check (2 hours before start)
      - Format lookup and position validation
    - Transactional section:
      - Invitation lookup (using txn connection; result used for sport bypass and invitation_id)
      - Sport on profile check (bypassed if invitation found)
      - Scheduling conflict
      - Already-accepted check
      - Already-pending check
      - Insert join_request (invitation_id from lookup)
      - Insert creator notification (type=join_request, recipient=listing creator, game_listing_id)
    - _Requirements: 1.1, 1.3, 1.4, 2.1–2.8, 3.1, 3.2, 4.1–4.9, 5.1–5.3_

- [x] 3. JoinRequestController (endpoint registration)
  - [x] 3.1 Create JoinRequestController class
    - Create `backend/src/main/java/com/codesphere/gameon/controller/JoinRequestController.java`
    - Constructor: JoinRequestService
    - Method: `register(Javalin app)` — registers POST /api/game-listings/{id}/join-requests
    - Handler: parse userId from session, parse listing ID from path, parse body as JoinRequestRequest, call service, return 201 with ApiResponse.success(response)
    - _Requirements: 6.1, 6.2, 6.3_

  - [x] 3.2 Wire JoinRequestController in JavalinConfig
    - Instantiate JoinRequestDao in `registerRoutes`
    - Instantiate JoinRequestService with all dependencies
    - Instantiate JoinRequestController and call `.register(app)`
    - _Requirements: 6.1_

  - [x] 3.3 Extend ListingDetailDto and BrowseListingService for join-form data
    - Add fields to `ListingDetailDto`: formatId (long), isCreator (boolean), isAcceptedParticipant (boolean), hasPendingRequest (boolean)
    - Update `BrowseListingService.getListingDetail` to populate:
      - `formatId` from `listing.getFormatId()`
      - `isCreator` from `listing.getCreatorId() == userId`
      - `isAcceptedParticipant` via `GameJoinerDao.isAcceptedJoiner` (new method from task 1.3, but can use non-transactional overload or separate query)
      - `hasPendingRequest` via `JoinRequestDao.hasPendingRequest` (needs non-transactional overload or a read-only query method)
    - These are display hints only; JoinRequestService performs authoritative validation
    - _Requirements: 7.1, 7.7_

- [x] 4. Checkpoint — Backend compiles and routes are registered
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Unit tests for JoinRequestService
  - [x] 5.1 Write validation rule tests
    - Test: creator cannot join own listing (400)
    - Test: non-OPEN listing rejected (CONFIRMED, CANCELLED_*, COMPLETED)
    - Test: lock-in boundary — exactly 2 hours before start = rejected
    - Test: lock-in boundary — 2 hours + 1 second before start = allowed
    - Test: sport not on profile and no invitation = rejected (400)
    - Test: sport not on profile but has PENDING invitation = allowed
    - Test: invalid team (null, empty, lowercase, "C") = rejected (400)
    - Use fake DAOs and injectable Clock, following GameListingServiceTest patterns
    - _Requirements: 2.3, 2.4, 2.5, 2.6, 4.1_

  - [x] 5.2 Write conflict and duplicate tests
    - Test: scheduling conflict — sessions overlap = rejected (400)
    - Test: scheduling conflict — gap less than 60 minutes = rejected (400)
    - Test: scheduling conflict — gap exactly 60 minutes = allowed
    - Test: scheduling conflict — reverse chronological order (existing before proposed) = rejected (400)
    - Test: no scheduling conflict = allowed
    - Test: already an ACCEPTED game_joiner = rejected (400)
    - Test: already has PENDING join_request = rejected (400)
    - Test: previously REJECTED user can submit new request
    - Test: request for a full team is allowed (no capacity rejection at submission)
    - _Requirements: 2.7, 2.8, 3.1, 3.2_

  - [x] 5.3 Write position validation tests
    - Test: positional format — valid position accepted
    - Test: positional format — missing position_id = rejected (400, "A position selection is required for this format")
    - Test: positional format — position not in format = rejected (400)
    - Test: positional format — alternate not in format = rejected (400)
    - Test: positional format — duplicate position IDs = rejected (400)
    - Test: positional format — alternate without primary = rejected (400)
    - Test: non-positional format — submitted positions stored as NULL
    - Test: positional format — anyPosition=true with null positions = accepted, both stored as NULL
    - Test: positional format — anyPosition=true with non-null positionId = rejected (400)
    - Test: positional format — anyPosition=true with non-null alternatePositionId = rejected (400)
    - Test: positional format — anyPosition=false with null positionId = rejected (400, existing behaviour)
    - Test: non-positional format — anyPosition flag ignored, positions stored as NULL
    - _Requirements: 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9_

  - [x] 5.4 Write happy path and invitation linking tests
    - Test: valid request creates PENDING join_request with correct fields
    - Test: player with PENDING invitation — invitation_id is set on join_request
    - Test: player without invitation — invitation_id is NULL
    - Test: response includes joinRequestId, gameListingId, team, status, invitationLinked
    - Test: successful request creates a notification for the listing creator (type=join_request)
    - Test: failed request (validation error) does not create a notification (transaction rollback)
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 5.1, 5.3_

- [x] 6. Frontend join request form
  - [x] 6.1 Add join request section to listing detail page
    - Add HTML section in `frontend/pages/listing-detail.html` below the roster
    - Include: team selection radios (with fill count labels), position dropdowns (conditional), submit button, error/success message area, pending status badge
    - Use semantic HTML with labels, accessible form structure
    - Style with existing CSS variables and card-based layout
    - _Requirements: 7.1, 7.2, 7.3_

  - [x] 6.2 Implement join request JavaScript logic
    - In `frontend/js/listingDetail.js` (or new file if detail page script doesn't exist):
    - On page load: determine user status (creator/accepted/pending/eligible) from detail response
    - Show/hide form sections based on status
    - Populate team fill counts from roster data
    - Populate position dropdowns if format has_positions
    - On submit: POST to `/api/game-listings/{id}/join-requests`, handle loading state, show success/error
    - Use `Api.post()` from shared `api.js`
    - _Requirements: 7.4, 7.5, 7.6, 7.7_

- [x] 7. Final checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- No database migration required — V3 already created the join_request table with all constraints
- The scheduling conflict check reuses the existing `GameListingDao.hasSchedulingConflict` method
- Position validation follows the same pattern established in `GameListingService.validatePositions`
- The `anyPosition` boolean in JoinRequestRequest allows the user to opt for "Any Position" instead of selecting a specific position for positional formats. When `anyPosition = true`, both `positionId` and `alternatePositionId` must be null and are stored as NULL. This is consistent with the Create Game Listing (A100) position handling.
- The filtered unique index `UX_join_request_one_pending` provides database-level protection against duplicate pending requests, but the service checks proactively to return a clear error message
- Unit tests use fake DAO implementations following existing GameListingServiceTest patterns
- Frontend assumes the listing detail DTO includes enough info to determine user eligibility (creator_id, roster, format details)
- The creator notification uses the existing `NotificationDao.insertBatch(Connection, List<Notification>)` within the transaction, following the same pattern as game listing creation notifications.

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "2.1", "2.2"] },
    { "id": 1, "tasks": ["1.2", "1.3", "1.4"] },
    { "id": 2, "tasks": ["2.3"] },
    { "id": 3, "tasks": ["3.1"] },
    { "id": 4, "tasks": ["3.2", "3.3"] },
    { "id": 5, "tasks": ["5.1", "5.2", "5.3", "5.4"] },
    { "id": 6, "tasks": ["6.1"] },
    { "id": 7, "tasks": ["6.2"] }
  ]
}
```
