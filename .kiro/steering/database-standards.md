---
inclusion: auto
name: gameon-database-standards
description: Use when working with the GameOn database, SQL, Flyway migrations, DAOs, transactions, schema mappings, identifiers, or database documentation.
---

# GameOn - Database Standards

## Database

- Microsoft SQL Server is used.
- Each developer has a local database instance.
- The database name is `GameOnDB`.
- Flyway manages the database schema and applies migrations automatically when the application starts.
- The existing Flyway migrations and the real database schema are the authority for database names and data types.

## Current Migration Baseline

The following migrations have already been applied:

- V1 established the original GameOn database schema.
- V2 seeded the supported sports, formats and positions.
- V3 aligned the schema with the confirmed Game On business rules.

V3 has been reviewed and successfully applied to `GameOnDB`.

The schema was verified at version 3 as part of checkpoint commit `1657f27`.

## Migration Rules

- Migration location: `backend/src/main/resources/db/migration/`
- Migration naming: `V<number>__<short_description>.sql` using two underscores.
- Never edit an applied migration.
- Do not modify or replace V1, V2 or V3.
- Do not create another migration using an existing version number.
- The next migration must be V4 or later.
- Only create a new migration when a future feature genuinely requires a database change.
- Do not create duplicate tables, columns or seed data that already exist.
- Before writing a migration, inspect the existing migrations and current schema.
- Correct migration mistakes by creating a new migration rather than editing an applied migration.

## Naming Conventions

The real GameOn schema uses lower snake_case.

### Table names

Use the existing lower snake_case table names, for example:

- `users`
- `sport`
- `sport_format`
- `position`
- `format_position`
- `user_sport_profile`
- `game_listing`
- `game_joiner`
- `join_request`
- `notification`
- `match_result`

Do not generate PascalCase alternatives such as `GameListing` or `UserSportProfile`.

### Column names

Use lower snake_case column names, for example:

- `user_id`
- `sport_id`
- `format_id`
- `game_listing_id`
- `start_time`
- `end_time`
- `duration_minutes`
- `created_at`

Do not generate camelCase database columns such as `userId`, `gameListingId` or `createdAt`.

Java fields may use camelCase, but SQL table and column names must match the lower snake_case database schema.

## Data Types

Use the existing migration files and referenced database columns to determine exact data types.

| Java type | Common SQL Server type |
|---|---|
| `int` / `Integer` | `INT` |
| `long` / `Long` | `BIGINT` |
| `String` | `NVARCHAR(n)` |
| `boolean` | `BIT` |
| `LocalDate` | `DATE` |
| `LocalDateTime` | `DATETIME2` |
| `BigDecimal` | `DECIMAL(p,s)` |

GameOn identifiers are primarily `BIGINT`. Java code should normally use `long` or `Long` for these identifiers.

Do not assume that a new identifier should be `INT`. Check the referenced primary or foreign key in the existing schema first.

## Primary and Foreign Keys

- Use the same SQL type for a foreign key as the primary key it references.
- Existing GameOn identifiers are primarily `BIGINT`.
- Follow the key structure already established by V1–V3.
- Some associative tables use composite keys.
- Do not change an existing key type merely to match a generic example.

## SQL Standards

- Always use parameterised queries through `PreparedStatement` and `?` placeholders.
- Never concatenate user input into SQL strings.
- Use explicit column lists instead of `SELECT *` in production code.
- Use transactions for operations containing multiple related database statements.
- Commit the transaction only after every required operation succeeds.
- Roll back the transaction if any required operation fails.
- Prefix stored procedures with `usp_` if stored procedures are introduced.
- Stored procedures are not required by default.

Example:

```java
String sql = """
        SELECT sport_id, sport_name
        FROM sport
        WHERE sport_id = ?
        """;

try (Connection connection = getConnection();
     PreparedStatement statement = connection.prepareStatement(sql)) {

    statement.setLong(1, sportId);

    try (ResultSet resultSet = statement.executeQuery()) {
        // Map the result using the exact existing column names.
    }
} catch (SQLException exception) {
    logger.error("Error finding sport by ID: {}", sportId, exception);
    throw new RuntimeException("Database error", exception);
}
```

## Indexing

- Primary keys are indexed automatically.
- Add indexes only when an identified query or performance requirement needs them.
- Check whether an equivalent index or constraint already exists before adding one.

## DAO Pattern

- DAOs handle database access and row mapping.
- SQL must use the exact lower snake_case names established by the migrations.
- Use `long` or `Long` for identifiers stored as `BIGINT`.
- Use parameterised queries and try-with-resources.
- Services should coordinate transactions involving multiple DAOs.

## Reserved Words

- Use the exact identifiers established by the existing migrations.
- Use square brackets when an existing SQL Server identifier requires escaping.
- Do not rename an existing database object without an approved new migration.

## Schema Documentation

The existing schema export was generated before V3 and is not the current post-V3 schema.

Until a new export is generated:

- Treat V1, V2 and V3 together as the schema authority.
- Clearly identify the old export as a pre-V3 reference.
- Check the migrations before generating code from the old export.