# GameOn — Security Implementation & Deployment Guide

> **Database:** GameOnDb (SQL Server)  
> **Stack:** Java 21 | Spring Boot 3.2 | Spring Security 6 | Hibernate | Flyway  
> **Document Purpose:** Complete security documentation, permissions matrix, and deployment checklist

---

## 1. Authentication Architecture

### 1.1 Authentication Flow

```
User submits login form (POST /login)
    → Spring Security SecurityFilterChain intercepts
    → CustomUserDetailsService.loadUserByUsername()
    → Query users table by username
    → BCryptPasswordEncoder.matches(rawPassword, encodedPassword)
    → Success: Create SecurityContext + HttpSession → Role-based redirect
    → Failure: Redirect to /login?error=true
```

### 1.2 Password Security

| Aspect | Implementation |
|--------|---------------|
| Algorithm | BCrypt (adaptive hashing) |
| Strength | 10 rounds ($2a$10$...) |
| Storage | users.password column (VARCHAR 255) |
| Registration | `passwordEncoder.encode(rawPassword)` |
| Login | Spring Security auto-compares via `matches()` |
| Plain text | NEVER stored anywhere |

### 1.3 Session Management

| Aspect | Configuration |
|--------|---------------|
| Session type | Server-side HttpSession |
| Timeout | 60 minutes (server.servlet.session.timeout=60m) |
| Concurrent sessions | 1 per user (maximum-sessions=1) |
| Cookie | HTTP-only, name=JSESSIONID |
| Logout | Invalidate session + delete cookies |
| Expired redirect | /login?expired=true |

---

## 2. Roles & Permissions

### 2.1 Role Definitions

| Role | Spring Authority | Purpose | Default |
|------|------------------|---------|---------|
| USER | ROLE_USER | Standard authenticated user | Assigned on registration |
| MODERATOR | ROLE_MODERATOR | Content governance (view/action reports) | Seeded account |
| ADMIN | ROLE_ADMIN | System administration | Seeded account |

### 2.2 Permissions Matrix

| Feature | Anonymous | USER | MODERATOR | ADMIN |
|---------|-----------|------|-----------|-------|
| View Login/Register | ✔ | ✘ (redirect) | ✘ | ✘ |
| Browse Listings | ✘ | ✔ | ✔ | ✔ |
| Create Listing | ✘ | ✔ | ✔ | ✔ |
| Join Listing | ✘ | ✔ | ✔ | ✔ |
| Manage Own Listing | ✘ | ✔ (own only) | ✔ (own only) | ✔ |
| View/Accept Join Requests | ✘ | ✔ (own listing) | ✔ (own listing) | ✔ |
| Record Match Result | ✘ | ✔ (own listing) | ✔ (own listing) | ✔ |
| Create Post | ✘ | ✔ | ✔ | ✔ |
| Edit/Delete Own Post | ✘ | ✔ (own only) | ✔ (own only) | ✔ |
| Delete Any Post | ✘ | ✘ | ✔ | ✔ |
| Like/Comment on Post | ✘ | ✔ | ✔ | ✔ |
| View Profile | ✘ | ✔ | ✔ | ✔ |
| Edit Own Profile | ✘ | ✔ | ✔ | ✔ |
| Follow/Unfollow | ✘ | ✔ | ✔ | ✔ |
| View Notifications | ✘ | ✔ (own) | ✔ (own) | ✔ |
| View Leaderboard | ✘ | ✔ | ✔ | ✔ |
| Report User/Post | ✘ | ✔ | ✔ | ✔ |
| View Reports Dashboard | ✘ | ✘ | ✔ | ✔ |
| Dismiss Report | ✘ | ✘ | ✔ | ✔ |
| Remove User/Post (from report) | ✘ | ✘ | ✔ | ✔ |
| Ban/Deactivate User | ✘ | ✘ | ✔ | ✔ |
| Manage Roles | ✘ | ✘ | ✘ | ✔ |
| System Configuration | ✘ | ✘ | ✘ | ✔ |

### 2.3 Route Security Configuration

| URL Pattern | Access | Notes |
|-------------|--------|-------|
| `/login` | permitAll | Login page |
| `/register` | permitAll | Registration Step 1 |
| `/register-sports` | permitAll | Registration Step 2 |
| `/css/**`, `/js/**`, `/images/**` | permitAll | Static resources |
| `/actuator/health` | permitAll | Health check |
| `/listings/**` | authenticated | Browse/create listings |
| `/game-listing/**` | authenticated | Listing CRUD |
| `/game-joiner/**` | authenticated | Join/leave/requests |
| `/match-result/**` | authenticated | Score operations |
| `/social/**` | authenticated | Social feed |
| `/post/**` | authenticated | Post CRUD |
| `/profile/**` | authenticated | Profile management |
| `/notifications/**` | authenticated | Notifications |
| `/report/**` | authenticated | Submit reports |
| `/leaderboard/**` | authenticated | Rankings |
| `/lobby/**` | authenticated | Lobby tabs |
| `/moderator/**` | hasRole('MODERATOR') | Reports dashboard |
| `/admin/**` | hasRole('ADMIN') | Administration |

