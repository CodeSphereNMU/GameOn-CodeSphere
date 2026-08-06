# Authentication & Account Access - Design

## Architecture

```
Frontend (index.html login form, dashboard.html session check)
  → fetch() POST /api/auth/login, GET /api/auth/me
    → AuthController
      → AuthService (validates input, compares passwords)
        → UserDao (queries dbo.users table)
```

## Database Table (Existing — V1)

### dbo.users
| Column | Type | Notes |
|--------|------|-------|
| user_id | BIGINT IDENTITY(1,1) PK | |
| username | VARCHAR(255) NOT NULL UNIQUE | Case-insensitive unique constraint |
| password | VARCHAR(255) NOT NULL | Plain text (university project; hashing prohibited) |
| type_of_user | VARCHAR(255) NULL | 'player' or 'moderator' |

The `users` table was created in V1 and has not been modified. No migration is needed to support login, registration, or session management.

## Implemented Components

### AuthService
- `login(String username, String password)` → validates input, finds user by username (trimmed), plain-text comparison, returns User or throws ApiException.
- Returns 400 for blank input, 401 for invalid credentials.
- Same error message for unknown username and wrong password (prevents enumeration).

### AuthController
- `POST /api/auth/login` — authenticates and sets `ctx.sessionAttribute("userId", user.getUserId())`.
- `GET /api/auth/me` — returns current user info or 401.
- Registered in `JavalinConfig.registerRoutes()`.

### UserDao
- `findByUsername(String username)` → `Optional<User>`
- `findById(long userId)` → `Optional<User>`

### Frontend
- Login form on `index.html` (the application root).
- `login.js` handles form submission, calls `POST /api/auth/login`, redirects to dashboard on success.
- `dashboard.js` calls `GET /api/auth/me` to verify session.

### Tests
- `AuthServiceTest` with 10 tests covering: successful login, unknown username, wrong password, blank/null inputs, trimming, null typeOfUser.
- Uses a FakeUserDao (no database dependency).

## Session Approach (Current)

Using Javalin's built-in Jetty session management with `ctx.sessionAttribute()`. Sessions live in server memory.

- Simple, no extra table needed.
- Sessions are lost on server restart.
- No expiry configured.
- Acceptable for a university project during development.

The group has not decided whether to stay with this approach or move to a database-backed session table.

## API Endpoints

### POST /api/auth/login (Implemented)
**Request:**
```json
{
  "username": "string",
  "password": "string"
}
```
**Response (200):**
```json
{
  "success": true,
  "data": { "userId": 1, "username": "playerOne", "typeOfUser": "player" }
}
```
**Errors:** 400 (blank input), 401 (invalid credentials)

### GET /api/auth/me (Implemented)
**Response (200):**
```json
{
  "success": true,
  "data": { "userId": 1, "username": "playerOne", "typeOfUser": "player" }
}
```
**Errors:** 401 (not logged in)

### POST /api/auth/register (Not implemented)
**Planned request:**
```json
{
  "username": "string",
  "password": "string",
  "confirmPassword": "string"
}
```
**Planned response (201):**
```json
{
  "success": true,
  "data": { "userId": 1, "username": "playerOne" }
}
```
**Errors:** 400 (validation), 409 (username taken)

### POST /api/auth/logout (Not implemented)
**Planned response (200):**
```json
{ "success": true }
```

## Key Classes (Existing)

| Class | Package | Status |
|-------|---------|--------|
| AuthController | controller | Implemented (login + me) |
| AuthService | service | Implemented (login only) |
| UserDao | dao | Implemented (findByUsername, findById) |
| User | model | Implemented |
| LoginRequest | dto | Implemented |

## Remaining Design Work

### Registration
- Add `register(String username, String password, String confirmPassword)` to AuthService.
- Validation rules depend on group decision (see unresolved questions).
- UserDao needs `create(...)` and `existsByUsername(...)` methods.
- Create `RegisterRequest` DTO.
- Add `POST /api/auth/register` to AuthController.
- Create `register.html` and `register.js`.

### Logout
- Add `POST /api/auth/logout` to AuthController.
- Invalidate the session via `ctx.req().getSession().invalidate()`.

### Global Authentication Middleware
- A Javalin `before` handler on `/api/*` that:
  1. Skips public routes (exact list pending group decision).
  2. Checks session attribute for authenticated user ID.
  3. If missing, throws `ApiException.unauthorized(...)`.
- Currently, individual controllers perform their own session checks.

### Password Storage
Passwords are stored and compared as plain text (university project requirement; hashing is prohibited). The `password` column in `users` holds the password directly. Login verification is a simple string comparison.

## Proposed Validation Rules (Pending Group Confirmation)

These are design suggestions, not confirmed requirements:
- Username: 3-20 chars, `^[a-zA-Z0-9_]+$`, unique (case-insensitive).
- Password: minimum 8 characters, no complexity rules beyond length.
- Confirm password: must equal password.

The group should confirm or adjust these before implementation.
