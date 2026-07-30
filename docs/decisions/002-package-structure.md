# ADR-002: Package and Project Structure

## Status
Accepted

## Date
2026-07-29

## Context
We needed a consistent project structure that all team members can follow, with clear separation of concerns.

## Decision
Use the root package `com.codesphere.gameon` with sub-packages for each layer:
- `config/` — application, database, and server configuration
- `controller/` — HTTP route handlers
- `service/` — business logic and validation
- `dao/` — database access with parameterised SQL
- `model/` — domain entities
- `dto/` — request/response objects for API boundaries
- `exception/` — custom exceptions for controlled error handling

Controllers are registered centrally in `JavalinConfig.registerRoutes()`.

## Rationale
- Matches the layered architecture (Controller → Service → DAO).
- Simple enough that all team members know where to put new code.
- Single Maven module avoids multi-module complexity.
- DTOs separate API shape from internal model, improving security and flexibility.

## Consequences
- All team members must follow the same structure.
- New features follow a predictable pattern: model → dao → service → controller → frontend.
- No circular dependencies between layers (controllers depend on services, services on DAOs).