---

## 3. Security Hardening

### 3.1 CSRF Protection

- **Enabled** globally via `CookieCsrfTokenRepository.withHttpOnlyFalse()`
- Thymeleaf auto-injects CSRF token in all `th:action` forms
- All POST/PUT/DELETE requests must include valid CSRF token
- Token stored in cookie for JavaScript AJAX compatibility

### 3.2 XSS Protection

- Thymeleaf auto-escapes all `th:text` output (prevents stored XSS)
- Content Security Policy header restricts script sources
- Input validation via Bean Validation (`@NotBlank`, `@Size`, `@Pattern`)
- `X-XSS-Protection` header enabled

### 3.3 SQL Injection Prevention

- All database queries use JPA/JPQL parameterized queries
- Spring Data JPA derived queries are safe by design
- Custom `@Query` annotations use named parameters (`:paramName`)
- No raw SQL string concatenation anywhere in code

### 3.4 Input Validation

| Layer | Mechanism | Examples |
|-------|-----------|---------|
| Client | HTML5 required/pattern attributes | Required fields, min/max |
| Controller | `@Valid` + `BindingResult` | Bean Validation annotations |
| Service | Business rule checks | BR1-BR14 enforcement |
| Database | CHECK constraints | Enum values, ranges |

### 3.5 Authorization Checks

| Level | Mechanism | Purpose |
|-------|-----------|---------|
| Route | `SecurityFilterChain` | URL-level access control |
| Controller | `@PreAuthorize` | Method-level role checks |
| Service | `SecurityUtils.isCurrentUser()` | Ownership verification |
| Service | Business rule validation | BR7, BR9, BR11, BR13 |

### 3.6 Secure Headers

| Header | Value | Purpose |
|--------|-------|---------|
| X-Frame-Options | SAMEORIGIN | Prevent clickjacking |
| Content-Security-Policy | Restrictive policy | Prevent XSS |
| X-Content-Type-Options | nosniff | MIME type sniffing prevention |
| Strict-Transport-Security | max-age=31536000 (prod) | Force HTTPS |

### 3.7 Session Security

- HTTP-only session cookies (no JavaScript access)
- Secure cookies in production (HTTPS only)
- Session fixation protection (default Spring Security)
- Maximum 1 concurrent session per user

### 3.8 Rate Limiting (Recommended)

For production deployment, add rate limiting via:
- Spring Cloud Gateway
- Bucket4j library
- Nginx/reverse proxy rate limiting

Recommended limits:
- Login attempts: 5 per minute per IP
- Registration: 3 per hour per IP
- API calls: 100 per minute per user

---

## 4. Database Security

### 4.1 Least Privilege SQL User

```sql
-- Create dedicated application user with minimal permissions
CREATE LOGIN gameon_user WITH PASSWORD = 'StrongPassword!2026';
USE GameOnDb;
CREATE USER gameon_user FOR LOGIN gameon_user;

-- Grant read/write on application tables only
EXEC sp_addrolemember 'db_datareader', 'gameon_user';
EXEC sp_addrolemember 'db_datawriter', 'gameon_user';

-- Grant execute on stored procedures
GRANT EXECUTE ON sp_CalculateWinPercentage TO gameon_user;
GRANT EXECUTE ON sp_GetLeaderboard TO gameon_user;
GRANT EXECUTE ON sp_GetPendingReportsCount TO gameon_user;

-- DENY dangerous operations
DENY ALTER ON SCHEMA::dbo TO gameon_user;
DENY DROP ON SCHEMA::dbo TO gameon_user;
```

### 4.2 Connection Encryption

```properties
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=GameOnDb;encrypt=true;trustServerCertificate=true
```

For production, use proper SSL certificates:
```properties
spring.datasource.url=jdbc:sqlserver://prod-server:1433;databaseName=GameOnDb;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net
```

### 4.3 Backup Strategy

| Type | Frequency | Retention |
|------|-----------|-----------|
| Full backup | Daily (midnight) | 30 days |
| Differential | Every 6 hours | 7 days |
| Transaction log | Every 15 minutes | 3 days |

### 4.4 Recovery Strategy

- RPO (Recovery Point Objective): 15 minutes
- RTO (Recovery Time Objective): 1 hour
- Test restore procedure monthly
- Store backups in separate location from primary server

---

## 5. Audit Logging

### 5.1 JPA Auditing (Automatic)

All entities extending `Auditable` automatically track:
- `created_at` - When the record was created
- `updated_at` - When the record was last modified
- `created_by` - Username of creator (from SecurityContext)
- `updated_by` - Username of last modifier (from SecurityContext)

### 5.2 Audit Log Table

The `audit_log` table captures significant actions:
- Entity type and ID
- Action (CREATE, UPDATE, DELETE)
- Performed by (username)
- Timestamp
- Old/new values (JSON)

### 5.3 Security Events Logged

