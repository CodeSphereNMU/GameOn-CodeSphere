-- V3: Align schema with confirmed business rules.
-- Status: CREATED — pending review before application to GameOnDB.
--
-- Adds: duration_minutes, lifecycle status, end_time, invitation table, join_request table,
--        game_joiner improvements, match_result constraints, notification enhancements.
-- Drops: is_completed, alternate_format_position, match_result.winners.
--
-- Relational integrity strategy:
--   - game_joiner.format_id must equal game_listing.format_id (composite FK)
--   - game_joiner positions scoped to format via composite FK to format_position
--   - join_request positions scoped to format via composite FK to format_position
--   - join_request.invitation_id scoped to same listing+user via composite FK
--   - game_joiner.join_request_id scoped to same listing+user via composite FK

-- ============================================================
-- 1. sport_format: Add duration_minutes (nullable, no default)
-- ============================================================

IF COL_LENGTH('dbo.sport_format', 'duration_minutes') IS NULL
BEGIN
    ALTER TABLE [dbo].[sport_format] ADD [duration_minutes] INT NULL;
END;
GO

-- Explicitly set every seeded format (Game On session durations).
UPDATE sf SET sf.[duration_minutes] = 60
FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id
WHERE s.sport_name = 'Padel' AND sf.format_name = 'Doubles';
GO
UPDATE sf SET sf.[duration_minutes] = 60
FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id
WHERE s.sport_name = 'Tennis' AND sf.format_name = 'Singles';
GO
UPDATE sf SET sf.[duration_minutes] = 60
FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id
WHERE s.sport_name = 'Tennis' AND sf.format_name = 'Doubles';
GO
UPDATE sf SET sf.[duration_minutes] = 60
FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id
WHERE s.sport_name = 'Basketball' AND sf.format_name = '1v1';
GO
UPDATE sf SET sf.[duration_minutes] = 60
FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id
WHERE s.sport_name = 'Basketball' AND sf.format_name = '2v2';
GO
UPDATE sf SET sf.[duration_minutes] = 60
FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id
WHERE s.sport_name = 'Basketball' AND sf.format_name = '3v3';
GO
UPDATE sf SET sf.[duration_minutes] = 60
FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id
WHERE s.sport_name = 'Basketball' AND sf.format_name = '4v4';
GO
UPDATE sf SET sf.[duration_minutes] = 60
FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id
WHERE s.sport_name = 'Basketball' AND sf.format_name = '5v5';
GO
UPDATE sf SET sf.[duration_minutes] = 60
FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id
WHERE s.sport_name = 'Rugby' AND sf.format_name = '7s Contact';
GO
UPDATE sf SET sf.[duration_minutes] = 60
FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id
WHERE s.sport_name = 'Rugby' AND sf.format_name = '7s Touch';
GO
UPDATE sf SET sf.[duration_minutes] = 120
FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id
WHERE s.sport_name = 'Rugby' AND sf.format_name = '15s Contact';
GO
UPDATE sf SET sf.[duration_minutes] = 120
FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id
WHERE s.sport_name = 'Rugby' AND sf.format_name = '15s Touch';
GO
UPDATE sf SET sf.[duration_minutes] = 60
FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id
WHERE s.sport_name = 'Football' AND sf.format_name = '3v3';
GO
UPDATE sf SET sf.[duration_minutes] = 60
FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id
WHERE s.sport_name = 'Football' AND sf.format_name = '5v5';
GO
UPDATE sf SET sf.[duration_minutes] = 120
FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id
WHERE s.sport_name = 'Football' AND sf.format_name = '11v11';
GO

IF EXISTS (SELECT 1 FROM [dbo].[sport_format] WHERE [duration_minutes] IS NULL OR [duration_minutes] <= 0)
BEGIN
    RAISERROR('sport_format contains rows with NULL or non-positive duration_minutes. All formats must have an explicit positive duration.', 16, 1);
    RETURN;
END;
GO

