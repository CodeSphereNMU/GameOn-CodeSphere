-- Align users with the approved GameOn model and persist courtesy invitations.

-- ============================================================
-- USERS: user_id, username, password, type_of_user, account_status
-- ============================================================

IF COL_LENGTH('dbo.users', 'type_of_user') IS NULL
    ALTER TABLE users ADD type_of_user VARCHAR(20) NULL;
GO

UPDATE users
SET type_of_user = CASE
    WHEN type_of_user IS NOT NULL THEN type_of_user
    WHEN user_role IN ('ADMIN', 'MODERATOR') THEN 'ADMIN'
    ELSE 'USER'
END;
GO

IF COL_LENGTH('dbo.users', 'account_status') IS NULL
    ALTER TABLE users ADD account_status VARCHAR(20) NULL;
GO

UPDATE users
SET account_status = CASE
    WHEN account_status IS NOT NULL THEN account_status
    WHEN is_active = 0 THEN 'BANNED'
    ELSE 'ACTIVE'
END;
GO

-- Update database objects before removing the legacy is_active column they use.
CREATE OR ALTER PROCEDURE sp_GetLeaderboard
    @sportId BIGINT,
    @topN INT = 50
AS
BEGIN
    SET NOCOUNT ON;
    SELECT TOP (@topN)
        u.user_id, u.username, usp.sport_id, s.sport_name,
        usp.skill_level, usp.wins, usp.losses, usp.win_percentage
    FROM user_sport_profiles usp
    INNER JOIN users u ON u.user_id = usp.user_id
    INNER JOIN sports s ON s.sport_id = usp.sport_id
    WHERE usp.sport_id = @sportId
      AND u.account_status = 'ACTIVE'
      AND (usp.wins + usp.losses) > 0
    ORDER BY usp.win_percentage DESC, usp.wins DESC;
END;
GO

CREATE OR ALTER VIEW vw_user_stats AS
SELECT
    u.user_id,
    u.username,
    (SELECT COUNT(*) FROM follows f WHERE f.followed_user_id = u.user_id) AS follower_count,
    (SELECT COUNT(*) FROM follows f WHERE f.follower_user_id = u.user_id) AS following_count,
    (SELECT COUNT(*) FROM game_joiners gj
     WHERE gj.user_id = u.user_id AND gj.status IN ('ACCEPTED', 'LOCKED')) AS games_played,
    (SELECT COUNT(*) FROM posts p WHERE p.user_id = u.user_id) AS post_count
FROM users u
WHERE u.account_status = 'ACTIVE';
GO

-- Existing BCrypt values cannot be reversed. Restore the documented meeting logins
-- while converting the database to the approved plain-text password model.
UPDATE users
SET password = CASE
    WHEN username IN ('Admin', 'Moderator') THEN 'Admin123'
    ELSE 'Test123'
END
WHERE password LIKE '$2%';
GO

IF OBJECT_ID('dbo.CK_users_role', 'C') IS NOT NULL
    ALTER TABLE users DROP CONSTRAINT CK_users_role;
GO

IF EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.users') AND name = 'IX_users_role')
    DROP INDEX IX_users_role ON users;
IF EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.users') AND name = 'IX_users_active')
    DROP INDEX IX_users_active ON users;
GO

IF OBJECT_ID('dbo.trg_users_updated_at', 'TR') IS NOT NULL
    DROP TRIGGER dbo.trg_users_updated_at;
GO

DECLARE @constraintName SYSNAME;
DECLARE @sql NVARCHAR(500);

DECLARE column_cursor CURSOR LOCAL FAST_FORWARD FOR
SELECT dc.name
FROM sys.default_constraints dc
JOIN sys.columns c ON c.default_object_id = dc.object_id
JOIN sys.tables t ON t.object_id = c.object_id
WHERE t.object_id = OBJECT_ID('dbo.users')
  AND c.name IN ('user_role', 'is_active', 'email', 'created_at', 'updated_at', 'created_by', 'updated_by');

