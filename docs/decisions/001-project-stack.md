# ADR-001: Project Technology Stack

## Status
Accepted

## Date
2026-07-29

## Context
CodeSphere needed to choose a technology stack for GameOn that:
- Is familiar or learnable by all 4 team members (university students).
- Meets the module requirements (Java-based, SQL Server, web frontend).
- Is simple enough for a semester project without excessive boilerplate.
- Supports a clear layered architecture.

## Decision
We will use:
- **Java 21** with **Javalin 6** (lightweight web framework with embedded Jetty).
- **Maven** for build management.
- **Microsoft SQL Server** with **Microsoft JDBC Driver** and **HikariCP** connection pooling.
- **Flyway** for database migrations.
- **Jackson** for JSON serialisation.
- **HTML/CSS/vanilla JavaScript** for the frontend, communicating via `fetch()` to JSON APIs.
- **JUnit 5** for testing.
- **dotenv-java** for environment configuration.

## Alternatives Considered
- **Spring Boot:** Too heavyweight for the project scope; excessive boilerplate and concepts to learn.
- **Hibernate/JPA:** Adds abstraction complexity; parameterised JDBC keeps SQL visible and learnable.
- **React/Vue/Angular:** Adds build tooling complexity (Node, bundlers) with limited benefit for this scope.
- **Supabase:** Would replace SQL Server and change the architecture fundamentally.

## Consequences
- Simple setup: one JAR, one `main()` method, no complex deployment.
- Team must write SQL directly (good for learning, more verbose than ORM).
- No automatic schema generation; Flyway migrations must be written manually.
- Frontend has no component system or state management (acceptable for project size).