ALTER TABLE [dbo].[sport_format] ALTER COLUMN [duration_minutes] INT NOT NULL;
GO

IF OBJECT_ID('dbo.CK_sport_format_duration_positive', 'C') IS NULL
    ALTER TABLE [dbo].[sport_format] ADD CONSTRAINT CK_sport_format_duration_positive CHECK ([duration_minutes] > 0);
GO

-- ============================================================
-- 2. game_listing: Add status, end_time; add UNIQUE for composite FK; drop is_completed
-- ============================================================

IF COL_LENGTH('dbo.game_listing', 'status') IS NULL
    ALTER TABLE [dbo].[game_listing] ADD [status] VARCHAR(50) NOT NULL CONSTRAINT DF_game_listing_status DEFAULT 'OPEN';
GO

IF OBJECT_ID('dbo.CK_game_listing_status', 'C') IS NULL
    ALTER TABLE [dbo].[game_listing] ADD CONSTRAINT CK_game_listing_status
        CHECK ([status] IN ('OPEN','CONFIRMED','CANCELLED_INSUFFICIENT_PLAYERS','CANCELLED_BY_CREATOR','COMPLETED'));
GO

IF COL_LENGTH('dbo.game_listing', 'end_time') IS NULL
    ALTER TABLE [dbo].[game_listing] ADD [end_time] DATETIME2(7) NULL;
GO

UPDATE [dbo].[game_listing] SET [status] = 'COMPLETED' WHERE [is_completed] = 1;
GO
UPDATE [dbo].[game_listing] SET [status] = 'OPEN' WHERE [is_completed] = 0;
GO

UPDATE gl SET gl.[end_time] = DATEADD(MINUTE, sf.[duration_minutes], gl.[date])
FROM [dbo].[game_listing] gl JOIN [dbo].[sport_format] sf ON gl.[format_id] = sf.[format_id]
WHERE gl.[end_time] IS NULL;
GO

ALTER TABLE [dbo].[game_listing] ALTER COLUMN [end_time] DATETIME2(7) NOT NULL;
GO

-- Drop is_completed
IF COL_LENGTH('dbo.game_listing', 'is_completed') IS NOT NULL
BEGIN
    DECLARE @dfName NVARCHAR(256);
    SELECT @dfName = dc.name FROM sys.default_constraints dc
    JOIN sys.columns c ON dc.parent_object_id = c.object_id AND dc.parent_column_id = c.column_id
    WHERE dc.parent_object_id = OBJECT_ID('dbo.game_listing') AND c.name = 'is_completed';
    IF @dfName IS NOT NULL EXEC('ALTER TABLE [dbo].[game_listing] DROP CONSTRAINT [' + @dfName + ']');
    ALTER TABLE [dbo].[game_listing] DROP COLUMN [is_completed];
END;
GO

-- UNIQUE key on (game_listing_id, format_id) to support composite FK from game_joiner/join_request
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UQ_game_listing_id_format' AND object_id = OBJECT_ID('dbo.game_listing'))
    CREATE UNIQUE NONCLUSTERED INDEX UQ_game_listing_id_format
    ON [dbo].[game_listing] ([game_listing_id], [format_id]);
GO

-- UNIQUE key on (game_listing_id, creator_id) — used by game_joiner composite FK for identity
-- Not needed: the PK game_listing_id is already unique, and we composite with user_id on game_joiner side.

-- ============================================================
-- 3. game_joiner: Tighten, validate, add constraints, composite FKs
-- ============================================================

IF EXISTS (SELECT 1 FROM [dbo].[game_joiner] WHERE [status] IS NULL)
BEGIN RAISERROR('game_joiner contains rows with NULL status.', 16, 1); RETURN; END;
GO
IF EXISTS (SELECT 1 FROM [dbo].[game_joiner] WHERE [team] IS NULL)
BEGIN RAISERROR('game_joiner contains rows with NULL team.', 16, 1); RETURN; END;
GO
IF EXISTS (SELECT 1 FROM [dbo].[game_joiner] WHERE [format_id] IS NULL)
BEGIN RAISERROR('game_joiner contains rows with NULL format_id.', 16, 1); RETURN; END;
GO

