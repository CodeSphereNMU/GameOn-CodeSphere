# Implementation Plan: A200 Browse Listings

## Overview

Implement the browse listings feature allowing authenticated players to view paginated, filtered OPEN public game listings for sports on their profile, plus a detail view with full roster. The detail endpoint enforces access control: the listing creator can always access their own listing; public listings require the sport on the user's profile; private listings require an invitation record. Extends existing `GameListingController`, `GameListingDao`, `GameJoinerDao`, and `InvitationDao` with new query methods. Adds a new `BrowseListingService` and supporting DTOs. Creates a new frontend page with card-based UI.

## Tasks

- [x] 1. Create DTOs and value objects
  - [x] 1.1 Create `BrowseFilter`, `BrowseListingDto`, `ListingDetailDto`, `RosterEntryDto`, and `PaginatedResponse<T>` classes
    - `BrowseFilter`: page (default 1), size (default 20), sportId (optional), skillLevel (optional), date (optional, single LocalDate), hideFull (default false)
    - `BrowseListingDto`: gameListingId, sportName, formatName, skillLevel, date, sessionWindow, location, spotsFilled, totalSpots, creatorUsername
    - `ListingDetailDto`: extends card fields with hasPositions, isPrivate, teamA, teamB lists of RosterEntryDto
    - `RosterEntryDto`: username, positionName (nullable)
    - `PaginatedResponse<T>`: items, page, size, totalItems, totalPages
    - _Requirements: 1.6, 1.7, 4.1–4.7, 5.1, 5.4_

- [x] 2. Extend DAO layer with browse and roster queries
  - [x] 2.1 Add `findBrowseListings(List<Long> userSportIds, BrowseFilter filter)` and `countBrowseListings(List<Long> userSportIds, BrowseFilter filter)` to `GameListingDao`
    - Dynamic SQL building for optional filters (sportId, skillLevel, date, hideFull)
    - Base WHERE: status='OPEN', is_private=0, date > NOW, sport_id IN user's sport IDs
    - JOIN sport_format, sport, users for denormalised card data
    - Subquery for spots_filled count
    - When hideFull=true: add HAVING spots_filled < no_players (or equivalent WHERE subquery)
    - Single date filter: CAST(gl.[date] AS DATE) = ?
    - ORDER BY date ASC, OFFSET/FETCH for pagination
    - Use parameterised queries with dynamic parameter list
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.8, 2.1, 2.2, 2.3, 3.1, 3.2, 3.3_

  - [x] 2.2 Add `findRosterByListingId(long gameListingId)` to `GameJoinerDao`
    - JOIN users for username, LEFT JOIN position for position_name
    - Filter by status='ACCEPTED'
    - Order by team, username
    - _Requirements: 5.1, 5.2_

  - [x] 2.3 Add `countAcceptedByListingId(long gameListingId)` to `GameJoinerDao`
    - Count ACCEPTED joiners for a single listing
    - _Requirements: 4.6_

  - [x] 2.4 Add `hasInvitation(long gameListingId, long userId)` to `InvitationDao`
    - Returns boolean: SELECT 1 FROM invitation WHERE game_listing_id = ? AND invitee_id = ?
    - Used by detail endpoint for private listing access check
    - _Requirements: 6.3, 6.4_

- [x] 3. Implement BrowseListingService
  - [x] 3.1 Create `BrowseListingService` with `browseListings(long userId, BrowseFilter filter)` method
    - Fetch user's sport IDs via `SportDao.findSportsByUserId()`
    - If user has no sports, return empty paginated response
    - If sportId filter provided but not in user's sport list, return empty response
    - Call DAO browse and count methods
    - Map results to `BrowseListingDto` list (creator username comes from JOIN in the query)
    - Calculate totalPages = ceil(totalItems / size)
    - Return `PaginatedResponse<BrowseListingDto>`
    - _Requirements: 1.1–1.8, 2.1–2.5, 3.1–3.3_

  - [x] 3.2 Add `getListingDetail(long userId, long listingId)` method to `BrowseListingService`
    - Fetch listing by ID (404 if not found)
    - If requesting user is the creator (game_listing.creator_id = userId): allow access, skip further checks
    - If listing is public: verify listing's sport is on user's profile (403 if not)
    - If listing is private: verify invitation record exists via InvitationDao.hasInvitation (403 if not)
    - Fetch roster via GameJoinerDao.findRosterByListingId
    - Fetch format info for hasPositions and capacity
    - Build and return `ListingDetailDto` with teamA/teamB roster arrays
    - _Requirements: 5.1–5.4, 6.1–6.7_

