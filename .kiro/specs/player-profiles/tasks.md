# Player Profiles & Sports - Tasks

## Prerequisites
- Authentication spec is implemented (user can register and log in).
- [User] table exists.

## Tasks

### Task 1: Create Sport and UserSportProfile tables
- [ ] Create migration `V3__create_sport_tables.sql`
- [ ] Create [Sport] table (sportId, sportName, noPlayers)
- [ ] Create [SportFormat] table (formatId, sportId FK, formatName, noPlayers, hasPositions)
- [ ] Create [UserSportProfile] table (userId FK, sportId FK, skillLevel, wins, losses) with composite PK
- [ ] Create [Follow] table (followerUserId FK, followedUserId FK, createdAt) with composite PK
- **Traces to:** REQ-PROF-1, REQ-PROF-3, REQ-PROF-7

### Task 2: Seed sports data
- [ ] Create migration `V4__seed_sports.sql`
- [ ] Insert common sports: Soccer, Basketball, Cricket, Tennis, Rugby, Hockey, Volleyball, Badminton
- [ ] Insert at least 2 formats per sport (e.g., 5-a-side, 11-a-side for soccer)
- **Traces to:** REQ-PROF-3

### Task 3: Create domain models
- [ ] Create `Sport.java` in model/
- [ ] Create `SportFormat.java` in model/
- [ ] Create `UserSportProfile.java` in model/
- **Traces to:** REQ-PROF-1, REQ-PROF-3

### Task 4: Create DAOs
- [ ] Create `SportDao.java` (findAll, findById)
- [ ] Create `UserSportProfileDao.java` (findByUserId, add, remove, updateSkillLevel)
- [ ] Create `FollowDao.java` (follow, unfollow, isFollowing, countFollowers, countFollowing)
- **Traces to:** REQ-PROF-1 through REQ-PROF-7

### Task 5: Create ProfileService
- [ ] Implement `getProfile(int userId)` → profile data with sports and stats
- [ ] Implement `updateUsername(int userId, String newUsername)` with validation
- [ ] Implement `followUser(int followerId, int followedId)` with self-follow check
- [ ] Implement `unfollowUser(int followerId, int followedId)`
- **Traces to:** REQ-PROF-1, REQ-PROF-2, REQ-PROF-6, REQ-PROF-7

### Task 6: Create SportService
- [ ] Implement `getAllSports()` → list of available sports
- [ ] Implement `addSportToProfile(int userId, int sportId, String skillLevel)` with validation
- [ ] Implement `removeSportFromProfile(int userId, int sportId)` with last-sport check
- [ ] Implement `updateSkillLevel(int userId, int sportId, String skillLevel)`
- **Traces to:** REQ-PROF-3, REQ-PROF-4, REQ-PROF-5

### Task 7: Create controllers
- [ ] Create `ProfileController.java` with routes for profile viewing, updating, follow/unfollow
- [ ] Create `SportController.java` with routes for sport catalog and user sport management
- [ ] Register both in `JavalinConfig.registerRoutes()`
- **Traces to:** REQ-PROF-1 through REQ-PROF-7

### Task 8: Create frontend pages
- [ ] Create `pages/profile.html` displaying user profile information
- [ ] Create `pages/add-sport.html` for sport selection (also used during registration)
- [ ] Create `js/profile.js` and `js/addSport.js`
- **Traces to:** REQ-PROF-1, REQ-PROF-3, REQ-PROF-6

### Task 9: Write tests
- [ ] Test ProfileService (update username validation, self-follow prevention)
- [ ] Test SportService (add duplicate sport, remove last sport, invalid skill level)
- **Traces to:** REQ-PROF-2 through REQ-PROF-5, REQ-PROF-7

### Task 10: Manual verification
- [ ] Add a sport to profile after registration
- [ ] View own profile with sport + stats displayed
- [ ] View another user's profile
- [ ] Follow and unfollow another user
- [ ] Capture evidence
- **Traces to:** REQ-PROF-1 through REQ-PROF-7