-- Fail if any game_joiner.format_id differs from its listing's format_id
IF EXISTS (
    SELECT 1 FROM [dbo].[game_joiner] gj
    JOIN [dbo].[game_listing] gl ON gj.[game_listing_id] = gl.[game_listing_id]
    WHERE gj.[format_id] <> gl.[format_id]
)
BEGIN RAISERROR('game_joiner contains rows where format_id differs from the listing format_id. Inspect manually.', 16, 1); RETURN; END;
GO

UPDATE [dbo].[game_joiner] SET [status] = UPPER([status]) WHERE [status] <> UPPER([status]);
GO

ALTER TABLE [dbo].[game_joiner] ALTER COLUMN [status] VARCHAR(20) NOT NULL;
GO
ALTER TABLE [dbo].[game_joiner] ALTER COLUMN [team] VARCHAR(10) NOT NULL;
GO
ALTER TABLE [dbo].[game_joiner] ALTER COLUMN [format_id] BIGINT NOT NULL;
GO

-- Add alternate_position_id
IF COL_LENGTH('dbo.game_joiner', 'alternate_position_id') IS NULL
    ALTER TABLE [dbo].[game_joiner] ADD [alternate_position_id] BIGINT NULL;
GO

-- Validate alternate_format_position before migration
IF EXISTS (SELECT 1 FROM [dbo].[game_joiner] WHERE [alternate_format_position] IS NOT NULL AND TRY_CAST([alternate_format_position] AS BIGINT) IS NULL)
BEGIN RAISERROR('game_joiner.alternate_format_position contains non-BIGINT values.', 16, 1); RETURN; END;
GO
IF EXISTS (SELECT 1 FROM [dbo].[game_joiner] gj WHERE gj.[alternate_format_position] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM [dbo].[position] p WHERE p.[position_id] = TRY_CAST(gj.[alternate_format_position] AS BIGINT)))
BEGIN RAISERROR('game_joiner.alternate_format_position references missing position.', 16, 1); RETURN; END;
GO
IF EXISTS (SELECT 1 FROM [dbo].[game_joiner] gj WHERE gj.[alternate_format_position] IS NOT NULL AND NOT EXISTS (SELECT 1 FROM [dbo].[format_position] fp WHERE fp.[format_id] = gj.[format_id] AND fp.[position_id] = TRY_CAST(gj.[alternate_format_position] AS BIGINT)))
BEGIN RAISERROR('game_joiner.alternate_format_position references position not in format.', 16, 1); RETURN; END;
GO
IF EXISTS (SELECT 1 FROM [dbo].[game_joiner] WHERE [alternate_format_position] IS NOT NULL AND [position_id] IS NOT NULL AND [position_id] = TRY_CAST([alternate_format_position] AS BIGINT))
BEGIN RAISERROR('game_joiner.alternate_format_position duplicates position_id.', 16, 1); RETURN; END;
GO

UPDATE [dbo].[game_joiner] SET [alternate_position_id] = TRY_CAST([alternate_format_position] AS BIGINT) WHERE [alternate_format_position] IS NOT NULL;
GO

IF COL_LENGTH('dbo.game_joiner', 'alternate_format_position') IS NOT NULL
    ALTER TABLE [dbo].[game_joiner] DROP COLUMN [alternate_format_position];
GO

-- CHECK constraints
IF OBJECT_ID('dbo.CK_game_joiner_status', 'C') IS NULL
    ALTER TABLE [dbo].[game_joiner] ADD CONSTRAINT CK_game_joiner_status CHECK ([status] IN ('ACCEPTED','WITHDRAWN'));
