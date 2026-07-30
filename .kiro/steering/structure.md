# GameOn - Project Structure

## Directory Layout

```
GameOn-CodeSphere/
├── .kiro/
│   ├── steering/              # Project guidance for Kiro sessions
│   └── specs/                 # Feature specifications (requirements, design, tasks)
├── backend/                   # Java/Maven server application
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd       # Maven Wrapper
│   ├── .mvn/                  # Wrapper config + JVM settings
│   └── src/
│       ├── main/
│       │   ├── java/com/codesphere/gameon/
│       │   │   ├── App.java              # Entry point
│       │   │   ├── config/               # AppConfig, DatabaseConfig, JavalinConfig
│       │   │   ├── controller/           # HTTP route handlers
│       │   │   ├── service/              # Business logic
│       │   │   ├── dao/                  # Database access (SQL + row mapping)
│       │   │   ├── model/                # Domain entities
│       │   │   ├── dto/                  # Request/response data transfer objects
│       │   │   └── exception/            # Custom exceptions
│       │   └── resources/
│       │       ├── db/migration/         # Flyway SQL migrations
│       │       └── logback.xml
│       └── test/
│           └── java/com/codesphere/gameon/  # JUnit 5 tests mirroring main
├── frontend/                  # Static web app (HTML/CSS/JS, no build step)
│   ├── index.html             # Landing/status page
│   ├── css/
│   │   └── main.css           # Shared styles and variables
│   ├── js/
│   │   ├── api.js             # Shared fetch helper (Api.get, Api.post, etc.)
│   │   └── <page>.js          # Page-specific scripts
│   └── pages/                 # Additional HTML pages (login, register, etc.)
├── docs/
│   ├── decisions/             # Architecture Decision Records (ADRs)
│   ├── diagrams/              # System diagrams
│   ├── evidence/              # Test evidence and screenshots
│   └── unresolved-questions.md
├── .env.example               # Environment variable template
├── .gitignore
└── README.md
```

## How Static File Serving Works

The frontend lives in its own top-level `frontend/` directory. At build time, Maven copies the contents of `frontend/` into `target/classes/public/` using a resource configuration in `backend/pom.xml`. Javalin then serves these files via `config.staticFiles.add("/public")`.

This means:
- Frontend developers edit files in `frontend/` directly.
- After editing frontend files, run `cd backend && .\mvnw.cmd clean compile` to copy them into the classpath.
- When running the app, all static content is served from http://localhost:7070/.

## Naming Conventions

| Item | Convention | Example |
|------|-----------|---------|
| Package | lowercase, dot-separated | `com.codesphere.gameon.controller` |
| Class | PascalCase | `GameListingService` |
| Interface | PascalCase (no I-prefix) | `UserDao` (if interface needed) |
| Method | camelCase | `findByUsername()` |
| Constant | UPPER_SNAKE_CASE | `MAX_PLAYERS` |
| DB table | PascalCase (matches FSSB domain) | `GameListing`, `UserSportProfile` |
| DB column | camelCase | `userName`, `skillLevel` |
| Migration | `V<n>__<description>.sql` | `V2__create_users_table.sql` |
| API route | `/api/<resource>` kebab-case | `/api/game-listings` |
| CSS class | kebab-case | `.status-card` |
| JS file | camelCase | `gameListing.js` |
| HTML file | kebab-case | `game-listings.html` |

## How to Add a New Feature

1. Create or review the spec in `.kiro/specs/<feature-name>/`.
2. Write the Flyway migration(s) in `backend/src/main/resources/db/migration/`.
3. Create model class(es) in `backend/.../model/`.
4. Create DAO class(es) extending `BaseDao` in `backend/.../dao/`.
5. Create service class(es) in `backend/.../service/`.
6. Create controller class and register routes in `JavalinConfig.registerRoutes()`.
7. Create request/response DTOs if the API shape differs from the model.
8. Add frontend page(s) and JS in `frontend/`.
9. Write JUnit tests in `backend/src/test/java/`.
10. Run `cd backend && .\mvnw.cmd clean test` to verify nothing is broken.

## Controller Registration Pattern

Every controller follows this pattern:

```java
public class ExampleController {
    private final ExampleService service;

    public ExampleController(ExampleService service) {
        this.service = service;
    }

    public void register(Javalin app) {
        app.get("/api/examples", this::getAll);
        app.post("/api/examples", this::create);
        // ...
    }

    private void getAll(Context ctx) { ... }
    private void create(Context ctx) { ... }
}
```

Controllers are instantiated in `JavalinConfig.registerRoutes()` with their dependencies.

## Build Commands

All Maven commands run from the `backend/` directory:

```bash
cd backend
.\mvnw.cmd clean compile       # Compile + copy frontend
.\mvnw.cmd clean test          # Compile + run tests
.\mvnw.cmd clean package       # Build executable JAR
```
