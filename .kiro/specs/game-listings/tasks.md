# Game Listings - Tasks (A100: Create Game Listing)

## Prerequisites
- Authentication is implemented (login, session).
- Sport, SportFormat, Position, FormatPosition tables exist with seed data (V1 + V2).
- V3 migration created, reviewed, and successfully applied to GameOnDB.

## Current Database State
- V1 and V2 have been applied (schema + seed data).
- V3 has been applied successfully. The code targets the post-V3 schema.

## Tasks

### Task 1: V3 Migration
- [x] Add duration_minutes to sport_format with confirmed values for all 15 formats
- [x] Add status column to game_listing with CHECK constraint
- [x] Add end_time column to game_listing
- [x] Drop is_completed from game_listing
- [x] Update game_joiner: uppercase status, alternate_position_id, join_request_id, CHECK constraints
- [x] Create invitation table with uniqueness constraint
- [x] Create join_request table with filtered unique index (foundation only)
- [x] Update match_result: drop winners, add score constraints
- [x] Add game_listing_id and created_at to notification
- [x] Add supporting indexes for scheduling queries
- **Traces to:** REQ-LIST-1

### Task 2: Update domain models
- [x] SportFormat — add durationMinutes
- [x] GameListing — replace isCompleted with status + endTime
- [x] GameJoiner — alternatePositionId (Long), joinRequestId (Long)
- [x] Invitation — new model
- [x] Notification — add gameListingId
- **Traces to:** REQ-LIST-1

### Task 3: Update DAOs
- [x] SportFormatDao — read duration_minutes
- [x] GameListingDao — new scheduling conflict (bidirectional overlap), insert with status+end_time
- [x] GameJoinerDao — write alternate_position_id, join_request_id
- [x] InvitationDao — new, insertBatch
- [x] NotificationDao — write game_listing_id
- **Traces to:** REQ-LIST-1

### Task 4: Update GameListingService
- [x] Calculate end_time from format.durationMinutes
- [x] Team validation (A or B required)
- [x] New scheduling conflict with proposedStart + proposedEnd
- [x] Remove capacity-based invitation limit
- [x] Insert PENDING invitation records
- [x] Notifications include gameListingId
- [x] Status = 'OPEN', joiner status = 'ACCEPTED'
- **Traces to:** REQ-LIST-1

### Task 5: Update DTOs and controllers
- [x] CreateListingRequest — add team field
- [x] CreateListingResponse — add endTime, sessionWindow, team
- [x] FormatDto — add durationMinutes
- [x] SportController — pass durationMinutes
- [x] JavalinConfig — wire InvitationDao
- **Traces to:** REQ-LIST-1

### Task 6: Update frontend
- [x] Team A/B selection in Step 2
- [x] Session window display on confirmation
- [x] Remove invitation capacity cap
- [x] Pass team in payload
- **Traces to:** REQ-LIST-1

### Task 7: Update tests
- [x] Rewrite GameListingServiceTest for new rules
- [x] Test end-time calculation (60 min and 120 min)
- [x] Test team selection validation
- [x] Test scheduling conflict handling (service rejects/allows based on DAO response)
- [x] Test unlimited invitations
- [x] Test invitation record insertion
- [x] Test position validation (all scenarios preserved)
- [x] Test rollback on invitation/notification failure
- [x] Test creator joiner receives correct team and position data
- **Note:** Scheduling conflict tests use a boolean fake DAO. They verify service behaviour only — not the SQL overlap logic, accepted-joiner query, status filtering, or bidirectional calculation. Those require database integration or manual verification.
- **Traces to:** REQ-LIST-1

### Task 8: Update documentation
- [x] requirements.md — confirmed rules and current implemented placeholders documented
- [x] design.md — V3 schema, API, frontend flow
- [x] tasks.md — this file
- [x] product.md — remove one-active-listing rule, update scheduling rule
- [x] unresolved-questions.md — mark resolved questions
- **Traces to:** REQ-LIST-1

### Task 9: Verify
- [x] Run `cd backend && .\mvnw.cmd clean test` — passed (manual)
- [x] Review V3 migration
- [x] Apply V3 to GameOnDB after review approval
- [x] Manual end-to-end test after migration
- [ ] Evidence capture
- **Traces to:** REQ-LIST-1