GO
IF OBJECT_ID('dbo.CK_game_joiner_team', 'C') IS NULL
    ALTER TABLE [dbo].[game_joiner] ADD CONSTRAINT CK_game_joiner_team CHECK ([team] IN ('A','B'));
GO
IF OBJECT_ID('dbo.CK_game_joiner_alt_requires_primary', 'C') IS NULL
    ALTER TABLE [dbo].[game_joiner] ADD CONSTRAINT CK_game_joiner_alt_requires_primary CHECK ([alternate_position_id] IS NULL OR [position_id] IS NOT NULL);
GO
IF OBJECT_ID('dbo.CK_game_joiner_positions_differ', 'C') IS NULL
    ALTER TABLE [dbo].[game_joiner] ADD CONSTRAINT CK_game_joiner_positions_differ CHECK ([alternate_position_id] IS NULL OR [position_id] <> [alternate_position_id]);
GO

-- Drop old simple FK on (format_id, position_id) if it exists (Hibernate-generated name)
IF OBJECT_ID('dbo.FKflc26oacgqui82woop4gcr4me', 'F') IS NOT NULL
    ALTER TABLE [dbo].[game_joiner] DROP CONSTRAINT [FKflc26oacgqui82woop4gcr4me];
GO

-- Composite FK: game_joiner.(game_listing_id, format_id) → game_listing.(game_listing_id, format_id)
-- Ensures format_id always matches the listing's format_id
IF OBJECT_ID('dbo.FK_game_joiner_listing_format', 'F') IS NULL
    ALTER TABLE [dbo].[game_joiner] ADD CONSTRAINT FK_game_joiner_listing_format
        FOREIGN KEY ([game_listing_id], [format_id]) REFERENCES [dbo].[game_listing] ([game_listing_id], [format_id]);
GO

-- Composite FK: game_joiner.(format_id, position_id) → format_position.(format_id, position_id)
-- Ensures primary position belongs to the format (retained from original design)
IF OBJECT_ID('dbo.FK_game_joiner_format_position', 'F') IS NULL
    ALTER TABLE [dbo].[game_joiner] ADD CONSTRAINT FK_game_joiner_format_position
        FOREIGN KEY ([format_id], [position_id]) REFERENCES [dbo].[format_position] ([format_id], [position_id]);
GO

-- Composite FK: game_joiner.(format_id, alternate_position_id) → format_position.(format_id, position_id)
-- Ensures alternate position also belongs to the format
IF OBJECT_ID('dbo.FK_game_joiner_format_alt_position', 'F') IS NULL
    ALTER TABLE [dbo].[game_joiner] ADD CONSTRAINT FK_game_joiner_format_alt_position
        FOREIGN KEY ([format_id], [alternate_position_id]) REFERENCES [dbo].[format_position] ([format_id], [position_id]);
GO

-- ============================================================
-- 4. invitation table
-- ============================================================

IF OBJECT_ID('dbo.invitation', 'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[invitation] (
        [invitation_id] BIGINT IDENTITY(1,1) NOT NULL,
        [game_listing_id] BIGINT NOT NULL,
        [invitee_id] BIGINT NOT NULL,
        [status] VARCHAR(20) NOT NULL CONSTRAINT DF_invitation_status DEFAULT 'PENDING',
        [created_at] DATETIME2(7) NOT NULL CONSTRAINT DF_invitation_created DEFAULT GETDATE(),
    CONSTRAINT PK_invitation PRIMARY KEY CLUSTERED ([invitation_id]),
    CONSTRAINT FK_invitation_listing FOREIGN KEY ([game_listing_id]) REFERENCES [dbo].[game_listing] ([game_listing_id]),
    CONSTRAINT FK_invitation_invitee FOREIGN KEY ([invitee_id]) REFERENCES [dbo].[users] ([user_id]),
    CONSTRAINT CK_invitation_status CHECK ([status] IN ('PENDING','ACCEPTED','DECLINED','EXPIRED')),
    CONSTRAINT UQ_invitation_listing_user UNIQUE ([game_listing_id], [invitee_id])
    );
