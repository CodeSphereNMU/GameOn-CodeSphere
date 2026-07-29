# GameOn-CodeSphere

A sports match booking platform with social features. Users can create and join game listings, record match results, follow other players, and engage with a community feed.

---

## Team

| Member | Module | Use Cases |
|--------|--------|-----------|
| Lihlumelo Mgijima | Listings | A100–A700: Create/Browse Listings, Join/Leave, Reminders, Confirm Session |
| Zane Griesel | Social / Posts | B100–B500: Create/Manage/Browse Posts, View Reports, Leaderboards |
| Gerard Mc Loughlin | Match Results | C100–C500: Record/Update Results, Manage Listing, View Results/Join Requests |
| Robert Lloyd | User Management | D100–D700: Register, Profile, Add Sport, Follow, Notifications, Reports |

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | HTML5, CSS3, JavaScript (ES6+) — multi-page application |
| Backend | Java (Spring Boot with Maven **or** generic Java Servlets) |
| Database | Microsoft SQL Server (`GameOnDb`) |
| Version Control | Git + GitHub |

---

## Project Structure

```
GameOn-CodeSphere/
├── .gitignore
├── README.md                        ← You are here
├── docs/
│   ├── requirements.md
│   ├── business-rules.md
│   ├── system-constraints.md
│   ├── architecture.md
│   ├── database-design.md
│   ├── api-endpoints.md
│   ├── specs/
│   │   ├── listings-spec.md
│   │   ├── social-spec.md
│   │   ├── match-results-spec.md
│   │   └── user-management-spec.md
│   └── tasks/
│       ├── lihlumelo-tasks.md
│       ├── zane-tasks.md
│       ├── gerard-tasks.md
│       └── robert-tasks.md
├── backend/
│   ├── README.md
│   └── docs/
│       ├── springboot-setup.md
│       └── generic-java-setup.md
├── frontend/
│   ├── README.md
│   ├── shared/
│   ├── auth/
│   ├── listings/
│   ├── social/
│   ├── profile/
│   ├── lobby/
│   ├── notifications/
│   ├── leaderboard/
│   └── admin/
└── database/
    └── README.md
```

---

## Getting Started

### Prerequisites

- Java 17+ (JDK)
- Maven 3.8+
- SQL Server (local or remote)
- Modern browser (Chrome, Firefox, or Edge)
- Git

### Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/your-org/GameOn-CodeSphere.git
   cd GameOn-CodeSphere
   ```

2. Create the database `GameOnDb` on your SQL Server instance.

3. Choose a backend framework and follow the corresponding guide:
   - [Spring Boot Setup](backend/docs/springboot-setup.md)
   - [Generic Java Setup](backend/docs/generic-java-setup.md)

4. Configure your database connection (see chosen guide).

5. Run the backend (default: `http://localhost:8080`).

6. Open frontend pages in a browser or via Live Server.

---

## Branching Strategy

```
main            ← production-ready, protected
  └── develop   ← integration branch, all features merge here
        ├── feature/A100-create-listing
        ├── feature/B100-create-posts
        ├── feature/C100-record-result
        └── feature/D100-register-user
```

### Rules

| Rule | Description |
|------|-------------|
| Feature branches | Create from `develop`, named `feature/<ID>-<short-description>` |
| Pull requests | All merges to `develop` via PR with at least one reviewer |
| Main merges | Only `develop` → `main` when a milestone is stable |
| No direct commits | Never commit directly to `main` or `develop` |
| Naming convention | Use use-case ID prefix (e.g., `feature/A100-create-listing`) |

### Workflow

1. Pull latest `develop`.
2. Create your feature branch: `git checkout -b feature/A100-create-listing`
3. Commit regularly with meaningful messages.
4. Push and open a Pull Request to `develop`.
5. Address review comments, then merge.
6. Delete the feature branch after merge.

---

## Documentation

| Document | Description |
|----------|-------------|
| [Requirements](docs/requirements.md) | Functional and non-functional requirements |
| [Business Rules](docs/business-rules.md) | Domain logic and constraints |
| [System Constraints](docs/system-constraints.md) | Technical and organisational constraints |
| [Architecture](docs/architecture.md) | System architecture and component design |
| [Database Design](docs/database-design.md) | Full schema with entity relationships |
| [API Endpoints](docs/api-endpoints.md) | RESTful API reference |

---

## Contributing

1. Pick a task from your [task tracker](docs/tasks/).
2. Create a feature branch from `develop`.
3. Implement the feature following the [spec](docs/specs/) for your module.
4. Test locally (frontend + backend integration).
5. Submit a PR and request a review.

---

## License

This project is developed for academic purposes as part of a university module.