| Event | Log Level | Details |
|-------|-----------|---------|
| Successful login | INFO | Username, IP |
| Failed login | WARN | Username attempted, IP |
| Account deactivated | WARN | Target user, admin who deactivated |
| Role changed | WARN | User, old role, new role, admin |
| Report actioned | INFO | Report ID, action taken, moderator |
| User removed | WARN | Removed user ID, moderator |

---

## 6. Test Accounts

| Username | Password | Role | Sports |
|----------|----------|------|--------|
| Zane | Test123 | USER | Tennis (Advanced), Football (Intermediate) |
| Lihlumelo | Test123 | USER | Football (Advanced), Basketball (Beginner) |
| Gerard | Test123 | USER | Basketball (Intermediate), Padel (Advanced) |
| Robert | Test123 | USER | Tennis (Beginner), Padel (Intermediate) |
| Moderator | Admin123 | MODERATOR | — |
| Admin | Admin123 | ADMIN | — |

---

## 7. Deployment Checklist

### 7.1 Pre-Deployment

- [ ] SQL Server instance running and accessible
- [ ] `CREATE DATABASE GameOnDb` executed
- [ ] Application user created with least-privilege
- [ ] Connection string configured in application.properties
- [ ] `DB_PASSWORD` environment variable set
- [ ] Flyway migrations verified (V1-V5)
- [ ] All tests passing (`mvn clean test`)
- [ ] Production profile activated (`spring.profiles.active=prod`)
- [ ] HTTPS configured (or behind reverse proxy with TLS)
- [ ] Logging configured for production
- [ ] Error pages customized (403, 404, 500)
- [ ] session.cookie.secure=true (for HTTPS)
- [ ] ddl-auto=validate (never update/create in prod)

### 7.2 Deployment Steps

```bash
# 1. Build the application
mvn clean package -DskipTests -Pprod

# 2. Verify SQL Server connection
sqlcmd -S localhost -U gameon_user -P "password" -d GameOnDb -Q "SELECT 1"

# 3. Run the application (Flyway will auto-migrate)
java -jar target/gameon-1.0.0.jar \
  --spring.profiles.active=prod \
  --DB_PASSWORD=ProductionPassword123!

# 4. Verify application is healthy
curl http://localhost:8080/actuator/health

# 5. Verify login works
# Navigate to http://localhost:8080/login
# Login with test account

# 6. Verify database
# Check all 16 tables exist
# Check seed data present
# Check indexes created
```

### 7.3 Post-Deployment Verification

- [ ] Application starts without errors
- [ ] Health endpoint returns UP
- [ ] Login page renders
- [ ] Can login with test accounts
- [ ] Moderator redirect works
- [ ] Listings page loads (data from DB)
- [ ] Registration flow works end-to-end
- [ ] CSRF protection active (form submission works)
- [ ] 403 page shows for unauthorized access
- [ ] Logout works (session cleared)
- [ ] Database queries execute without errors

### 7.4 Performance Recommendations

| Area | Recommendation |
|------|----------------|
| Connection Pool | HikariCP with max 20 connections |
| Queries | Indexes on frequently queried columns (see V4 migration) |
| Fetch strategy | LAZY for collections, EAGER only for critical relationships |
| Pagination | Use Spring Data `Pageable` for all list endpoints |
| Caching | Consider Spring Cache for sport/position reference data |
| Static assets | CDN or nginx for CSS/JS/images |
| Session storage | Consider Redis for multi-instance deployments |

### 7.5 Monitoring

| Metric | Tool | Threshold |
|--------|------|-----------|
| Application health | /actuator/health | UP |
| Database connectivity | Health indicator | Connected |
| Response time | Metrics/logs | < 500ms |
| Error rate | Log aggregation | < 1% |
| Session count | Actuator metrics | < 1000 concurrent |
| DB connection pool | HikariCP metrics | < 80% utilization |

---

## 8. Database Initialization Script

For manual database setup (without Flyway), run:

```sql
-- Step 1: Create the database
CREATE DATABASE GameOnDb;
GO
USE GameOnDb;
GO

-- Step 2: Run V1__Create_Tables.sql (creates all 16 tables)
-- Step 3: Run V2__Seed_Data.sql (seeds sports, formats, positions)
-- Step 4: Run V3__Security_Data.sql (seeds user accounts)
-- Step 5: Run V4__Indexes.sql (creates performance indexes)
-- Step 6: Run V5__Constraints.sql (additional constraints, SPs, views)
```

---

## 9. Index Recommendations

| Table | Indexed Columns | Query Purpose |
|-------|----------------|---------------|
| users | username (UNIQUE) | Login lookup |
| users | user_role, is_active | Role-based queries |
| user_sport_profiles | sport_id, win_percentage DESC | Leaderboard |
| game_listings | creator_id, is_completed | BR1 check |
| game_listings | scheduled_date (filtered) | Browse active |
| game_joiners | game_listing_id, status | Pending requests |
| posts | created_at DESC | Feed ordering |
| notifications | recipient_id, is_read | Badge count |
| reports | status (filtered WHERE PENDING) | Moderator queue |

---

> **End of Security Documentation**  
> For functional requirements, refer to ARCHITECTURE.md and GameOn_Java_Development_Plan.md.