END;
GO

-- Composite unique key for identity FK from join_request: (invitation_id, game_listing_id, invitee_id)
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UQ_invitation_id_listing_invitee' AND object_id = OBJECT_ID('dbo.invitation'))
    CREATE UNIQUE NONCLUSTERED INDEX UQ_invitation_id_listing_invitee
    ON [dbo].[invitation] ([invitation_id], [game_listing_id], [invitee_id]);
GO

-- ============================================================
-- 5. join_request table
-- ============================================================

IF OBJECT_ID('dbo.join_request', 'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[join_request] (
        [join_request_id] BIGINT IDENTITY(1,1) NOT NULL,
        [game_listing_id] BIGINT NOT NULL,
        [user_id] BIGINT NOT NULL,
        [format_id] BIGINT NOT NULL,
        [team] VARCHAR(10) NOT NULL,
        [position_id] BIGINT NULL,
        [alternate_position_id] BIGINT NULL,
        [invitation_id] BIGINT NULL,
        [status] VARCHAR(20) NOT NULL CONSTRAINT DF_join_request_status DEFAULT 'PENDING',
        [created_at] DATETIME2(7) NOT NULL CONSTRAINT DF_join_request_created DEFAULT GETDATE(),
        [updated_at] DATETIME2(7) NOT NULL CONSTRAINT DF_join_request_updated DEFAULT GETDATE(),
    CONSTRAINT PK_join_request PRIMARY KEY CLUSTERED ([join_request_id]),
    CONSTRAINT FK_join_request_user FOREIGN KEY ([user_id]) REFERENCES [dbo].[users] ([user_id]),
    CONSTRAINT CK_join_request_status CHECK ([status] IN ('PENDING','ACCEPTED','REJECTED','WITHDRAWN','EXPIRED')),
    CONSTRAINT CK_join_request_team CHECK ([team] IN ('A','B')),
    CONSTRAINT CK_join_request_alt_requires_primary CHECK ([alternate_position_id] IS NULL OR [position_id] IS NOT NULL),
    CONSTRAINT CK_join_request_positions_differ CHECK ([alternate_position_id] IS NULL OR [position_id] <> [alternate_position_id]),
    -- Composite FK: format_id matches listing's format_id
    CONSTRAINT FK_join_request_listing_format FOREIGN KEY ([game_listing_id], [format_id]) REFERENCES [dbo].[game_listing] ([game_listing_id], [format_id]),
    -- Composite FK: position scoped to format
    CONSTRAINT FK_join_request_format_position FOREIGN KEY ([format_id], [position_id]) REFERENCES [dbo].[format_position] ([format_id], [position_id]),
    -- Composite FK: alternate position scoped to format
    CONSTRAINT FK_join_request_format_alt_position FOREIGN KEY ([format_id], [alternate_position_id]) REFERENCES [dbo].[format_position] ([format_id], [position_id]),
    -- Composite FK: invitation_id references same listing and user
    CONSTRAINT FK_join_request_invitation_identity FOREIGN KEY ([invitation_id], [game_listing_id], [user_id]) REFERENCES [dbo].[invitation] ([invitation_id], [game_listing_id], [invitee_id])
    );
END;
GO

-- Filtered unique index: one PENDING per user per listing
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_join_request_one_pending' AND object_id = OBJECT_ID('dbo.join_request'))
    CREATE UNIQUE NONCLUSTERED INDEX UX_join_request_one_pending
    ON [dbo].[join_request] ([game_listing_id], [user_id]) WHERE [status] = 'PENDING';
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_join_request_listing_status' AND object_id = OBJECT_ID('dbo.join_request'))
    CREATE NONCLUSTERED INDEX IX_join_request_listing_status
    ON [dbo].[join_request] ([game_listing_id], [status]) INCLUDE ([user_id], [invitation_id], [created_at]);
GO

