# Player Profiles & Sports - Tasks

## Prerequisites
- Authentication registration is implemented (user can register and log in).
- V1–V3 applied. All required tables exist (`users`, `sport`, `sport_format`, `user_sport_profile`, `follow`).
- Seed data applied (5 sports, 15 formats, 25 positions).

## Existing Foundation (no work needed)

The following were built for A100 and are reusable. Do not recreate them.

- [x] `sport`, `sport_format`, `position`, `format_position`, `user_sport_profile`, `follow` tables (V1)
- [x] Seed data for 5 sports, 15 formats, 25 positions (V2)
- [x] `Sport`, `SportFormat`, `Position` models
- [x] `SportDao`, `SportFormatDao`, `PositionDao`, `FollowDao`
- [x] `UserSportController` (GET /api/users/me/sports)
- [x] `SportController` (formats, positions)
- [x] `FriendController` (mutual followers)

## Remaining Tasks

### Task 1: Create UserSportProfileDao
- [ ] `findByUserId(long userId)` — returns user's sport profiles
- [ ] `add(long userId, long sportId, String skillLevel)` — inserts row
- [ ] `remove(long userId, long sportId)` — deletes row
- [ ] `updateSkillLevel(long userId, long sportId, String skillLevel)`
- [ ] `existsByUserAndSport(long userId, long sportId)` — check for duplicates
- [ ] Use parameterised SQL with exact lower snake_case column names
- **Traces to:** REQ-PROF-3, REQ-PROF-4, REQ-PROF-5

### Task 2: Create SportService
- [ ] `addSportToProfile(long userId, long sportId, String skillLevel)` — validates sport exists, not already on profile, valid skill level
- [ ] `removeSportFromProfile(long userId, long sportId)` — removes sport from profile; active-listing blocking behaviour depends on group decision (see unresolved questions)
- [ ] `updateSkillLevel(long userId, long sportId, String skillLevel)` — validates sport is on profile, valid skill level
- [ ] `getAvailableSports(long userId)` — sports not yet on user's profile
- **Traces to:** REQ-PROF-3, REQ-PROF-4, REQ-PROF-5

### Task 3: Create ProfileService
- [ ] `getProfile(long userId)` — assembles profile data (user info, sports, stats, follower counts)
- [ ] `updateUsername(long userId, String newUsername)` — validation (rules per group decision), duplicate check
- [ ] `followUser(long followerId, long followedId)` — self-follow prevention, duplicate check
- [ ] `unfollowUser(long followerId, long followedId)`
- [ ] `getFollowerCount(long userId)`, `getFollowingCount(long userId)`
- **Traces to:** REQ-PROF-1, REQ-PROF-2, REQ-PROF-6, REQ-PROF-7

### Task 4: Create ProfileController
- [ ] `GET /api/profiles/{userId}` — view profile
- [ ] `PUT /api/profiles/me` — update username
- [ ] `POST /api/profiles/me/sports` — add sport
- [ ] `DELETE /api/profiles/me/sports/{sportId}` — remove sport
- [ ] `PUT /api/profiles/me/sports/{sportId}` — update skill level
- [ ] `POST /api/profiles/{userId}/follow` — follow
- [ ] `DELETE /api/profiles/{userId}/follow` — unfollow
- [ ] Register in `JavalinConfig.registerRoutes()`
- **Traces to:** REQ-PROF-1 through REQ-PROF-7

### Task 5: Create frontend pages
- [ ] `pages/profile.html` — displays user profile
- [ ] `pages/add-sport.html` — sport selection (also usable during registration)
- [ ] `js/profile.js` — profile page logic
- [ ] `js/addSport.js` — add sport page logic
- [ ] Reference relevant Canva designs before implementation
- **Traces to:** REQ-PROF-1, REQ-PROF-3, REQ-PROF-6

### Task 6: Write tests
- [ ] SportService: add duplicate sport, valid/invalid skill level, removal with active listing
- [ ] ProfileService: update username validation, self-follow prevention, duplicate follow
- **Traces to:** REQ-PROF-2 through REQ-PROF-7

### Task 7: Manual verification and evidence
- [ ] Add a sport to profile
- [ ] View own profile with sports and stats
- [ ] View another user's profile
- [ ] Follow and unfollow another user
- [ ] Remove a sport (when allowed)
- [ ] Capture evidence screenshots in `docs/evidence/`
- **Traces to:** REQ-PROF-1 through REQ-PROF-7

## Notes

- Do not create migrations for `sport`, `sport_format`, `user_sport_profile`, or `follow` tables. They already exist.
- Do not create a `V3__create_sport_tables.sql` or `V4__seed_sports.sql`. These are obsolete names from an early draft that was never applied.
- The `sport` table has only `sport_id` and `sport_name`. There is no `noPlayers` column on `sport`.
- All identifiers are `BIGINT` in the database and `long` in Java.
- Username validation rules depend on a group decision shared with authentication.
- The last-sport rule (whether removal of the final sport is allowed) is unresolved.
- The active-listing blocking rule (whether removal is blocked while the user has active involvement in listings for that sport) is unresolved.
