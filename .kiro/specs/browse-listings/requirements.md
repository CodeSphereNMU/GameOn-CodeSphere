# Requirements Document — A200: Browse Listings

## Introduction

Browse Listings enables authenticated players to discover and view upcoming OPEN game listings. The feature restricts results to public listings for sports on the user's profile, supports pagination, filtering (sport, skill level, single date), a "show full listings" toggle, and default date-ascending sort. A detail view displays the full roster grouped by team with position information. Private listings are only accessible via invitation. A "Request to Join" button is shown in disabled state (functionality deferred to A300).

## Glossary

- **System**: The GameOn backend application (Javalin API server and associated database).
- **Browse_Service**: The service-layer component responsible for querying, filtering, and paginating game listings.
- **Listing_Card**: A frontend UI component that summarises a single game listing in browse results.
- **Detail_View**: A frontend page showing full listing information including the team roster.
- **User_Sport_Profile**: The set of sports a player has registered on their profile (table `user_sport_profile`).
- **OPEN_Listing**: A game listing with `status = 'OPEN'` that has not yet reached its two-hour lock-in window and has not been confirmed or cancelled.
- **Full_Listing**: An OPEN listing where the count of ACCEPTED game joiners equals `sport_format.no_players`.
- **Player**: An authenticated user (registered user who successfully logs in).
- **Invitation_Record**: A row in the `invitation` table linking a game listing to an invited user.

## Requirements

### Requirement 1: Retrieve Paginated Listings

**User Story:** As a player, I want to browse a paginated list of open game listings for sports on my profile, so that I can find games to join.

#### Acceptance Criteria

1. WHEN a player requests the browse listings endpoint, THE Browse_Service SHALL return only OPEN_Listing records whose sport is on the player's User_Sport_Profile.
2. WHEN a player requests the browse listings endpoint, THE Browse_Service SHALL exclude listings where `is_private = true`.
3. WHEN a player requests the browse listings endpoint, THE Browse_Service SHALL exclude listings with status other than OPEN.
4. WHEN a player requests the browse listings endpoint, THE Browse_Service SHALL exclude listings whose session start date is in the past.
5. THE Browse_Service SHALL return results sorted by session start date ascending (soonest first) by default.
6. THE Browse_Service SHALL paginate results using `page` and `size` query parameters, defaulting to page 1 and size 20.
7. THE Browse_Service SHALL return pagination metadata including `items`, `page`, `size`, `totalItems`, and `totalPages`.
8. THE Browse_Service SHALL include Full_Listing records in browse results by default.

### Requirement 2: Filter Listings

**User Story:** As a player, I want to filter listings by sport, skill level, and date, so that I can narrow results to games that suit me.

#### Acceptance Criteria

1. WHEN a `sportId` query parameter is provided, THE Browse_Service SHALL return only listings for that sport.
2. WHEN a `skillLevel` query parameter is provided, THE Browse_Service SHALL return only listings matching that skill level.
3. WHEN a `date` query parameter is provided (single ISO date), THE Browse_Service SHALL return only listings whose session start date falls on that specific date.
4. WHEN multiple filter parameters are provided, THE Browse_Service SHALL apply all filters conjunctively (AND logic).
5. IF a provided `sportId` is not on the player's profile, THEN THE Browse_Service SHALL return an empty result set rather than an error.

### Requirement 3: Show Full Listings Toggle

**User Story:** As a player, I want to hide full listings from browse results, so that I only see games with available spots.

#### Acceptance Criteria

1. THE Browse_Service SHALL accept a `hideFull` query parameter (boolean, default false).
2. WHEN `hideFull` is true, THE Browse_Service SHALL exclude listings where the count of ACCEPTED game joiners equals `sport_format.no_players`.
3. WHEN `hideFull` is false or not provided, THE Browse_Service SHALL include both full and non-full OPEN listings.

### Requirement 4: Listing Card Display

**User Story:** As a player, I want each listing card to show key details at a glance, so that I can quickly assess whether a game interests me.

#### Acceptance Criteria

1. THE Listing_Card SHALL display the sport name derived from the listing's format's sport.
2. THE Listing_Card SHALL display the format name from `sport_format.format_name`.
3. THE Listing_Card SHALL display the skill level of the listing.
4. THE Listing_Card SHALL display the date and session window (start time to end time formatted as HH:mm–HH:mm).
5. THE Listing_Card SHALL display the location of the listing.
6. THE Listing_Card SHALL display capacity as "X / Y" where X is the count of ACCEPTED game joiners and Y is `sport_format.no_players`.
7. THE Listing_Card SHALL display the creator's username.

### Requirement 5: Listing Detail View

**User Story:** As a player, I want to view full details of a listing including the roster, so that I can decide whether to request to join.

#### Acceptance Criteria

1. WHEN a player views a listing detail, THE Detail_View SHALL display the roster of accepted participants divided by team (Team A and Team B).
2. WHEN a listing's format has positions (`has_positions = true`), THE Detail_View SHALL display each participant's position name next to their username.
3. THE Detail_View SHALL display a "Request to Join" button in a disabled state with a label indicating "Coming soon".
4. THE Detail_View SHALL display all Listing_Card fields plus the full roster information.

### Requirement 6: Listing Detail Access Control

**User Story:** As the system, I want to enforce access rules on the listing detail endpoint, so that private listings remain protected and users only view relevant listings.

#### Acceptance Criteria

1. IF a player requests a listing that does not exist, THEN THE System SHALL return HTTP 404.
2. WHEN the requesting player is the listing creator (`game_listing.creator_id = userId`), THE System SHALL grant access regardless of whether the listing is public or private.
3. WHEN a player requests a public listing detail and the listing's sport is on the player's User_Sport_Profile, THE System SHALL grant access.
4. IF a player requests a public listing whose sport is not on their User_Sport_Profile, THEN THE System SHALL return HTTP 403.
5. WHEN a player requests a private listing detail and a row exists in the `invitation` table for (`game_listing_id`, `invitee_id` = userId), THE System SHALL grant access.
6. IF a player requests a private listing and the player is not the creator and no Invitation_Record exists for that player, THEN THE System SHALL return HTTP 403.
7. THE System SHALL enforce these access checks regardless of how the listing ID is obtained (browsing or direct URL entry).

### Requirement 7: Authentication

**User Story:** As the system, I want to ensure only authenticated players can browse listings, so that the platform remains secure.

#### Acceptance Criteria

1. IF a request to the browse listings endpoint lacks a valid session, THEN THE System SHALL return HTTP 401 with an error message.
2. IF a request to the listing detail endpoint lacks a valid session, THEN THE System SHALL return HTTP 401 with an error message.
3. WHEN a valid session is present, THE System SHALL use the session's userId to determine the player's sport profile for filtering.

### Requirement 8: Browse Listings API

**User Story:** As a frontend developer, I want well-structured API endpoints for browsing listings, so that I can build the UI against a predictable contract.

#### Acceptance Criteria

1. THE System SHALL expose a GET endpoint at `/api/game-listings` for browsing listings.
2. THE System SHALL accept query parameters: `page`, `size`, `sportId`, `skillLevel`, `date`, `hideFull`.
3. THE System SHALL respond with the standard `ApiResponse` wrapper containing paginated listing data.
4. THE System SHALL expose a GET endpoint at `/api/game-listings/{id}` returning full listing details including roster.
5. IF invalid pagination parameters are provided (non-numeric or less than 1), THEN THE System SHALL return HTTP 400 with an error message.
6. IF an invalid date format is provided in the `date` parameter, THEN THE System SHALL return HTTP 400 with an error message.
