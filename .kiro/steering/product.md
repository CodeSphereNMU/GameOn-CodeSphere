# GameOn - Product Context

## What is GameOn?

GameOn is a web-based sports organisation and community platform built by CodeSphere (university group project, 2026). Its core purpose is helping sports players create structured game listings, find suitable games and opponents, organise teams and positions, manage participants, and record results.

## Core User Journey

1. Register and log in.
2. Create a player profile with sports and skill levels.
3. Create a game listing specifying sport, format, skill level, date/time, location, teams, positions, and capacity.
4. Other eligible players browse/filter listings and view details.
5. A player selects a team/position and sends a join request.
6. The listing creator accepts or rejects the request.
7. Accepted players appear in the roster.
8. The listing progresses through its lifecycle (active, confirmed, completed, expired, cancelled).
9. The game takes place outside the system.
10. The creator records the result, contributing to match history, stats, and leaderboards.

## Secondary Features (Community Layer)

- Player profiles with stats display
- Following other players
- Posts, comments, and likes
- Notifications
- Reporting users/posts/comments
- Moderation
- Leaderboards

The game-organisation features are the core product. Social features support it but are lower priority.

## Roles

| Role | Description |
|------|-------------|
| Visitor | Can view landing page, register, log in |
| Player | Normal registered user |
| Listing Creator | The player who created a specific listing (contextual role) |
| Participant | A player accepted into a specific listing |
| Moderator | Handles reported users/content |
| System/Time | Automated processes (reminders, expiry, confirmation) |

## Key Business Rules (Confirmed)

- A user can have only one active game listing at a time.
- A user can join multiple listings (subject to scheduling constraints).
- A user cannot join two listings whose scheduled times are less than 3 hours apart.
- A user can only create a listing for a sport they have on their profile.
- A user can only join a listing for a sport they have on their profile.
- Only the listing creator can accept/reject join requests.
- Only the listing creator can record and update match results.
- Only a moderator can remove users, posts, or comments.
- All participants in a game must have a GameOn account.
- One match result per game listing.

## Constraints

- Venue availability is not synced with the system.
- Not all sports grounds can be verified.
- No payment gateway integration.
- The system does not handle actual gameplay, only organisation and results.
