# Backend

## Overview

The backend for GameOn-CodeSphere is a Java-based REST API that serves the frontend via HTTP endpoints. It handles authentication, business logic, and data persistence to SQL Server (`GameOnDb`).

---

## Framework Options

The team has two framework paths documented. Choose one and follow the corresponding setup guide:

| Option | Guide | Best For |
|--------|-------|----------|
| Spring Boot (Maven) | [docs/springboot-setup.md](docs/springboot-setup.md) | Rapid development, built-in dependency injection, auto-configuration |
| Generic Java (Servlets) | [docs/generic-java-setup.md](docs/generic-java-setup.md) | Lightweight, manual control, fewer abstractions |

---

## Architecture Layers

```
Controllers      → Handle HTTP requests, route to services
Services         → Business logic and rule enforcement
Repositories     → Data access (JDBC or JPA)
Models/Entities  → Domain objects mapped to DB tables
Config           → App configuration, security, CORS
```

---

## Key Responsibilities

- RESTful API serving JSON responses
- Session-based authentication
- Input validation and error handling
- Database interaction with SQL Server
- CORS configuration for frontend access

---

## API Documentation

See [../docs/api-endpoints.md](../docs/api-endpoints.md) for the full endpoint reference.

---

## Running the Backend

Refer to the chosen framework guide for specific run instructions. General workflow:

1. Configure database connection (SQL Server connection string).
2. Build the project.
3. Run the application.
4. API available at `http://localhost:8080/api`.

---

## Environment Configuration

| Variable | Description |
|----------|-------------|
| `DB_URL` | JDBC connection string for GameOnDb |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `SERVER_PORT` | Application port (default: 8080) |
