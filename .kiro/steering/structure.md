---
inclusion: auto
name: Project Structure
description: Use when creating, locating, moving, or organising GameOn files, packages, controllers, DAOs, services, frontend assets, tests, documentation, or build configuration.
---

# GameOn - Project Structure

## Directory Layout

```text
GameOn-CodeSphere/
├── .kiro/
│   ├── steering/              # Project-wide guidance for Kiro
│   └── specs/                 # Feature requirements, designs and tasks
├── backend/                   # Java and Maven server application
│   ├── pom.xml
│   ├── mvnw
│   ├── mvnw.cmd
│   ├── .mvn/                  # Maven Wrapper configuration
│   └── src/
│       ├── main/
│       │   ├── java/com/codesphere/gameon/
│       │   │   ├── App.java              # Application entry point
│       │   │   ├── config/               # Application, database and Javalin configuration
│       │   │   ├── controller/           # HTTP route handlers
│       │   │   ├── service/              # Business logic and transaction coordination
│       │   │   ├── dao/                  # SQL access and row mapping
│       │   │   ├── model/                # Domain entities
│       │   │   ├── dto/                  # API request and response objects
│       │   │   └── exception/            # Custom exceptions
│       │   └── resources/
│       │       ├── db/migration/          # Versioned Flyway migrations
│       │       └── logback.xml
│       └── test/
│           └── java/com/codesphere/gameon/  # JUnit 5 tests
├── frontend/                  # Static HTML, CSS and vanilla JavaScript
│   ├── index.html             # Landing and status page
│   ├── css/                   # Shared and page-specific styles
│   ├── js/                    # Shared and page-specific scripts
│   └── pages/                 # Additional HTML pages
├── docs/
│   ├── Database/              # Database exports and related references
│   ├── decisions/             # Architecture Decision Records
│   ├── designs/               # Canva designs and other UI references
│   ├── diagrams/              # System diagrams
│   ├── evidence/              # Test evidence and screenshots
│   └── unresolved-questions.md
├── .env.example               # Environment-variable template
├── .gitignore
└── README.md
```

The local `.env` file contains environment-specific values and must remain excluded from Git. Use `.env.example` to document required variables without including credentials or secrets.

## Static File Serving

The frontend lives in the top-level `frontend/` directory.

During the Maven build, `backend/pom.xml` copies the contents of `frontend/` into:

```text
backend/target/classes/public/
```

Javalin serves these classpath resources using:

```java
config.staticFiles.add("/public");
```

Therefore:

- Edit frontend source files in the top-level `frontend/` directory.
- Do not edit generated copies inside `backend/target/`.
- Run a Maven compile or package command after frontend changes when a fresh classpath copy is required.
- The running application serves the frontend from `http://localhost:7070/` unless the configured port changes.

## Naming Conventions

| Item | Convention | Example |
|---|---|---|
| Java package | Lowercase and dot-separated | `com.codesphere.gameon.controller` |
| Java class | PascalCase | `GameListingService` |
| Java interface | PascalCase without an `I` prefix | `GameListingRepository` |
| Java method | camelCase | `findByUsername()` |
| Java constant | UPPER_SNAKE_CASE | `MAX_PLAYERS` |
| Database table | lower_snake_case | `game_listing` |
| Database column | lower_snake_case | `game_listing_id` |
| Flyway migration | `V<n>__<description>.sql` | `V3__align_schema_confirmed_rules.sql` |
| API route | `/api/<resource>` using kebab-case | `/api/game-listings` |
| CSS class | kebab-case | `.status-card` |
| JavaScript file | camelCase | `createListing.js` |
| HTML file | kebab-case | `create-listing.html` |

Follow `.kiro/steering/database-standards.md` for the complete database and migration rules.

## Adding or Changing a Feature

Start by reviewing the applicable specification and the existing implementation.

Create or modify only the layers required by the feature:

1. Review or update the relevant specification in `.kiro/specs/<feature-name>/`.
2. Add a new Flyway migration only if the schema or required seed data must change.
3. Add or update model classes only when the domain representation requires it.
4. Add or update DAOs when database access or row mapping is required.
5. Add or update services for business rules, validation or transaction coordination.
6. Add or update controllers for HTTP endpoints and register them centrally.
7. Use DTOs when API request or response shapes should not directly expose models.
8. Add or update frontend HTML, CSS and JavaScript when the feature has a user interface.
9. Add or update tests for the behaviour being introduced or changed.
10. Run the relevant tests and build checks before considering the feature complete.

Do not create placeholder layers or files that the feature does not need.

Do not modify an already-applied Flyway migration. Create the next correctly numbered migration for any new database change.

## DAO Pattern

Current concrete DAO classes are stored in:

```text
backend/src/main/java/com/codesphere/gameon/dao/
```

They extend `BaseDao` for shared database access.

DAOs should:

- Contain database queries and row mapping.
- Use the exact database identifiers established by the migrations.
- Use parameterised SQL and try-with-resources.
- Avoid implementing business decisions that belong in services.
- Accept an existing connection when participating in a service-managed transaction.

Services should coordinate operations involving multiple DAOs and control the transaction boundary.

## Controller Registration Pattern

Controllers expose a `register(Javalin app)` method:

```java
public class ExampleController {
    private final ExampleService service;

    public ExampleController(ExampleService service) {
        this.service = service;
    }

    public void register(Javalin app) {
        app.get("/api/examples", this::getAll);
        app.post("/api/examples", this::create);
    }

    private void getAll(Context ctx) {
        // Handle request.
    }

    private void create(Context ctx) {
        // Handle request.
    }
}
```

Controllers and their dependencies are instantiated centrally in:

```text
backend/src/main/java/com/codesphere/gameon/config/JavalinConfig.java
```

Register new controllers through `JavalinConfig.registerRoutes()` rather than creating separate application entry points.

## UI Design References

Canva-derived designs are stored in `docs/designs/Canva/`. These are the authoritative visual references for the GameOn frontend.

When implementing or redesigning a frontend page:

1. Identify the relevant Canva screen(s) for the use case before starting.
2. Use the design's branding, layout, hierarchy, colour direction, spacing, components, and sports-community character as a strong reference.
3. Adapt the design for accessibility (WCAG AA), responsiveness, and actual business rules.
4. Do not replace it with a bare, generic interface merely because that is easier.
5. Continue using HTML, CSS and vanilla JavaScript unless the group approves a change.
6. Compare the completed page against the reference before marking frontend design work complete.

Known issues with the Canva files:
- `31.png` — requires verification (may be blank or a placeholder).
- Several screens have numbered variants; the group should identify current versions before implementing those screens.

## Build Commands

Run Maven commands from the `backend/` directory:

```powershell
cd backend
.\mvnw.cmd clean compile
.\mvnw.cmd clean test
.\mvnw.cmd clean package
```

These commands respectively:

- Compile the application and copy frontend resources.
- Compile the application and run the tests.
- Build the executable JAR using the Maven Shade plugin.

To start the application from source (PowerShell):

```powershell
cd backend
.\mvnw.cmd clean compile exec:java "-Dexec.mainClass=com.codesphere.gameon.App"
```

The configured main class is:

```text
com.codesphere.gameon.App
```

Do not commit generated contents from `backend/target/`.