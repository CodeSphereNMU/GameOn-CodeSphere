# Architecture

## Purpose

This document describes the high-level architecture of GameOn-CodeSphere, including system layers, component interactions, and deployment topology.

---

## Architecture Overview

GameOn-CodeSphere follows a **three-tier architecture**:

```
┌─────────────────────────────────────────────┐
│              Presentation Tier               │
│         (HTML / CSS / JavaScript)            │
│            Multi-page frontend               │
└──────────────────────┬──────────────────────┘
                       │ HTTP (REST API)
┌──────────────────────▼──────────────────────┐
│              Application Tier                │
│         (Java — Servlets + JDBC)             │
│       Servlets → Services → DAOs            │
└──────────────────────┬──────────────────────┘
                       │ JDBC
┌──────────────────────▼──────────────────────┐
│                Data Tier                     │
│          (SQL Server — GameOnDb)             │
└─────────────────────────────────────────────┘
```

---

## Component Diagram

### Frontend (Presentation Tier)

| Component | Responsibility |
|-----------|---------------|
| `frontend/shared/` | Navbar, footer, global styles, utility JS |
| `frontend/auth/` | Login, registration pages |
| `frontend/listings/` | Create, browse, and manage game listings |
| `frontend/social/` | Posts feed, create/edit posts |
| `frontend/profile/` | User profile, sport management |
| `frontend/lobby/` | Game lobby, join requests |
| `frontend/notifications/` | Notification centre |
| `frontend/leaderboard/` | Leaderboard views |
| `frontend/admin/` | Report management (admin panel) |

### Backend (Application Tier)

| Layer | Responsibility |
|-------|---------------|
| Servlets | Handle HTTP requests, validate input, return responses |
| Services | Business logic, rule enforcement, orchestration |
| DAOs | Data access, SQL queries (JDBC), entity mapping |
| Models / Entities | Domain objects representing database tables |
| Util / Config | Application configuration, security, CORS, DB connection |

### Database (Data Tier)

- SQL Server instance hosting `GameOnDb`
- See [database-design.md](database-design.md) for full schema

---

## Communication Flow

1. User interacts with an HTML page in the browser.
2. JavaScript sends an HTTP request (GET/POST/PUT/DELETE) to the backend API.
3. Backend servlet receives the request, delegates to the service layer.
4. Service applies business rules and calls the DAO layer.
5. DAO executes SQL via JDBC against `GameOnDb` and returns data.
6. Response flows back through service → servlet → HTTP response → frontend renders result.

---

## Folder-to-Layer Mapping

```
GameOn-CodeSphere/
├── frontend/       → Presentation Tier
├── backend/        → Application Tier
└── database/       → Data Tier (scripts, migrations)
```

---

## Cross-Cutting Concerns

| Concern | Approach |
|---------|----------|
| Authentication | Session-based; login returns session cookie |
| Authorisation | Role checks in service layer (user vs host vs admin) |
| Error Handling | Global exception handler returns consistent JSON error responses |
| Logging | Server-side logging (java.util.logging) |
| CORS | Configured to allow frontend origin during development |

---

## Deployment (Development)

- **Frontend:** Served as static files (or via Tomcat's static resource folder)
- **Backend:** Apache Tomcat 10+ (WAR deployment)
- **Database:** Local SQL Server instance only (offline — no hosted/remote server available)

---

## Future Considerations

- Containerisation with Docker for consistent environments
- CI/CD pipeline via GitHub Actions
- Production hosting on cloud infrastructure
