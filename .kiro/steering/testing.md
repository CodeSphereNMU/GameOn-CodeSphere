# GameOn - Testing Standards

## Framework

- JUnit 5 (Jupiter) for all Java tests.
- Tests live in `backend/src/test/java/` mirroring the main source structure.

## What to Test

| Layer | What to Test | Priority |
|-------|-------------|----------|
| Service | Business logic, validation rules, edge cases | High |
| DAO | SQL correctness, mapping, edge cases | Medium |
| Controller | Route registration, status codes, response shape | Medium |
| Integration | End-to-end flows (requires running DB) | Lower (later) |

## Test Naming

```java
@Test
void shouldRejectJoinRequestWhenListingIsFull() { ... }

@Test
void shouldReturnEmptyListWhenNoListingsExist() { ... }
```

Use `should<Expected>When<Condition>` for clarity.

## Test Organisation

```
backend/src/test/java/com/codesphere/gameon/
├── config/
│   └── AppConfigTest.java
├── dto/
│   └── ApiResponseTest.java
├── exception/
│   └── ApiExceptionTest.java
├── service/
│   └── UserServiceTest.java      (when implemented)
├── dao/
│   └── UserDaoTest.java          (when implemented)
└── controller/
    └── HealthControllerTest.java  (when implemented)
```

## Running Tests

```bash
cd backend
.\mvnw.cmd clean test
```

Tests must pass before pushing to the shared branch.

## Database in Tests

- Unit tests for services should not require a database (mock the DAO or use in-memory state).
- DAO tests require a real SQL Server connection. Mark these with a tag or skip if DB is unavailable.
- Use a separate test database or transaction rollback to keep tests isolated.

**Proposal (requires group confirmation):** Use a `GameOnDb_Test` database for DAO integration tests, configured via a separate test `.env` or system properties.

## Test Evidence

- Screenshots and test run outputs go in `docs/evidence/`.
- For assessed submissions, capture both passing tests and UI demonstrations.

## Coverage

- No formal coverage target, but aim to test all business rules defined in requirements.
- Critical business rules (scheduling conflicts, role permissions, listing lifecycle) should always have tests.
