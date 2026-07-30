# GameOn - API Standards

## Base URL

All API routes are prefixed with `/api/`.

## HTTP Methods

| Method | Usage |
|--------|-------|
| GET | Retrieve resource(s), no side effects |
| POST | Create a new resource |
| PUT | Update an existing resource (full replacement) |
| PATCH | Partial update (use sparingly) |
| DELETE | Remove a resource |

## Response Format

All responses use the `ApiResponse` wrapper:

```json
// Success
{
  "success": true,
  "data": { ... }
}

// Success (no data)
{
  "success": true
}

// Error
{
  "success": false,
  "error": "Human-readable error message"
}
```

## Status Codes

| Code | When to Use |
|------|-------------|
| 200 | Successful GET, PUT, PATCH |
| 201 | Successful POST (resource created) |
| 204 | Successful DELETE (no body) |
| 400 | Validation failure, malformed request |
| 401 | Not authenticated |
| 403 | Authenticated but not authorised |
| 404 | Resource not found |
| 409 | Conflict (e.g., duplicate username) |
| 500 | Unexpected server error |

## Route Naming

- Use kebab-case for multi-word resources: `/api/game-listings`
- Use plural nouns for collections: `/api/users`, `/api/posts`
- Use path parameters for specific resources: `/api/users/{userId}`
- Nest sub-resources only one level deep: `/api/game-listings/{id}/join-requests`
- Use query parameters for filtering: `/api/game-listings?sport=soccer&skill=intermediate`

## Request Bodies

- Always JSON (`Content-Type: application/json`).
- Use camelCase for field names.
- Validate all input server-side regardless of frontend validation.
- Return specific 400 error messages explaining what failed.

## Pagination (when needed)

```
GET /api/posts?page=1&size=20

Response:
{
  "success": true,
  "data": {
    "items": [...],
    "page": 1,
    "size": 20,
    "totalItems": 87,
    "totalPages": 5
  }
}
```

## Authentication (future)

- Session-based authentication (server-side sessions).
- Protected routes return 401 if no valid session.
- Login: `POST /api/auth/login`
- Register: `POST /api/auth/register`
- Logout: `POST /api/auth/logout`

## Versioning

No API versioning for now. If breaking changes are needed later, prefix with `/api/v2/`.
