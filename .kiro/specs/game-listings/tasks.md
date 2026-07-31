# Game Listings - Tasks (A100: Create Game Listing)

## Prerequisites
- Authentication is implemented (login, session).
- Sport, SportFormat, Position, FormatPosition tables exist with seed data (V1 + V2 migrations).
- user_sport_profile table exists.
- follow table exists.
- notification table exists.

## Tasks

### Task 1: Create domain models
- [x] GameListing.java
- [x] GameJoiner.java
- [x] Sport.java
- [x] SportFormat.java
- [x] Position.java
- **Traces to:** REQ-LIST-1

### Task 2: Create DAOs
- [x] SportDao — findSportsByUserId(long userId)
- [x] SportFormatDao — findFormatsBySportId(long sportId)
- [x] PositionDao — findPositionsByFormatId(long formatId)
- [x] GameListingDao — insert(Connection, GameListing), hasSchedulingConflict(Connection, long userId, LocalDateTime proposedDateTime)
- [x] GameJoinerDao — insertCreator(Connection, GameJoiner)
- [x] FollowDao — findMutualFollowers(long userId)
- [x] NotificationDao — insertBatch(Connection, List<Notification>)
- **Traces to:** REQ-LIST-1

### Task 3: Create DTOs
- [x] CreateListingRequest
- [x] SportDto, FormatDto, PositionDto, FriendDto
- **Traces to:** REQ-LIST-1

### Task 4: Create GameListingService
- [x] Full validation (all rules from requirements)
- [x] Transactional creation (listing + creator joiner + notifications)
- **Traces to:** REQ-LIST-1

### Task 5: Create controllers and register routes
- [x] UserSportController — GET /api/users/me/sports
- [x] SportController — GET /api/sports/{sportId}/formats, GET /api/formats/{formatId}/positions
- [x] FriendController — GET /api/users/me/friends
- [x] GameListingController — POST /api/game-listings
- [x] Register all in JavalinConfig.registerRoutes()
- **Traces to:** REQ-LIST-1

### Task 6: Create frontend
- [x] pages/create-listing.html — multi-step form matching Canva designs
- [x] js/createListing.js — step navigation, API integration, validation
- [x] css/main.css additions — styles for create-listing card and steps
- **Traces to:** REQ-LIST-1

### Task 7: Write tests
- [x] GameListingServiceTest — all validation scenarios
- **Traces to:** REQ-LIST-1

### Task 8: Verify
- [ ] Run `cd backend && .\mvnw.cmd clean test`
- [ ] Manual end-to-end test
- [ ] Capture evidence
- **Traces to:** REQ-LIST-1
