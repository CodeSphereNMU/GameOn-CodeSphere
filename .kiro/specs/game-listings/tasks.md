# Game Listings - Tasks

## Prerequisites
- Authentication spec is implemented.
- Player Profiles spec is implemented (sports on profile).
- Sport, SportFormat, Position tables exist.

## Tasks

### Task 1: Create listing tables migration
- [ ] Create `V5__create_listing_tables.sql`
- [ ] Create [Position] table
- [ ] Create [FormatPosition] junction table
- [ ] Create [GameListing] table with all columns and FKs
- [ ] Create [GameJoiner] table with composite PK and FKs
- **Traces to:** REQ-LIST-1, REQ-LIST-4

### Task 2: Seed positions data
- [ ] Create migration with positions for soccer, basketball, etc.
- [ ] Link positions to formats via [FormatPosition]
- **Traces to:** REQ-LIST-1, REQ-LIST-4

### Task 3: Create domain models
- [ ] Create `GameListing.java`
- [ ] Create `GameJoiner.java`
- [ ] Create `Position.java`
- **Traces to:** REQ-LIST-1, REQ-LIST-4

### Task 4: Create GameListingDao
- [ ] Implement create, findById, findAll (with filters), update, delete
- [ ] Implement countActiveByCreator (for one-listing rule)
- [ ] All queries parameterised; expired listings filtered by date
- **Traces to:** REQ-LIST-1, REQ-LIST-2, REQ-LIST-7, REQ-LIST-8

### Task 5: Create GameJoinerDao
- [ ] Implement addJoinRequest, findByListing, findByUser
- [ ] Implement updateStatus (accept/reject)
- [ ] Implement removeParticipant (leave)
- [ ] Implement countAcceptedByTeam (for capacity checks)
- [ ] Implement findJoinedListingsNearTime (for scheduling conflict)
- **Traces to:** REQ-LIST-4, REQ-LIST-5, REQ-LIST-6

### Task 6: Create GameListingService
- [ ] Implement create with validations (sport on profile, no existing active listing, future date)
- [ ] Implement browse with filters (exclude expired/cancelled, only user's sports)
- [ ] Implement getDetail (with roster)
- [ ] Implement update and cancel (creator only)
- **Traces to:** REQ-LIST-1, REQ-LIST-2, REQ-LIST-3, REQ-LIST-7

### Task 7: Create JoinRequestService
- [ ] Implement sendRequest with validations (sport, scheduling conflict, capacity)
- [ ] Implement acceptRequest (creator only, capacity check, add to roster)
- [ ] Implement rejectRequest (creator only, notify requester)
- [ ] Implement leaveListing (remove participant, update spots)
- **Traces to:** REQ-LIST-4, REQ-LIST-5, REQ-LIST-6

### Task 8: Create controllers
- [ ] Create `GameListingController.java` with CRUD routes
- [ ] Create `JoinRequestController.java` with request management routes
- [ ] Register both in `JavalinConfig.registerRoutes()`
- **Traces to:** REQ-LIST-1 through REQ-LIST-7

### Task 9: Create frontend pages
- [ ] Create `pages/game-listings.html` (browse page with filters)
- [ ] Create `pages/listing-detail.html` (full detail with rosters)
- [ ] Create `pages/create-listing.html` (multi-step creation form)
- [ ] Create corresponding JS files
- **Traces to:** REQ-LIST-1, REQ-LIST-2, REQ-LIST-3

### Task 10: Write tests
- [ ] Test GameListingService validations (duplicate listing, past date, wrong sport)
- [ ] Test JoinRequestService validations (scheduling conflict, full team, self-join to own listing rules)
- [ ] Test browse filtering logic
- **Traces to:** REQ-LIST-1, REQ-LIST-2, REQ-LIST-4

### Task 11: Manual verification
- [ ] Create a game listing
- [ ] Browse listings with filters
- [ ] Send a join request from another account
- [ ] Accept the request and verify roster update
- [ ] Leave a listing and verify spot opens
- [ ] Cancel a listing as creator
- [ ] Capture evidence
- **Traces to:** REQ-LIST-1 through REQ-LIST-7
