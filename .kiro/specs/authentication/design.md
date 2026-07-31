# Authentication & Account Access - Design

## Architecture

```
Frontend (login.html, register.html)
  → fetch() POST /api/auth/login or /api/auth/register
    → AuthController
      → AuthService (validates input, compares passwords, manages sessions)
        → UserDao (CRUD on [User] table)
```

## Database Tables

### [User]
| Column | Type | Notes |
|--------|------|-------|
| userId | INT IDENTITY(1,1) PK | |
| userName | NVARCHAR(50) UNIQUE | Case-insensitive unique; length TBD by group |
| password | NVARCHAR(255) | Plain text (university project; hashing prohibited) |
| typeOfUser | NVARCHAR(20) | 'player' or 'moderator' (from FSSB) |
| createdAt | DATETIME2 | |

Note: The `userName` column length is set conservatively. Final validation rules (min/max length, allowed characters) are pending group decision.

### Session Approach (Pending Group Decision)

**Option A (Proposed):** Use Javalin's built-in Jetty session management with `ctx.sessionAttribute()`. Sessions live in server memory. Simple, no extra table needed, acceptable for a university project.

**Option B:** Custom `[Session]` table with token, userId, expiresAt. More robust but more work.

The group must decide before implementation begins.

## Proposed API Endpoints

### POST /api/auth/register
**Request:**
```json
{
  "username": "string",
  "password": "string",
  "confirmPassword": "string"
}
```
**Response (201):**
```json
{
  "success": true,
  "data": { "userId": 1, "username": "playerOne" }
}
```
**Errors:** 400 (validation), 409 (username taken)

### POST /api/auth/login
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
  "data": { "userId": 1, "username": "playerOne" }
}
```
**Errors:** 401 (invalid credentials)

### POST /api/auth/logout
**Response (200):**
```json
{ "success": true }
```

### GET /api/auth/me
Returns the currently authenticated user (for frontend session checks).
**Response (200):**
```json
{
  "success": true,
  "data": { "userId": 1, "username": "playerOne", "typeOfUser": "player" }
}
```
**Errors:** 401 (not logged in)

## Key Classes

| Class | Package | Responsibility |
|-------|---------|---------------|
| AuthController | controller | Routes, request parsing, response formatting |
| AuthService | service | Validation, password comparison, session logic |
| UserDao | dao | SQL queries for [User] table |
| User | model | Domain entity |
| RegisterRequest | dto | Incoming registration payload |
| LoginRequest | dto | Incoming login payload |

## Proposed Authentication Middleware

A Javalin `before` handler on `/api/*` that:
1. Skips public routes (exact list pending group decision).
2. Checks session attribute for authenticated user ID.
3. If missing, throws `ApiException.unauthorized(...)`.

## Password Storage

Passwords are stored and compared as plain text (university project requirement; hashing is prohibited). The `password` column in the `users` table holds the password directly. Login verification is a simple string comparison.

## Proposed Validation Rules (Pending Group Confirmation)

These are design suggestions, not confirmed requirements:
- Username: 3-20 chars, `^[a-zA-Z0-9_]+$`, unique (case-insensitive).
- Password: minimum 8 characters, no complexity rules beyond length.
- Confirm password: must equal password.

The group should confirm or adjust these before implementation.
