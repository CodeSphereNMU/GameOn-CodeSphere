# Backend

## Overview

The backend for GameOn-CodeSphere is a Java-based REST API that serves the frontend via HTTP endpoints. It handles authentication, business logic, and data persistence to SQL Server (`GameOnDb`).

---

## Framework

The backend uses **plain Java with Servlets and JDBC** — no frameworks. This gives full manual control over the application lifecycle.

See the setup guide: [Generic Java Setup](docs/generic-java-setup.md)

---

## Architecture Layers

```
Servlets         → Handle HTTP requests, route to services
Services         → Business logic and rule enforcement
DAOs             → Data access (JDBC)
Models/Entities  → Domain objects mapped to DB tables
Config/Util      → App configuration, security, CORS, DB connection
```

---

## Key Responsibilities

- RESTful API serving JSON responses
- Session-based authentication
- Input validation and error handling
- Database interaction with SQL Server via JDBC
- CORS configuration for frontend access

---

## API Documentation

See [../docs/api-endpoints.md](../docs/api-endpoints.md) for the full endpoint reference.

---

## Running the Backend

1. Configure database connection (SQL Server connection string in `DatabaseConnection.java`).
2. Build the WAR: `mvn clean package`
3. Deploy to Apache Tomcat 10+.
4. API available at `http://localhost:8080/codesphere/api/`.

---

## Environment Configuration

| Variable | Description |
|----------|-------------|
| `DB_URL` | JDBC connection string for GameOnDb |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `SERVER_PORT` | Tomcat port (default: 8080) |
