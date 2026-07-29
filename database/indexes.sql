-- ============================================================
-- GameOnDb Performance Indexes
-- Creates non-clustered indexes on columns commonly used in
-- WHERE, JOIN, and ORDER BY clauses to improve query performance.
-- Each index is guarded with IF NOT EXISTS to allow safe re-runs.
-- ============================================================

USE GameOnDb;
GO

-- 1. IX_User_Email — speeds up login/email lookups
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_User_Email' AND object_id = OBJECT_ID('dbo.[User]'))
    CREATE INDEX IX_User_Email ON [User](email);
GO

-- 2. IX_User_Username — speeds up username searches and uniqueness checks
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_User_Username' AND object_id = OBJECT_ID('dbo.[User]'))
    CREATE INDEX IX_User_Username ON [User](username);
GO

-- 3. IX_GameListing_GameDate — speeds up date-based browse filtering
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_GameListing_GameDate' AND object_id = OBJECT_ID('dbo.GameListing'))
    CREATE INDEX IX_GameListing_GameDate ON GameListing(game_date);
GO

-- 4. IX_GameListing_Status — speeds up status-based filtering (Open, Full, etc.)
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_GameListing_Status' AND object_id = OBJECT_ID('dbo.GameListing'))
    CREATE INDEX IX_GameListing_Status ON GameListing(status);
GO

-- 5. IX_GameListing_SportId — speeds up sport-based browse filtering
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_GameListing_SportId' AND object_id = OBJECT_ID('dbo.GameListing'))
    CREATE INDEX IX_GameListing_SportId ON GameListing(sport_id);
GO

-- 6. IX_GameJoiner_ListingId — speeds up lookups for players in a listing
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_GameJoiner_ListingId' AND object_id = OBJECT_ID('dbo.GameJoiner'))
    CREATE INDEX IX_GameJoiner_ListingId ON GameJoiner(listing_id);
GO

-- 7. IX_GameJoiner_UserId — speeds up lookups for games a user has joined
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_GameJoiner_UserId' AND object_id = OBJECT_ID('dbo.GameJoiner'))
    CREATE INDEX IX_GameJoiner_UserId ON GameJoiner(user_id);
GO

-- 8. IX_Post_UserId — speeds up feed queries filtered by author
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Post_UserId' AND object_id = OBJECT_ID('dbo.Post'))
    CREATE INDEX IX_Post_UserId ON Post(user_id);
GO

-- 9. IX_Post_CreatedAt — speeds up chronological feed ordering
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Post_CreatedAt' AND object_id = OBJECT_ID('dbo.Post'))
    CREATE INDEX IX_Post_CreatedAt ON Post(created_at);
GO

-- 10. IX_Notification_UserId — speeds up notification centre queries per user
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Notification_UserId' AND object_id = OBJECT_ID('dbo.Notification'))
    CREATE INDEX IX_Notification_UserId ON Notification(user_id);
GO

-- 11. IX_Notification_IsRead — speeds up unread notification filtering
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Notification_IsRead' AND object_id = OBJECT_ID('dbo.Notification'))
    CREATE INDEX IX_Notification_IsRead ON Notification(is_read);
GO
