# Requirements Document

## Introduction

This document defines the requirements for generating the complete set of SQL scripts (T-SQL) for the GameOn-CodeSphere database (`GameOnDb`) running on Microsoft SQL Server. The scripts cover schema creation, seed data, performance indexes, and a drop-all utility, enabling each developer to initialise a fully functional local database from scratch.

## Glossary

- **Schema_Script**: The SQL file (`schema.sql`) containing all CREATE TABLE statements for the GameOnDb database
- **Seed_Script**: The SQL file (`seed-data.sql`) containing INSERT statements for initial reference data
- **Index_Script**: The SQL file (`indexes.sql`) containing CREATE INDEX statements for performance optimisation
- **Drop_Script**: The SQL file (`drop-all.sql`) containing DROP TABLE statements for tearing down the database
- **GameOnDb**: The Microsoft SQL Server database instance used by the GameOn-CodeSphere application
- **Dependency_Order**: The sequence in which tables must be created or dropped to satisfy foreign key relationships

## Requirements

### Requirement 1: Schema Script Generation

**User Story:** As a developer, I want a single schema SQL script that creates all 16 tables with correct T-SQL DDL, so that I can initialise the full database structure on my local SQL Server instance.

#### Acceptance Criteria

1. THE Schema_Script SHALL create all 16 tables: [User], Sport, SportFormat, Position, FormatPosition, UserSportProfile, GameListing, GameJoiner, Session, MatchResult, Post, Comment, [Like], [Follow], Notification, Report
2. THE Schema_Script SHALL create tables in Dependency_Order so that referenced tables exist before referencing tables
3. WHEN a table has a primary key column, THE Schema_Script SHALL define the primary key as INT IDENTITY(1,1) with a PRIMARY KEY constraint
4. WHEN a column references another table, THE Schema_Script SHALL define a FOREIGN KEY constraint referencing the parent table's primary key
5. THE Schema_Script SHALL define UNIQUE constraints on User.username, User.email, Sport.sport_name, Like(post_id, user_id), and Follow(follower_id, following_id)
6. THE Schema_Script SHALL define a CHECK constraint on the Follow table ensuring follower_id is not equal to following_id
7. WHEN a column has a specified default value, THE Schema_Script SHALL apply the DEFAULT constraint using the documented value (GETDATE(), 'Open', 'Pending', 'Scheduled', 0, 1)
8. WHEN a column is specified as NOT NULL, THE Schema_Script SHALL include the NOT NULL constraint; WHEN a column is specified as nullable, THE Schema_Script SHALL allow NULL values
9. THE Schema_Script SHALL use correct T-SQL data types as documented: INT, VARCHAR(n), NVARCHAR(MAX), DATETIME, DATE, TIME, BIT

### Requirement 2: Drop-All Script Generation

**User Story:** As a developer, I want a drop-all script that removes all tables in the correct order, so that I can cleanly reset my local database during development.

#### Acceptance Criteria

1. THE Drop_Script SHALL drop all 16 tables from the GameOnDb database
2. THE Drop_Script SHALL drop tables in reverse Dependency_Order so that referencing tables are dropped before referenced tables
3. THE Drop_Script SHALL use conditional drop syntax (IF EXISTS or equivalent) to avoid errors when tables do not exist

### Requirement 3: Seed Data Script Generation

**User Story:** As a developer, I want a seed data script with initial reference data for sports, formats, and positions, so that the application has the baseline lookup data needed to function.

#### Acceptance Criteria

1. THE Seed_Script SHALL insert initial Sport records including Football, Basketball, Cricket, Tennis, and Rugby
2. THE Seed_Script SHALL insert SportFormat records for each sport (e.g., 5-a-side and 11-a-side for Football)
3. THE Seed_Script SHALL insert Position records covering common playing positions across all supported sports
4. THE Seed_Script SHALL insert FormatPosition records linking each format to its valid positions
5. WHEN a seed record conflicts with an existing record, THE Seed_Script SHALL handle the conflict gracefully without causing a runtime error

### Requirement 4: Index Script Generation

**User Story:** As a developer, I want a performance index script based on the documented indexing strategy, so that common query patterns perform efficiently.

#### Acceptance Criteria

1. THE Index_Script SHALL create indexes on User.email and User.username for login lookups
2. THE Index_Script SHALL create indexes on GameListing.game_date, GameListing.status, and GameListing.sport_id for browse filtering
3. THE Index_Script SHALL create indexes on GameJoiner.listing_id and GameJoiner.user_id for join lookups
4. THE Index_Script SHALL create indexes on Post.user_id and Post.created_at for feed queries
5. THE Index_Script SHALL create indexes on Notification.user_id and Notification.is_read for notification centre queries
6. WHEN an index already exists, THE Index_Script SHALL skip creation without causing a runtime error

### Requirement 5: Script Placement and Organisation

**User Story:** As a developer, I want the SQL scripts placed in the correct project directory with clear naming, so that the team can find and execute them consistently.

#### Acceptance Criteria

1. THE Schema_Script SHALL be located at `database/schema.sql`
2. THE Drop_Script SHALL be located at `database/drop-all.sql`
3. THE Seed_Script SHALL be located at `database/seed-data.sql`
4. THE Index_Script SHALL be located at `database/indexes.sql`
5. WHEN a developer runs the scripts in order (drop-all → schema → seed-data → indexes), THE GameOnDb database SHALL be in a fully initialised and usable state