OPEN column_cursor;
FETCH NEXT FROM column_cursor INTO @constraintName;
WHILE @@FETCH_STATUS = 0
BEGIN
    SET @sql = N'ALTER TABLE dbo.users DROP CONSTRAINT ' + QUOTENAME(@constraintName);
    EXEC sp_executesql @sql;
    FETCH NEXT FROM column_cursor INTO @constraintName;
END;
CLOSE column_cursor;
DEALLOCATE column_cursor;
GO

IF COL_LENGTH('dbo.users', 'email') IS NOT NULL ALTER TABLE users DROP COLUMN email;
IF COL_LENGTH('dbo.users', 'user_role') IS NOT NULL ALTER TABLE users DROP COLUMN user_role;
IF COL_LENGTH('dbo.users', 'is_active') IS NOT NULL ALTER TABLE users DROP COLUMN is_active;
IF COL_LENGTH('dbo.users', 'created_at') IS NOT NULL ALTER TABLE users DROP COLUMN created_at;
IF COL_LENGTH('dbo.users', 'updated_at') IS NOT NULL ALTER TABLE users DROP COLUMN updated_at;
IF COL_LENGTH('dbo.users', 'created_by') IS NOT NULL ALTER TABLE users DROP COLUMN created_by;
IF COL_LENGTH('dbo.users', 'updated_by') IS NOT NULL ALTER TABLE users DROP COLUMN updated_by;
GO

ALTER TABLE users ALTER COLUMN type_of_user VARCHAR(20) NOT NULL;
ALTER TABLE users ALTER COLUMN account_status VARCHAR(20) NOT NULL;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.default_constraints dc
    JOIN sys.columns c ON c.default_object_id = dc.object_id
    WHERE c.object_id = OBJECT_ID('dbo.users') AND c.name = 'type_of_user'
)
    ALTER TABLE users ADD CONSTRAINT DF_users_type_of_user DEFAULT 'USER' FOR type_of_user;
IF NOT EXISTS (
    SELECT 1 FROM sys.default_constraints dc
    JOIN sys.columns c ON c.default_object_id = dc.object_id
    WHERE c.object_id = OBJECT_ID('dbo.users') AND c.name = 'account_status'
)
    ALTER TABLE users ADD CONSTRAINT DF_users_account_status DEFAULT 'ACTIVE' FOR account_status;
GO

IF OBJECT_ID('dbo.CK_users_type_of_user', 'C') IS NULL
    ALTER TABLE users ADD CONSTRAINT CK_users_type_of_user
        CHECK (type_of_user IN ('USER', 'ADMIN'));
IF OBJECT_ID('dbo.CK_users_account_status', 'C') IS NULL
    ALTER TABLE users ADD CONSTRAINT CK_users_account_status
        CHECK (account_status IN ('ACTIVE', 'SUSPENDED', 'BANNED'));
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.users') AND name = 'IX_users_type')
    CREATE NONCLUSTERED INDEX IX_users_type ON users(type_of_user);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.users') AND name = 'IX_users_account_status')
    CREATE NONCLUSTERED INDEX IX_users_account_status ON users(account_status);
GO

-- ============================================================
-- INVITATION: courtesy only; invitees still submit join requests
-- ============================================================

IF OBJECT_ID('dbo.invitation', 'U') IS NULL
BEGIN
    CREATE TABLE invitation (
        invitation_id BIGINT IDENTITY(1,1) NOT NULL,
        game_listing_id BIGINT NOT NULL,
        invitee_id BIGINT NOT NULL,
        status VARCHAR(20) NOT NULL CONSTRAINT DF_invitation_status DEFAULT 'PENDING',
        created_at DATETIME2 NOT NULL CONSTRAINT DF_invitation_created DEFAULT GETDATE(),
        CONSTRAINT PK_invitation PRIMARY KEY (invitation_id),
        CONSTRAINT UQ_invitation_listing_user UNIQUE (game_listing_id, invitee_id),
        CONSTRAINT FK_invitation_listing FOREIGN KEY (game_listing_id)
            REFERENCES game_listings(game_listing_id),
        CONSTRAINT FK_invitation_invitee FOREIGN KEY (invitee_id)
            REFERENCES users(user_id),
        CONSTRAINT CK_invitation_status CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'EXPIRED'))
    );
END;
GO
