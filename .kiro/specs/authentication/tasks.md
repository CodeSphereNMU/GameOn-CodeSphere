# Authentication & Account Access - Tasks

## Prerequisites
- Project foundation compiles and starts (health endpoint works).
- SQL Server database `GameOnDB` exists locally with V1–V3 applied.
- The `users` table already exists (created in V1).

## Completed Tasks

### Task 1: User model
- [x] `User.java` in `model/` with fields: userId (long), username, password, typeOfUser
- [x] Constructor, getters, setters
- **Traces to:** REQ-AUTH-1, REQ-AUTH-3

### Task 2: UserDao
- [x] `UserDao.java` extending `BaseDao`
- [x] `findByUsername(String username)` → `Optional<User>`
- [x] `findById(long userId)` → `Optional<User>`
- [x] All queries use parameterised SQL with exact lower snake_case column names
- **Traces to:** REQ-AUTH-1, REQ-AUTH-3

### Task 3: AuthService (login)
- [x] `AuthService.java` in `service/`
- [x] `login(String username, String password)` — validates input, plain-text comparison
- [x] Returns same 401 error for unknown username and wrong password
- [x] Trims username before lookup
- [x] Throws `ApiException` for blank input (400) and invalid credentials (401)
- **Traces to:** REQ-AUTH-3

### Task 4: LoginRequest DTO
- [x] `LoginRequest.java` in `dto/` with username and password fields
- **Traces to:** REQ-AUTH-3

### Task 5: AuthController (login + me)
- [x] `AuthController.java` in `controller/`
- [x] `POST /api/auth/login` — authenticates, sets session attribute `userId`
- [x] `GET /api/auth/me` — returns current user from session or 401
- [x] Registered in `JavalinConfig.registerRoutes()`
- **Traces to:** REQ-AUTH-3, REQ-AUTH-5

### Task 6: Frontend login page
- [x] Login form on `index.html` (username, password, submit)
- [x] `login.js` calling `POST /api/auth/login` via Api helper
- [x] Error display on failure, redirect to dashboard on success
- [x] Password visibility toggle
- **Traces to:** REQ-AUTH-3

### Task 7: Dashboard session verification
- [x] `dashboard.html` with session check UI
- [x] `dashboard.js` calling `GET /api/auth/me`
- [x] Shows user info on valid session, error state when not logged in
- **Traces to:** REQ-AUTH-5

### Task 8: AuthService unit tests
- [x] `AuthServiceTest.java` with FakeUserDao (no DB dependency)
- [x] Tests: successful login, unknown username, wrong password, same error message for both
- [x] Tests: blank/null username, blank/null password (400 responses)
- [x] Tests: username trimming, null typeOfUser handling
- [x] 10 tests passing
- **Traces to:** REQ-AUTH-3

## Remaining Tasks

### Task 9: Registration endpoint
- [ ] Add `existsByUsername(String username)` to UserDao
- [ ] Add `create(String username, String password, String typeOfUser)` to UserDao
- [ ] Add `register(...)` method to AuthService with validation
- [ ] Create `RegisterRequest.java` DTO
- [ ] Add `POST /api/auth/register` to AuthController
- **Blocked by:** Group decision on validation rules (username length, password length)
- **Traces to:** REQ-AUTH-1

### Task 10: Registration frontend
- [ ] Create `frontend/pages/register.html` with registration form
- [ ] Create `frontend/js/register.js` calling `POST /api/auth/register`
- [ ] Sport selection step (timing depends on group decision: mandatory or optional)
- [ ] Fix the broken Sign Up link (currently points to missing `register.html`)
- **Blocked by:** Group decision on sport selection timing, validation rules
- **Traces to:** REQ-AUTH-1, REQ-AUTH-2

### Task 11: Logout endpoint
- [ ] Add `POST /api/auth/logout` to AuthController
- [ ] Invalidate session on logout
- [ ] Add logout button/link to dashboard or navigation
- **Traces to:** REQ-AUTH-4

### Task 12: Global authentication middleware
- [ ] Add `before` handler in JavalinConfig for route protection
- [ ] Skip confirmed public routes (per group decision)
- [ ] Check session attribute; throw 401 if missing
- [ ] Remove ad-hoc session checks from individual controllers once middleware is active
- **Blocked by:** Group decision on which routes are public
- **Traces to:** REQ-AUTH-6

### Task 13: Registration unit tests
- [ ] Test registration validation (per confirmed rules)
- [ ] Test duplicate username rejection
- [ ] Test confirm-password mismatch
- **Blocked by:** Group decision on validation rules
- **Traces to:** REQ-AUTH-1

### Task 14: Manual verification and evidence
- [ ] Register a new user via the UI
- [ ] Log in with the new user
- [ ] Verify session persists across page loads (GET /api/auth/me)
- [ ] Log out and verify protected routes return 401
- [ ] Capture evidence screenshots in `docs/evidence/`
- **Traces to:** REQ-AUTH-1 through REQ-AUTH-6

## Notes

- The `users` table exists since V1. No migration is needed for authentication.
- Do not create `V2__create_user_table.sql` or any migration to recreate the users table.
- Identifiers are `BIGINT` in the database and `long` in Java.
- Session expiry configuration is a separate decision tracked in `docs/unresolved-questions.md`.
