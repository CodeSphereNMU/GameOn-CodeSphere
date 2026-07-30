# GameOn

Sports organisation and community platform by **CodeSphere**.

GameOn helps sports players create structured game listings, find suitable games and opponents, organise teams and positions, manage participants, and record match results.

## Team

| Member | Use Cases |
|--------|-----------|
| Lihlumelo Mgijima | A100-A700 (Listings, join requests, lifecycle) |
| Zane Griesel | B100-B500 (Posts, moderation, leaderboards) |
| Gerard Mc Loughlin | C100-C500 (Match results, listing management) |
| Robert Lloyd | D100-D700 (Registration, profiles, notifications, reporting) |

## Tech Stack

- Java 21
- Javalin 6 (embedded Jetty)
- Maven
- Microsoft SQL Server
- HikariCP (connection pool)
- Flyway (database migrations)
- Jackson (JSON)
- HTML / CSS / vanilla JavaScript
- JUnit 5

## Project Structure

```
GameOn-CodeSphere/
├── .kiro/
│   ├── steering/              # Project standards and guidelines
│   └── specs/                 # Feature specifications
├── backend/                   # Java/Maven server application
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd       # Maven Wrapper (no system Maven needed)
│   ├── .mvn/                  # Wrapper config + JVM settings
│   └── src/
│       ├── main/
│       │   ├── java/com/codesphere/gameon/
│       │   │   ├── App.java
│       │   │   ├── config/
│       │   │   ├── controller/
│       │   │   ├── service/
│       │   │   ├── dao/
│       │   │   ├── model/
│       │   │   ├── dto/
│       │   │   └── exception/
│       │   └── resources/
│       │       ├── db/migration/
│       │       └── logback.xml
│       └── test/
│           └── java/          # JUnit 5 tests
├── frontend/                  # Static web app (HTML/CSS/JS, no build step)
│   ├── index.html
│   ├── css/
│   │   └── main.css
│   ├── js/
│   │   ├── api.js
│   │   └── health.js
│   └── pages/                 # Additional pages (login, register, etc.)
├── docs/
│   ├── decisions/             # Architecture Decision Records
│   ├── diagrams/              # System diagrams
│   ├── evidence/              # Test evidence for submissions
│   └── unresolved-questions.md
├── .env.example
├── .gitignore
└── README.md
```

## Prerequisites

1. **Java 21+ JDK** — [Download from Adoptium](https://adoptium.net/)
2. **Microsoft SQL Server** — Local instance (Developer Edition is free)
3. **Git**

Maven is handled by the included wrapper (`mvnw` / `mvnw.cmd`) — no system install needed.

Verify Java:
```bash
java --version    # Should show 21+
```

## Database Setup

1. Open SQL Server Management Studio (SSMS) or Azure Data Studio.
2. Create a new database:
   ```sql
   CREATE DATABASE GameOnDb;
   ```
3. Ensure your SQL Server instance accepts TCP connections on port 1433.

## Configuration

1. Copy the environment template (from the repository root):
   ```bash
   cp .env.example .env
   ```
2. Edit `.env` with your local values:
   ```
   DB_HOST=localhost
   DB_PORT=1433
   DB_NAME=GameOnDb
   DB_USER=sa
   DB_PASSWORD=YourActualPassword
   APP_PORT=7070
   APP_ENV=development
   ```
3. **Never commit `.env` to Git.** It is already in `.gitignore`.

The `.env` file lives at the repository root. The backend automatically loads it from `../` when running from `backend/`. System environment variables take precedence over `.env` values.

## Build and Run

All Maven commands run from the `backend/` directory:

### Compile
```bash
cd backend
.\mvnw.cmd clean compile
```

### Run Tests
```bash
cd backend
.\mvnw.cmd clean test
```

### Start the Application
```bash
cd backend
.\mvnw.cmd clean compile exec:java -Dexec.mainClass="com.codesphere.gameon.App"
```

Or build and run the JAR:
```bash
cd backend
.\mvnw.cmd clean package -DskipTests
java -jar target/gameon-1.0-SNAPSHOT.jar
```

### Access the Application
- **Web UI:** http://localhost:7070
- **Health API:** http://localhost:7070/api/health

The backend serves the frontend files automatically. At build time, Maven copies everything from `frontend/` into the backend's classpath so Javalin serves them as static content.

## Development Workflow

1. Pull latest `main`.
2. Create a feature branch: `git checkout -b feature/D100-user-registration`
3. Implement the feature following the spec in `.kiro/specs/`.
4. Run tests: `cd backend && .\mvnw.cmd clean test`
5. Push and create a Pull Request.
6. Get at least one review, then merge.

See `.kiro/steering/git-workflow.md` for full details.

## Adding a New Feature

1. Review or create the spec in `.kiro/specs/<feature-name>/`.
2. Write the Flyway migration in `backend/src/main/resources/db/migration/`.
3. Create model → DAO → service → controller in `backend/src/main/java/`.
4. Register the controller in `JavalinConfig.registerRoutes()`.
5. Add frontend pages/JS in `frontend/`.
6. Write tests in `backend/src/test/java/`.
7. Run `cd backend && .\mvnw.cmd clean test`.

## Troubleshooting

**Connection refused on port 1433:**
- Ensure SQL Server is running and TCP/IP is enabled in SQL Server Configuration Manager.

**Authentication failed:**
- Verify your `.env` credentials match your SQL Server login.

**Port 7070 already in use:**
- Change `APP_PORT` in your `.env` file.

**Frontend changes not showing:**
- Run `.\mvnw.cmd clean compile` to re-copy frontend files into the classpath.
- Or restart the app (it copies on build).