-- Composite unique key for identity FK from game_joiner: (join_request_id, game_listing_id, user_id)
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UQ_join_request_id_listing_user' AND object_id = OBJECT_ID('dbo.join_request'))
    CREATE UNIQUE NONCLUSTERED INDEX UQ_join_request_id_listing_user
    ON [dbo].[join_request] ([join_request_id], [game_listing_id], [user_id]);
GO

-- ============================================================
-- 6. game_joiner: Add join_request_id with composite FK for identity
-- ============================================================

IF COL_LENGTH('dbo.game_joiner', 'join_request_id') IS NULL
    ALTER TABLE [dbo].[game_joiner] ADD [join_request_id] BIGINT NULL;
GO

-- Composite FK: join_request_id references same listing and user
IF OBJECT_ID('dbo.FK_game_joiner_join_request_identity', 'F') IS NULL
    ALTER TABLE [dbo].[game_joiner] ADD CONSTRAINT FK_game_joiner_join_request_identity
        FOREIGN KEY ([join_request_id], [game_listing_id], [user_id])
        REFERENCES [dbo].[join_request] ([join_request_id], [game_listing_id], [user_id]);
GO

-- ============================================================
-- 7. match_result: Drop winners, add score constraints
-- ============================================================

IF EXISTS (SELECT 1 FROM [dbo].[match_result] WHERE [teamascore] IS NULL OR [teambscore] IS NULL)
BEGIN RAISERROR('match_result contains NULL scores. Inspect and fix manually before applying V3.', 16, 1); RETURN; END;
GO

IF COL_LENGTH('dbo.match_result', 'winners') IS NOT NULL
    ALTER TABLE [dbo].[match_result] DROP COLUMN [winners];
GO

ALTER TABLE [dbo].[match_result] ALTER COLUMN [teamascore] INT NOT NULL;
GO
ALTER TABLE [dbo].[match_result] ALTER COLUMN [teambscore] INT NOT NULL;
GO

IF OBJECT_ID('dbo.CK_match_result_teamascore', 'C') IS NULL
    ALTER TABLE [dbo].[match_result] ADD CONSTRAINT CK_match_result_teamascore CHECK ([teamascore] >= 0);
GO
IF OBJECT_ID('dbo.CK_match_result_teambscore', 'C') IS NULL
    ALTER TABLE [dbo].[match_result] ADD CONSTRAINT CK_match_result_teambscore CHECK ([teambscore] >= 0);
GO

-- ============================================================
-- 8. notification enhancements
-- ============================================================

IF COL_LENGTH('dbo.notification', 'game_listing_id') IS NULL
    ALTER TABLE [dbo].[notification] ADD [game_listing_id] BIGINT NULL;
GO
IF OBJECT_ID('dbo.FK_notification_listing', 'F') IS NULL
    ALTER TABLE [dbo].[notification] ADD CONSTRAINT FK_notification_listing
        FOREIGN KEY ([game_listing_id]) REFERENCES [dbo].[game_listing] ([game_listing_id]);
GO
IF COL_LENGTH('dbo.notification', 'created_at') IS NULL
    ALTER TABLE [dbo].[notification] ADD [created_at] DATETIME2(7) NOT NULL CONSTRAINT DF_notification_created DEFAULT GETDATE();
GO

-- ============================================================
-- 9. Supporting indexes
-- ============================================================

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_game_listing_scheduling' AND object_id = OBJECT_ID('dbo.game_listing'))
    CREATE NONCLUSTERED INDEX IX_game_listing_scheduling
    ON [dbo].[game_listing] ([status]) INCLUDE ([game_listing_id], [date], [end_time], [format_id], [creator_id]);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_game_joiner_user_status' AND object_id = OBJECT_ID('dbo.game_joiner'))
    CREATE NONCLUSTERED INDEX IX_game_joiner_user_status
    ON [dbo].[game_joiner] ([user_id], [status]) INCLUDE ([game_listing_id]);
GO
