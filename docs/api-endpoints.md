# API Endpoints

## Purpose

This document defines the RESTful API endpoints exposed by the GameOn-CodeSphere backend. All endpoints return JSON responses.

---

## Base URL

```
http://localhost:8080/api
```

---

## Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/auth/register` | Register a new user |
| POST | `/auth/login` | Authenticate and create session |
| POST | `/auth/logout` | Invalidate session |

---

## Users (D100–D700)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/users/{id}` | Get user profile |
| PUT | `/users/{id}` | Update user profile |
| POST | `/users/{id}/sports` | Add sport to user profile |
| DELETE | `/users/{id}/sports/{sportId}` | Remove sport from profile |
| POST | `/users/{id}/follow` | Follow a user |
| DELETE | `/users/{id}/follow` | Unfollow a user |
| GET | `/users/{id}/followers` | Get user's followers |
| GET | `/users/{id}/following` | Get users being followed |

---

## Listings (A100–A700)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/listings` | Create a new game listing |
| GET | `/listings` | Browse listings (supports filters) |
| GET | `/listings/{id}` | Get listing details |
| PUT | `/listings/{id}` | Update listing |
| DELETE | `/listings/{id}` | Cancel listing |
| POST | `/listings/{id}/join` | Send join request |
| DELETE | `/listings/{id}/leave` | Leave a listing |
| POST | `/listings/{id}/remind` | Send reminder to joiners |
| POST | `/listings/{id}/confirm` | Confirm session |

### Query Parameters for Browse

| Parameter | Type | Description |
|-----------|------|-------------|
| sport | INT | Filter by sport ID |
| date | DATE | Filter by game date |
| status | STRING | Filter by status (Open, Full) |
| page | INT | Page number (default: 1) |
| size | INT | Page size (default: 10) |

---

## Join Requests (C500)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/listings/{id}/joiners` | View join requests for a listing |
| PUT | `/listings/{id}/joiners/{joinerId}` | Approve or reject join request |

---

## Match Results (C100–C400)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/sessions/{sessionId}/results` | Record match result |
| PUT | `/sessions/{sessionId}/results` | Update match result |
| GET | `/sessions/{sessionId}/results` | View match result |
| GET | `/users/{id}/results` | View user's match history |

---

## Posts (B100–B300)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/posts` | Create a new post |
| GET | `/posts` | Browse posts (feed) |
| GET | `/posts/{id}` | Get post details |
| PUT | `/posts/{id}` | Update post |
| DELETE | `/posts/{id}` | Delete post |
| POST | `/posts/{id}/like` | Like a post |
| DELETE | `/posts/{id}/like` | Unlike a post |
| POST | `/posts/{id}/comments` | Add comment |
| GET | `/posts/{id}/comments` | Get post comments |

---

## Leaderboards (B500)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/leaderboards` | Get overall leaderboard |
| GET | `/leaderboards/{sportId}` | Get leaderboard by sport |

---

## Notifications (D500)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/notifications` | Get current user's notifications |
| PUT | `/notifications/{id}/read` | Mark notification as read |
| PUT | `/notifications/read-all` | Mark all as read |

---

## Reports (B400, D600, D700)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/reports` | Submit a report (user or post) |
| GET | `/reports` | View all reports (admin) |
| PUT | `/reports/{id}` | Update report status (admin) |

---

## Common Response Codes

| Code | Meaning |
|------|---------|
| 200 | Success |
| 201 | Created |
| 400 | Bad Request (validation error) |
| 401 | Unauthorised |
| 403 | Forbidden |
| 404 | Not Found |
| 409 | Conflict (e.g., duplicate join request) |
| 500 | Internal Server Error |

---

## Response Format

All responses follow this structure:

```json
{
  "success": true,
  "data": { },
  "message": "Operation completed successfully"
}
```

Error responses:

```json
{
  "success": false,
  "data": null,
  "message": "Validation failed",
  "errors": ["Field 'title' is required"]
}
```
