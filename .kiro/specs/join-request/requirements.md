# Requirements Document — A300: Send Join Request

## Introduction

This spec covers use case A300 — Send Join Request. An authenticated player views a game listing and submits a request to join, selecting a team and optionally preferred positions. The listing creator must later approve or reject the request (C500, separate use case). This spec covers only the submission of the request and the immediate server-side validation and persistence.

## Glossary

- **System**: The GameOn backend application (Javalin server).
- **Player**: The authenticated user submitting the join request.
- **Listing**: A game_listing row representing an upcoming sports session.
- **Creator**: The user who created the listing (game_listing.creator_id).
- **Join_Request**: A row in the join_request table representing a player's request to join a listing.
- **Invitation**: A row in the invitation table representing a courtesy invitation from the creator to a player.
- **Format**: A sport_format row defining capacity (no_players), positions (has_positions), and duration.
- **Accepted_Joiner**: A game_joiner row with status = 'ACCEPTED'.
- **Lock_In**: The point 2 hours before a listing's start time, after which no new requests are allowed.
- **Travel_Buffer**: A 60-minute window added after a session's end_time for scheduling-conflict checks.

## Requirements

### Requirement 1: Submit Join Request

**User Story:** As a player, I want to send a join request to a game listing, so that the creator can consider me for participation.

#### Acceptance Criteria

1. WHEN an authenticated player submits a join request with a valid team and optional position preferences, THE System SHALL create a PENDING join_request row linked to the listing and player.
2. WHEN the join request is successfully created, THE System SHALL return the join_request_id, listing ID, team, position selections, status, and whether the request is invitation-linked.
3. WHEN the join request is successfully created, THE System SHALL create a notification for the listing creator indicating that the player has requested to join. The notification is part of the same transaction as the join_request insert. The requester does not receive a notification for their own submission.
4. WHEN the player has a PENDING invitation for the listing, THE System SHALL link the join_request to that invitation by setting invitation_id.

### Requirement 2: Eligibility Validation

**User Story:** As the system, I want to enforce eligibility rules before creating a join request, so that only valid requests are persisted.

#### Acceptance Criteria

1. IF the user is not authenticated, THEN THE System SHALL return a 401 Unauthorized response.
2. IF the listing does not exist, THEN THE System SHALL return a 404 Not Found response.
3. IF the player is the creator of the listing, THEN THE System SHALL return a 400 Bad Request with message "Cannot join your own listing".
4. IF the listing status is not OPEN, THEN THE System SHALL return a 400 Bad Request with message "Listing is not open for join requests".
5. IF the listing has passed lock-in (current time is within 2 hours of start time), THEN THE System SHALL return a 400 Bad Request with message "Listing has passed lock-in and is no longer accepting requests".
6. IF the player does not have the listing's sport on their profile AND does not have a PENDING invitation for the listing, THEN THE System SHALL return a 400 Bad Request with message "Selected sport is not on your profile".
7. IF the player already has a PENDING join_request for the listing, THEN THE System SHALL return a 400 Bad Request with message "You already have a pending request for this listing".
8. IF the player is already an ACCEPTED game_joiner on the listing, THEN THE System SHALL return a 400 Bad Request with message "You are already a participant in this listing".

### Requirement 3: Scheduling Conflict Validation

**User Story:** As the system, I want to prevent join requests when the player has a scheduling conflict, so that players cannot commit to overlapping sessions.

#### Acceptance Criteria

1. WHEN a join request is submitted, THE System SHALL check for scheduling conflicts using the same logic as listing creation: the listing's session window [date, end_time] plus a 60-minute travel buffer must not overlap with any existing session and buffer where the player is an ACCEPTED participant.
2. IF a scheduling conflict exists, THEN THE System SHALL return a 400 Bad Request with message "Scheduling conflict: the proposed session overlaps with an existing session and its travel buffer".

### Requirement 4: Input Validation

**User Story:** As the system, I want to validate the team and position inputs, so that only well-formed requests are accepted.

#### Acceptance Criteria

1. IF the team field is missing or not one of 'A' or 'B', THEN THE System SHALL return a 400 Bad Request with message "Team selection is required (A or B)".
2. WHILE the format has positions (has_positions = true), THE System SHALL require position_id. IF position_id is missing, THEN THE System SHALL return a 400 Bad Request with message "A position selection is required for this format".
3. WHILE the format has positions (has_positions = true), WHEN a position_id is provided, THE System SHALL verify the position belongs to the listing's format.
4. WHILE the format has positions (has_positions = true), WHEN an alternate_position_id is provided, THE System SHALL verify it belongs to the listing's format and differs from position_id.
5. WHILE the format does not have positions (has_positions = false), THE System SHALL ignore any submitted position values and store them as NULL.
6. IF a provided position_id does not belong to the listing's format, THEN THE System SHALL return a 400 Bad Request with message "Selected position does not belong to the chosen format".
7. IF a provided alternate_position_id does not belong to the listing's format, THEN THE System SHALL return a 400 Bad Request with message "Selected alternate position does not belong to the chosen format".
8. IF position_id and alternate_position_id are the same, THEN THE System SHALL return a 400 Bad Request with message "First and second position preferences must be different".
9. IF alternate_position_id is provided without position_id, THEN THE System SHALL return a 400 Bad Request with message "Alternate position requires a primary position selection".

### Requirement 5: Transaction and Data Integrity

**User Story:** As the system, I want all join request operations to be atomic, so that partial state is never persisted.

#### Acceptance Criteria

1. THE System SHALL perform all validation checks, the join_request insert, and the creator notification insert within a single database transaction.
2. IF any step fails, THEN THE System SHALL roll back the transaction and return an appropriate error response with nothing persisted.
3. THE System SHALL set the join_request format_id to match the listing's format_id (enforced by composite FK).

### Requirement 6: API Contract

**User Story:** As a frontend developer, I want a well-defined API endpoint for submitting join requests, so that I can build the UI form.

#### Acceptance Criteria

1. THE System SHALL expose the endpoint as POST /api/game-listings/{id}/join-requests.
2. WHEN a request is successful, THE System SHALL return HTTP 201 with the ApiResponse wrapper containing the join request details.
3. WHEN a request fails validation, THE System SHALL return the appropriate HTTP status code (400, 401, 403, or 404) with the ApiResponse error wrapper.
4. THE System SHALL accept the request body as JSON with fields: team (required), positionId (optional), alternatePositionId (optional).

### Requirement 7: Frontend Join Request Form

**User Story:** As a player viewing a listing detail page, I want a join request form so that I can select my team and positions and submit a request.

#### Acceptance Criteria

1. WHEN a player views a listing detail page where they are not the creator and not already a participant, THE System SHALL display a join request form.
2. WHEN displaying the join request form, THE System SHALL show team selection (A/B) with current fill counts for each team.
3. WHILE the listing's format has positions, WHEN the join form is displayed, THE System SHALL show position selection dropdowns populated with valid positions for the format.
4. WHEN the player submits the join request form, THE System SHALL display a loading state and disable the submit button.
5. WHEN the join request succeeds, THE System SHALL display a success message and hide the form.
6. WHEN the join request fails, THE System SHALL display the server error message to the player.
7. WHILE the player already has a PENDING request for the listing, THE System SHALL display a "Request Pending" status instead of the form.
