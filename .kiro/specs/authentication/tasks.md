# Authentication & Account Access - Tasks

## Prerequisites
- Project foundation compiles and starts (health endpoint works).
- SQL Server database `GameOnDb` exists locally.
- **Group has confirmed:** session approach, validation rules, public route list.

## Tasks

### Task 1: Create User table migration
- [ ] Create `V2__create_user_table.sql` in `backend/src/main/resources/db/migration/`
- [ ] Define [User] table with userId, userName, password, typeOfUser, createdAt
- [ ] Add UNIQUE constraint on userName
- **Blocked by:** Group decision on username length/character constraints
- **Traces to:** REQ-AUTH-1

### Task 2: Create User model
- [ ] Create `User.java` in `model/` with fields matching the table
- [ ] Include constructor, getters (no password in responses)
- **Traces to:** REQ-AUTH-1

### Task 4: Create UserDao
- [ ] Create `UserDao.java` extending `BaseDao`
- [ ] Implement `findByUsername(String username)` → `Optional<User>`
- [ ] Implement `create(String username, String password, String typeOfUser)` → `User`
- [ ] Implement `existsByUsername(String username)` → `boolean`
- [ ] All queries use parameterised SQL
- **Traces to:** REQ-AUTH-1, REQ-AUTH-3

### Task 4: Create AuthService
- [ ] Create `AuthService.java` in `service/`
- [ ] Implement `register(...)` with validation (rules per group decision)
- [ ] Implement `login(...)` with plain text password comparison
- [ ] Throw `ApiException` for validation failures and credential errors
- **Blocked by:** Group decision on validation rules (username length, password length)
- **Traces to:** REQ-AUTH-1, REQ-AUTH-3

### Task 6: Create request DTOs
- [ ] Create `RegisterRequest.java` (username, password, confirmPassword)
- [ ] Create `LoginRequest.java` (username, password)
- **Traces to:** REQ-AUTH-1, REQ-AUTH-3

### Task 7: Create AuthController
- [ ] Create `AuthController.java` in `controller/`
- [ ] Implement POST `/api/auth/register`
- [ ] Implement POST `/api/auth/login`
- [ ] Implement POST `/api/auth/logout`
- [ ] Implement GET `/api/auth/me`
- [ ] Register in `JavalinConfig.registerRoutes()`
- **Blocked by:** Group decision on whether registration auto-starts a session
- **Traces to:** REQ-AUTH-1, REQ-AUTH-3, REQ-AUTH-4, REQ-AUTH-5

### Task 8: Create authentication middleware
- [ ] Add `before` handler in JavalinConfig for route protection
- [ ] Skip public routes (per group decision)
- [ ] Check session attribute; throw 401 if missing
- **Blocked by:** Group decision on which routes are public
- **Traces to:** REQ-AUTH-6

### Task 9: Create frontend login page
- [ ] Create `frontend/pages/login.html` with username/password form
- [ ] Create `frontend/js/login.js` calling POST `/api/auth/login`
- [ ] Handle success (redirect) and error (show message)
- **Traces to:** REQ-AUTH-3

### Task 10: Create frontend registration page
- [ ] Create `frontend/pages/register.html` with registration form
- [ ] Create `frontend/js/register.js` calling POST `/api/auth/register`
- [ ] Sport selection step (depends on group decision: mandatory or optional)
- **Blocked by:** Group decision on sport selection timing
- **Traces to:** REQ-AUTH-1, REQ-AUTH-2

### Task 11: Write unit tests
- [ ] Test AuthService validation (per confirmed rules)
- [ ] Test AuthService login (correct password, wrong password)
- [ ] Test UserDao queries (requires DB or is deferred to integration tests)
- **Traces to:** REQ-AUTH-1, REQ-AUTH-3

### Task 12: Manual verification
- [ ] Register a new user via the UI
- [ ] Log in with the new user
- [ ] Verify session persists across page loads (GET /api/auth/me)
- [ ] Log out and verify protected routes return 401
- [ ] Capture evidence screenshots in `docs/evidence/`
- **Traces to:** REQ-AUTH-1 through REQ-AUTH-6