- [x] 4. Extend GameListingController with browse and detail endpoints
  - [x] 4.1 Add `GET /api/game-listings` handler to `GameListingController`
    - Parse query params: page, size, sportId, skillLevel, date, hideFull
    - Validate session (401 if missing)
    - Validate pagination params (400 if invalid)
    - Parse date filter as single LocalDate (400 if malformed)
    - Parse hideFull as boolean (default false)
    - Call `BrowseListingService.browseListings()`
    - Return `ApiResponse.success(paginatedResponse)`
    - _Requirements: 7.1, 7.3, 8.1, 8.2, 8.3, 8.5, 8.6_

  - [x] 4.2 Add `GET /api/game-listings/{id}` handler to `GameListingController`
    - Parse path param listingId
    - Validate session (401 if missing)
    - Call `BrowseListingService.getListingDetail()`
    - Return `ApiResponse.success(detailDto)`
    - Handle 404 and 403 from service
    - _Requirements: 7.2, 8.4_

  - [x] 4.3 Wire `BrowseListingService` into `JavalinConfig` and inject into `GameListingController`
    - Instantiate BrowseListingService with required DAOs (GameListingDao, GameJoinerDao, InvitationDao, SportDao)
    - Pass to GameListingController constructor (or add setter)
    - _Requirements: 8.1_

- [x] 5. Checkpoint — Backend complete
  - Ensure all backend code compiles, ask the user if questions arise.

- [ ] 6. Create frontend browse listings page
  - [x] 6.1 Create `frontend/pages/browse-listings.html` with card-based layout
    - Shared app header with navigation back link
    - Filter controls: sport dropdown, skill level dropdown, single date input
    - "Show full listings" toggle (on by default)
    - Listing cards grid/list area
    - Pagination controls (previous/next, page indicator)
    - Empty state message when no results
    - Loading state indicator
    - Semantic HTML, WCAG AA accessible, kebab-case classes
    - _Requirements: 2.1–2.3, 3.1–3.3, 4.1–4.7_

  - [ ] 6.2 Create `frontend/js/browseListings.js` page script
    - On DOMContentLoaded: fetch user sports for filter dropdown, then load listings
    - Build query string from active filters (sportId, skillLevel, date, hideFull)
    - "Show full listings" toggle: when off, set hideFull=true in query
    - Call `Api.get('/api/game-listings?...')` and render listing cards
    - Handle pagination (next/previous buttons update page param)
    - Handle loading and error states
    - Handle empty results state
    - Card click navigates to detail view
    - _Requirements: 1.6, 2.1–2.5, 3.1–3.3, 4.1–4.7_

  - [x] 6.3 Create listing detail view (`frontend/pages/listing-detail.html` + `frontend/js/listingDetail.js`)
    - Display all card fields plus roster
    - Two-column Team A / Team B roster layout
    - Show position names for positional formats
    - Show username for each accepted participant
    - "Request to Join" button (disabled, labelled "Coming soon")
    - _Requirements: 5.1–5.4_

  - [x] 6.4 Add browse-listings styles to `frontend/css/main.css`
    - Listing card styles (`.listing-card`, `.listing-card__sport`, `.listing-card__capacity`, etc.)
    - Filter bar styles
    - "Show full listings" toggle styles
    - Pagination controls styles
    - Roster table/grid styles for detail view
    - Disabled button styles for "Request to Join"
    - Responsive adjustments
    - Use existing CSS variables (--color-primary, --radius, --shadow-card, etc.)
    - _Requirements: 4.1–4.7, 5.1–5.3_

- [x] 7. Write unit tests (JUnit 5)
  - [x] 7.1 Write unit tests for `BrowseListingService`
    - Test: empty sport profile returns empty results
    - Test: sportId filter not on profile returns empty results
    - Test: detail for non-existent listing returns 404
    - Test: detail for public listing with sport not on profile returns 403
    - Test: detail for private listing without invitation returns 403
    - Test: detail for private listing with invitation succeeds
    - Test: creator can access own public listing regardless of sport profile
    - Test: creator can access own private listing without invitation record
    - Test: hideFull=true excludes full listings
    - Test: hideFull=false (default) includes full listings
    - Test: pagination defaults (page=1, size=20)
    - Test: single date filter returns only listings on that date
    - _Requirements: 1.1, 2.5, 3.2, 3.3, 6.1–6.7, 7.1_

  - [x] 7.2 Write unit tests for `GameListingController` browse/detail handlers
    - Test: missing session returns 401
    - Test: invalid page/size returns 400
    - Test: invalid date format returns 400
    - Test: valid request returns 200 with ApiResponse wrapper
    - _Requirements: 7.1, 7.2, 8.5, 8.6_

- [x] 8. Final checkpoint
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Each task references specific requirements for traceability.
- Checkpoints ensure incremental validation.
- No new Flyway migration required — all queries target existing V3 schema.
- The "Request to Join" button is UI-only in disabled state (A300 provides functionality).
- The browse endpoint reuses the existing `/api/game-listings` path with GET method (POST already handles creation).
- The `hideFull` parameter maps to the frontend "Show full listings" toggle (toggle off → hideFull=true).
- The date filter accepts a single ISO date (YYYY-MM-DD), not a date range.
- Creator username is obtained via JOIN in the browse query — no separate UserDao method needed.
- Private listing access is enforced by checking for any invitation record (regardless of invitation status).

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["2.1", "2.2", "2.3", "2.4"] },
    { "id": 2, "tasks": ["3.1", "3.2"] },
    { "id": 3, "tasks": ["4.1", "4.2", "4.3"] },
    { "id": 4, "tasks": ["6.1", "6.4"] },
    { "id": 5, "tasks": ["6.2", "6.3"] },
    { "id": 6, "tasks": ["7.1", "7.2"] }
  ]
}
```
