# GameOn - Technical Stack & Decisions

## Confirmed Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Web framework | Javalin 6 (embedded Jetty) |
| Build tool | Maven |
| Database | Microsoft SQL Server |
| DB driver | Microsoft JDBC Driver (`mssql-jdbc`) |
| Connection pool | HikariCP |
| Migrations | Flyway |
| JSON | Jackson (with Java Time module) |
| Configuration | Environment variables via `dotenv-java` |
| Frontend | HTML, CSS, vanilla JavaScript |
| API style | JSON REST via browser `fetch()` |
| Testing | JUnit 5 |
| Logging | SLF4J + Logback |
| Source control | Git + GitHub |

## Architecture

```
Frontend (HTML/CSS/JS) → fetch() → Controller → Service → DAO → SQL Server
```

- **Controllers** handle HTTP concerns: request parsing, response formatting, status codes.
- **Services** hold business logic, validation, and workflow orchestration.
- **DAOs** contain parameterised SQL and ResultSet-to-model mapping.
- **DTOs** separate API contracts from internal domain models where it improves clarity.

## Not In Scope

These technologies are explicitly excluded unless the group changes direction:

- Spring Boot
- Hibernate / JPA
- React, Angular, or Vue
- TypeScript
- Supabase
- Replacing SQL Server

## Key Technical Decisions

| Decision | Rationale |
|----------|-----------|
| Single Maven module | Keeps the project simple for a 4-person team |
| Root package `com.codesphere.gameon` | Follows Java convention; assumption (no prior package existed) |
| `dotenv-java` for config | Loads `.env` in dev, system env vars in other environments |
| Flyway baseline-on-migrate | Allows connecting to existing databases without errors |
| Jackson Java Time module | Proper serialisation of `LocalDateTime`, `LocalDate` |
| Shade plugin for JAR | Produces a single executable JAR for simple deployment |
| Port 7070 default | Avoids conflict with common ports (3000, 8080) |

## Environment Variables

| Variable | Purpose | Default |
|----------|---------|---------|
| `DB_HOST` | SQL Server hostname | `localhost` |
| `DB_PORT` | SQL Server port | `1433` |
| `DB_NAME` | Database name | `GameOnDb` |
| `DB_USER` | Database username | (required) |
| `DB_PASSWORD` | Database password | (required) |
| `APP_PORT` | HTTP server port | `7070` |
| `APP_ENV` | Environment (`development`/`production`) | `development` |

## Security Baseline

- Never commit `.env` or real credentials.
- All SQL uses parameterised queries (PreparedStatement).
- Error responses never expose stack traces or internal details.
- Passwords stored as plain text (university project requirement; hashing is prohibited).
- Server-side validation backs all frontend checks.
