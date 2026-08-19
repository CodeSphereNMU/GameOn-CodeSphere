-- ============================================================
-- GameOnDb - Drop All Tables Script
-- ============================================================
-- WARNING: This script is for DEVELOPMENT USE ONLY.
-- It will permanently remove all tables and their data from
-- the GameOnDb database. Do NOT run in production.
--
-- Tables are dropped in reverse dependency order to avoid
-- foreign key constraint violations.
-- ============================================================

USE GameOnDb;
GO

-- Wave 1: Most dependent (references Session)
IF OBJECT_ID('dbo.MatchResult', 'U') IS NOT NULL
    DROP TABLE dbo.MatchResult;

-- Wave 2: Tables referencing Wave 3+ tables
IF OBJECT_ID('dbo.GameJoiner', 'U') IS NOT NULL
    DROP TABLE dbo.GameJoiner;

IF OBJECT_ID('dbo.Session', 'U') IS NOT NULL
    DROP TABLE dbo.Session;

IF OBJECT_ID('dbo.Comment', 'U') IS NOT NULL
    DROP TABLE dbo.Comment;

IF OBJECT_ID('dbo.[Like]', 'U') IS NOT NULL
    DROP TABLE dbo.[Like];

IF OBJECT_ID('dbo.[Follow]', 'U') IS NOT NULL
    DROP TABLE dbo.[Follow];

IF OBJECT_ID('dbo.Notification', 'U') IS NOT NULL
    DROP TABLE dbo.Notification;

IF OBJECT_ID('dbo.Report', 'U') IS NOT NULL
    DROP TABLE dbo.Report;

-- Wave 3: Tables referencing Wave 4+ tables
IF OBJECT_ID('dbo.FormatPosition', 'U') IS NOT NULL
    DROP TABLE dbo.FormatPosition;

IF OBJECT_ID('dbo.UserSportProfile', 'U') IS NOT NULL
    DROP TABLE dbo.UserSportProfile;

IF OBJECT_ID('dbo.GameListing', 'U') IS NOT NULL
    DROP TABLE dbo.GameListing;

IF OBJECT_ID('dbo.Post', 'U') IS NOT NULL
    DROP TABLE dbo.Post;

-- Wave 4: References base tables
IF OBJECT_ID('dbo.SportFormat', 'U') IS NOT NULL
    DROP TABLE dbo.SportFormat;

-- Wave 5: Base tables (no foreign key dependencies)
IF OBJECT_ID('dbo.[User]', 'U') IS NOT NULL
    DROP TABLE dbo.[User];

IF OBJECT_ID('dbo.Sport', 'U') IS NOT NULL
    DROP TABLE dbo.Sport;

IF OBJECT_ID('dbo.Position', 'U') IS NOT NULL
    DROP TABLE dbo.Position;

PRINT 'All tables dropped successfully.';
GO
