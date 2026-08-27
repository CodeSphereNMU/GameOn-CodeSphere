# Implementation Plan: Database Schema Creation

## Overview

Create four T-SQL scripts that fully initialise the GameOnDb database on a local SQL Server instance. Each task produces a single `.sql` file in the `database/` directory. Scripts are written in dependency-aware order and are idempotent where applicable.

## Tasks

- [x] 1. Create schema.sql with all 16 CREATE TABLE statements
  - [x] 1.1 Create `database/schema.sql` with Wave 1 tables: [User], Sport, Position
    - Define [User] with all 10 columns, IDENTITY PK, UNIQUE on username and email, defaults for created_at and is_active
    - Define Sport with IDENTITY PK, UNIQUE on sport_name
    - Define Position with IDENTITY PK
    - _Requirements: 1.1, 1.2, 1.3, 1.5, 1.7, 1.8, 1.9_

  - [x] 1.2 Add Wave 2 table: SportFormat
    - FK to Sport(sport_id), all columns with correct types and NOT NULL constraints
    - _Requirements: 1.2, 1.3, 1.4, 1.8, 1.9_

  - [x] 1.3 Add Wave 3 tables: FormatPosition, UserSportProfile, GameListing, Post
    - FormatPosition: FKs to SportFormat and Position
    - UserSportProfile: FKs to User, Sport, Position (nullable)
    - GameListing: FKs to User, Sport, SportFormat; DEFAULT 'Open' on status; DEFAULT GETDATE() on created_at
    - Post: FK to User; NVARCHAR(MAX) for content; DEFAULT GETDATE() on created_at
    - _Requirements: 1.2, 1.3, 1.4, 1.7, 1.8, 1.9_

  - [x] 1.4 Add Wave 4 tables: GameJoiner, Session, Comment, [Like], [Follow], Notification, Report
    - GameJoiner: FKs to GameListing and User; DEFAULT 'Pending'
    - Session: FK to GameListing; DEFAULT 'Scheduled'
    - Comment: FKs to Post and User
    - [Like]: FKs to Post and User; UNIQUE constraint on (post_id, user_id)
    - [Follow]: FKs to User (follower_id, following_id); UNIQUE on (follower_id, following_id); CHECK constraint follower_id <> following_id
    - Notification: FK to User; DEFAULT 0 on is_read
    - Report: FKs to User (reporter_id, reported_user_id nullable), Post (reported_post_id nullable); DEFAULT 'Pending'
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9_

  - [x] 1.5 Add Wave 5 table: MatchResult
    - FKs to Session and User (recorded_by)
    - NVARCHAR(MAX) for result_data; DEFAULT GETDATE() on recorded_at
    - _Requirements: 1.2, 1.3, 1.4, 1.7, 1.8, 1.9_

- [x] 2. Create drop-all.sql with DROP TABLE IF EXISTS in reverse dependency order
  - [x] 2.1 Create `database/drop-all.sql` with all 16 DROP statements
    - Use `IF OBJECT_ID('dbo.TableName', 'U') IS NOT NULL DROP TABLE dbo.TableName;` pattern
    - Order: MatchResult → GameJoiner, Session, Comment, [Like], [Follow], Notification, Report → FormatPosition, UserSportProfile, GameListing, Post → SportFormat → [User], Sport, Position
    - _Requirements: 2.1, 2.2, 2.3_

- [x] 3. Create seed-data.sql with initial reference data
  - [x] 3.1 Create `database/seed-data.sql` with Sport seed data
    - Insert 5 sports (Football, Basketball, Cricket, Tennis, Rugby) with explicit IDs using SET IDENTITY_INSERT ON/OFF
    - Use IF NOT EXISTS guard before each insert
    - _Requirements: 3.1, 3.5_

  - [x] 3.2 Add SportFormat seed data
    - Insert format variants for each sport (5-a-side, 7-a-side, 11-a-side for Football, etc.) with explicit IDs
    - Use IF NOT EXISTS guard and IDENTITY_INSERT handling
    - _Requirements: 3.2, 3.5_

  - [x] 3.3 Add Position seed data
    - Insert common positions across all sports with explicit IDs
    - Use IF NOT EXISTS guard and IDENTITY_INSERT handling
    - _Requirements: 3.3, 3.5_

  - [x] 3.4 Add FormatPosition link records
    - Insert mappings linking each format to its valid positions
    - Use IF NOT EXISTS guard and IDENTITY_INSERT handling
    - _Requirements: 3.4, 3.5_

- [x] 4. Create indexes.sql with performance indexes
  - [x] 4.1 Create `database/indexes.sql` with all performance indexes
    - IX_User_Email, IX_User_Username
    - IX_GameListing_GameDate, IX_GameListing_Status, IX_GameListing_SportId
    - IX_GameJoiner_ListingId, IX_GameJoiner_UserId
    - IX_Post_UserId, IX_Post_CreatedAt
    - IX_Notification_UserId, IX_Notification_IsRead
    - Use `IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = '...')` guard before each CREATE INDEX
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_

- [x] 5. Final checkpoint
  - Ensure all four scripts exist in `database/` directory and can be executed in sequence (drop-all → schema → seed-data → indexes) on a fresh GameOnDb without errors. Ask the user if questions arise.
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

## Notes

- All scripts use T-SQL syntax targeting Microsoft SQL Server.
- Reserved words ([User], [Like], [Follow]) must always be bracket-escaped.
- FK constraint naming: `FK_{ChildTable}_{ParentTable}`
- Index naming: `IX_{TableName}_{ColumnName}`
- Seed data uses explicit IDENTITY_INSERT to keep FK references deterministic.
- No property-based testing applies — validation is done by executing the script sequence and verifying database state.

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "2.1"] },
    { "id": 1, "tasks": ["1.2"] },
    { "id": 2, "tasks": ["1.3"] },
    { "id": 3, "tasks": ["1.4"] },
    { "id": 4, "tasks": ["1.5", "3.1"] },
    { "id": 5, "tasks": ["3.2", "3.3"] },
    { "id": 6, "tasks": ["3.4", "4.1"] }
  ]
}
```
