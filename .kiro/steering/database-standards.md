# GameOn - Database Standards

## Database

- Microsoft SQL Server (local instance per developer).
- Database name: `GameOnDb`.
- Each developer creates the database locally; Flyway handles schema.

## Migrations (Flyway)

- Location: `backend/src/main/resources/db/migration/`
- Naming: `V<number>__<short_description>.sql` (two underscores)
- Once a migration has been applied to any team member's database, it must never be modified.
- To fix a mistake, create a new migration.
- Migrations run automatically on application startup.

## SQL Standards

- **Always use parameterised queries** (PreparedStatement with `?` placeholders).
- Never concatenate user input into SQL strings.
- Use explicit column lists in SELECT (avoid `SELECT *` in production code).
- Use transactions for multi-statement operations.
- Prefix stored procedures with `usp_` if used (not required initially).

## Table Naming

- PascalCase matching the domain model: `User`, `GameListing`, `UserSportProfile`.
- Junction/associative tables: combine both entity names: `GameJoiner`, `Follow`.

## Column Naming

- camelCase: `userName`, `skillLevel`, `gameListingId`.
- Primary keys: `<entityName>Id` (e.g., `userId`, `gameListingId`).
- Foreign keys: same name as the referenced PK where practical.
- Boolean columns: use `is` prefix where it reads naturally (`isCompleted`, `isRead`).
- Timestamps: `createdAt`, `updatedAt` (use `DATETIME2`).

## Data Types

| Java Type | SQL Server Type |
|-----------|----------------|
| `int` / `Integer` | `INT` |
| `long` / `Long` | `BIGINT` |
| `String` | `NVARCHAR(n)` |
| `boolean` | `BIT` |
| `LocalDate` | `DATE` |
| `LocalDateTime` | `DATETIME2` |
| `BigDecimal` | `DECIMAL(p,s)` |

## Primary Keys

- Use `INT IDENTITY(1,1)` for auto-increment integer PKs.
- Composite keys for junction tables (e.g., `GameJoiner` uses `userId + gameListingId`).

## Indexing

- PKs are indexed automatically.
- Add indexes for columns frequently used in WHERE/JOIN (add as performance needs arise, not prematurely).

## DAO Pattern

```java
public class UserDao extends BaseDao {
    public UserDao(DataSource dataSource) {
        super(dataSource);
    }

    public Optional<User> findById(int userId) {
        String sql = "SELECT userId, userName, password, typeOfUser FROM [User] WHERE userId = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding user by ID: {}", userId, e);
            throw new RuntimeException("Database error", e);
        }
        return Optional.empty();
    }

    private User mapRow(ResultSet rs) throws SQLException {
        // Map columns to domain object
    }
}
```

## Reserved Words

SQL Server reserves words like `User`, `Date`, `Status`. Wrap table names in square brackets: `[User]`, or choose non-reserved names.
