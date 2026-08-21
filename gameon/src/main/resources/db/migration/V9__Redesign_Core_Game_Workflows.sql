-- Redesign the game workflow while preserving existing rows where possible.
-- This is a forward-only SQL Server migration from the V8 schema.

-- Views must be replaced after columns such as is_completed are removed.
IF OBJECT_ID('dbo.vw_active_game_listings', 'V') IS NOT NULL DROP VIEW dbo.vw_active_game_listings;
IF OBJECT_ID('dbo.vw_user_stats', 'V') IS NOT NULL DROP VIEW dbo.vw_user_stats;
GO

-- ============================================================
-- GAME LISTINGS: explicit lifecycle and minute-accurate duration
-- ============================================================
ALTER TABLE game_listings ADD listing_status VARCHAR(40) NULL;
ALTER TABLE game_listings ADD duration_minutes INT NULL;
GO

UPDATE gl
SET listing_status = CASE WHEN gl.is_completed = 1 THEN 'COMPLETED' ELSE 'OPEN' END,
    duration_minutes = sf.duration_minutes
FROM game_listings gl
JOIN sport_formats sf ON sf.format_id = gl.format_id;
GO

IF EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.game_listings') AND name = 'IX_game_listings_active')
    DROP INDEX IX_game_listings_active ON game_listings;
IF EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.game_listings') AND name = 'IX_game_listings_creator_active')
    DROP INDEX IX_game_listings_creator_active ON game_listings;
GO

IF OBJECT_ID('dbo.CK_game_listings_session_duration', 'C') IS NOT NULL
    ALTER TABLE game_listings DROP CONSTRAINT CK_game_listings_session_duration;
IF OBJECT_ID('dbo.DF_game_listings_session_duration', 'D') IS NOT NULL
    ALTER TABLE game_listings DROP CONSTRAINT DF_game_listings_session_duration;
GO

DECLARE @listingDefault SYSNAME;
DECLARE @listingDefaultSql NVARCHAR(500);
SELECT @listingDefault = dc.name
FROM sys.default_constraints dc
JOIN sys.columns c ON c.default_object_id = dc.object_id
WHERE c.object_id = OBJECT_ID('dbo.game_listings') AND c.name = 'is_completed';
IF @listingDefault IS NOT NULL
BEGIN
    SET @listingDefaultSql = N'ALTER TABLE dbo.game_listings DROP CONSTRAINT ' + QUOTENAME(@listingDefault);
    EXEC sp_executesql @listingDefaultSql;
END;
GO

ALTER TABLE game_listings DROP COLUMN is_completed;
ALTER TABLE game_listings DROP COLUMN session_duration;
ALTER TABLE game_listings ALTER COLUMN listing_status VARCHAR(40) NOT NULL;
ALTER TABLE game_listings ALTER COLUMN duration_minutes INT NOT NULL;
GO

ALTER TABLE game_listings ADD CONSTRAINT DF_game_listings_status DEFAULT 'OPEN' FOR listing_status;
ALTER TABLE game_listings ADD CONSTRAINT CK_game_listings_status CHECK (listing_status IN (
    'OPEN', 'CONFIRMED', 'CANCELLED_INSUFFICIENT_PLAYERS', 'CANCELLED_BY_CREATOR', 'COMPLETED'
));
ALTER TABLE game_listings ADD CONSTRAINT CK_game_listings_duration_minutes
    CHECK (duration_minutes BETWEEN 1 AND 480);
GO

CREATE NONCLUSTERED INDEX IX_game_listings_status_date
    ON game_listings(listing_status, scheduled_date);
CREATE NONCLUSTERED INDEX IX_game_listings_creator_status
    ON game_listings(creator_id, listing_status);
GO

-- Capacity belongs to a format, not the sport itself.
IF OBJECT_ID('dbo.CK_sports_players', 'C') IS NOT NULL
    ALTER TABLE sports DROP CONSTRAINT CK_sports_players;
GO

DECLARE @sportsDefault SYSNAME;
DECLARE @sportsDefaultSql NVARCHAR(500);
SELECT @sportsDefault = dc.name
FROM sys.default_constraints dc
JOIN sys.columns c ON c.default_object_id = dc.object_id
WHERE c.object_id = OBJECT_ID('dbo.sports') AND c.name = 'no_players';
IF @sportsDefault IS NOT NULL
BEGIN
    SET @sportsDefaultSql = N'ALTER TABLE dbo.sports DROP CONSTRAINT ' + QUOTENAME(@sportsDefault);
    EXEC sp_executesql @sportsDefaultSql;
END;
GO

ALTER TABLE sports DROP COLUMN no_players;
GO

-- ============================================================
-- JOIN REQUESTS: durable request history
-- ============================================================
CREATE TABLE join_requests (
    join_request_id       BIGINT IDENTITY(1,1) NOT NULL,
    game_listing_id       BIGINT NOT NULL,
    user_id               BIGINT NOT NULL,
    format_id             BIGINT NOT NULL,
    team                  CHAR(1) NOT NULL,
    primary_position_id   BIGINT NULL,
    alternate_position_id BIGINT NULL,
    invitation_id         BIGINT NULL,
    status                VARCHAR(20) NOT NULL CONSTRAINT DF_join_requests_status DEFAULT 'PENDING',
    created_at            DATETIME2 NOT NULL CONSTRAINT DF_join_requests_created DEFAULT GETDATE(),
    updated_at            DATETIME2 NOT NULL CONSTRAINT DF_join_requests_updated DEFAULT GETDATE(),

    CONSTRAINT PK_join_requests PRIMARY KEY (join_request_id),
    CONSTRAINT FK_join_requests_listing FOREIGN KEY (game_listing_id) REFERENCES game_listings(game_listing_id),
    CONSTRAINT FK_join_requests_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT FK_join_requests_format FOREIGN KEY (format_id) REFERENCES sport_formats(format_id),
    CONSTRAINT FK_join_requests_invitation FOREIGN KEY (invitation_id) REFERENCES invitation(invitation_id),
    CONSTRAINT FK_join_requests_primary_position FOREIGN KEY (primary_position_id) REFERENCES positions(position_id),
    CONSTRAINT FK_join_requests_alternate_position FOREIGN KEY (alternate_position_id) REFERENCES positions(position_id),
    CONSTRAINT CK_join_requests_team CHECK (team IN ('A', 'B')),
    CONSTRAINT CK_join_requests_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'WITHDRAWN', 'EXPIRED'))
);
GO

INSERT INTO join_requests (
    game_listing_id, user_id, format_id, team,
    primary_position_id, alternate_position_id, status, created_at, updated_at
)
SELECT
    gj.game_listing_id,
    gj.user_id,
    gl.format_id,
    gj.team,
    gj.format_position_id,
    gj.alt_format_position_id,
    CASE gj.status
        WHEN 'PENDING' THEN 'PENDING'
        WHEN 'REJECTED' THEN 'REJECTED'
        WHEN 'LEFT' THEN 'WITHDRAWN'
        ELSE 'ACCEPTED'
    END,
    gj.created_at,
    gj.updated_at
FROM game_joiners gj
JOIN game_listings gl ON gl.game_listing_id = gj.game_listing_id
WHERE gj.user_id <> gl.creator_id;
GO

ALTER TABLE game_joiners ADD join_request_id BIGINT NULL;
ALTER TABLE game_joiners ADD format_id BIGINT NULL;
ALTER TABLE game_joiners ADD primary_position_id BIGINT NULL;
ALTER TABLE game_joiners ADD alternate_position_id BIGINT NULL;
GO

UPDATE gj
SET format_id = gl.format_id,
    primary_position_id = gj.format_position_id,
    alternate_position_id = gj.alt_format_position_id,
    join_request_id = jr.join_request_id
FROM game_joiners gj
JOIN game_listings gl ON gl.game_listing_id = gj.game_listing_id
OUTER APPLY (
    SELECT TOP (1) request.join_request_id
    FROM join_requests request
    WHERE request.game_listing_id = gj.game_listing_id
      AND request.user_id = gj.user_id
    ORDER BY request.created_at DESC, request.join_request_id DESC
) jr;
GO

DELETE FROM game_joiners WHERE status IN ('PENDING', 'REJECTED');
GO

IF OBJECT_ID('dbo.CK_game_joiners_status', 'C') IS NOT NULL
    ALTER TABLE game_joiners DROP CONSTRAINT CK_game_joiners_status;
GO

DECLARE @joinerStatusDefault SYSNAME;
DECLARE @joinerStatusDefaultSql NVARCHAR(500);
SELECT @joinerStatusDefault = dc.name
FROM sys.default_constraints dc
JOIN sys.columns c ON c.default_object_id = dc.object_id
WHERE c.object_id = OBJECT_ID('dbo.game_joiners') AND c.name = 'status';
IF @joinerStatusDefault IS NOT NULL
BEGIN
    SET @joinerStatusDefaultSql = N'ALTER TABLE dbo.game_joiners DROP CONSTRAINT ' + QUOTENAME(@joinerStatusDefault);
    EXEC sp_executesql @joinerStatusDefaultSql;
END;
GO

ALTER TABLE game_joiners DROP COLUMN format_position_id;
ALTER TABLE game_joiners DROP COLUMN alt_format_position_id;
ALTER TABLE game_joiners ALTER COLUMN format_id BIGINT NOT NULL;
GO

ALTER TABLE game_joiners ADD CONSTRAINT FK_game_joiners_request
    FOREIGN KEY (join_request_id) REFERENCES join_requests(join_request_id);
ALTER TABLE game_joiners ADD CONSTRAINT FK_game_joiners_format
    FOREIGN KEY (format_id) REFERENCES sport_formats(format_id);
ALTER TABLE game_joiners ADD CONSTRAINT FK_game_joiners_primary_position
    FOREIGN KEY (primary_position_id) REFERENCES positions(position_id);
ALTER TABLE game_joiners ADD CONSTRAINT FK_game_joiners_alternate_position
    FOREIGN KEY (alternate_position_id) REFERENCES positions(position_id);
ALTER TABLE game_joiners ADD CONSTRAINT CK_game_joiners_status
    CHECK (status IN ('ACCEPTED', 'LOCKED', 'LEFT'));
ALTER TABLE game_joiners ADD CONSTRAINT DF_game_joiners_status DEFAULT 'ACCEPTED' FOR status;
GO

CREATE NONCLUSTERED INDEX IX_join_requests_listing_status
    ON join_requests(game_listing_id, status, created_at);
CREATE NONCLUSTERED INDEX IX_join_requests_user_status
    ON join_requests(user_id, status, created_at DESC);
CREATE UNIQUE NONCLUSTERED INDEX UX_join_requests_one_pending
    ON join_requests(game_listing_id, user_id) WHERE status = 'PENDING';
CREATE UNIQUE NONCLUSTERED INDEX UX_game_joiners_one_participant_per_request
    ON game_joiners(join_request_id) WHERE join_request_id IS NOT NULL;
GO

-- A confirmed listing is the session; a second one-to-one table is unnecessary.
DROP TABLE sessions;
GO

-- ============================================================
-- INVITATIONS, RESULTS, AND NOTIFICATIONS
-- ============================================================
ALTER TABLE posts ADD removed_at DATETIME2 NULL;
ALTER TABLE posts ADD removed_by_user_id BIGINT NULL;
ALTER TABLE posts ADD CONSTRAINT FK_posts_removed_by
    FOREIGN KEY (removed_by_user_id) REFERENCES users(user_id);
CREATE NONCLUSTERED INDEX IX_posts_active_created
    ON posts(removed_at, created_at DESC);
GO

ALTER TABLE invitation ADD updated_at DATETIME2 NULL;
GO
IF OBJECT_ID('dbo.CK_invitation_status', 'C') IS NOT NULL
    ALTER TABLE invitation DROP CONSTRAINT CK_invitation_status;
GO
UPDATE invitation
SET status = CASE WHEN status = 'ACCEPTED' THEN 'USED' ELSE status END,
    updated_at = created_at;
ALTER TABLE invitation ALTER COLUMN updated_at DATETIME2 NOT NULL;
GO
ALTER TABLE invitation ADD CONSTRAINT DF_invitation_updated DEFAULT GETDATE() FOR updated_at;
ALTER TABLE invitation ADD CONSTRAINT CK_invitation_status
    CHECK (status IN ('PENDING', 'USED', 'DECLINED', 'EXPIRED'));
GO

IF OBJECT_ID('dbo.CK_match_results_winners', 'C') IS NOT NULL
    ALTER TABLE match_results DROP CONSTRAINT CK_match_results_winners;
ALTER TABLE match_results DROP COLUMN winners;
ALTER TABLE match_results DROP COLUMN created_by;
ALTER TABLE match_results DROP COLUMN updated_by;
GO

ALTER TABLE notifications ADD actor_user_id BIGINT NULL;
ALTER TABLE notifications ADD game_listing_id BIGINT NULL;
ALTER TABLE notifications ADD join_request_id BIGINT NULL;
ALTER TABLE notifications ADD match_result_id BIGINT NULL;
ALTER TABLE notifications ADD read_at DATETIME2 NULL;
GO
IF OBJECT_ID('dbo.CK_notifications_type', 'C') IS NOT NULL
    ALTER TABLE notifications DROP CONSTRAINT CK_notifications_type;
GO
ALTER TABLE notifications ALTER COLUMN notification_type VARCHAR(50) NOT NULL;
GO
UPDATE notifications
SET notification_type = 'LISTING_CONFIRMED'
WHERE notification_type = 'GAME_REMINDER';
UPDATE notifications SET read_at = updated_at WHERE is_read = 1;
GO
DECLARE @notificationUpdatedDefault SYSNAME;
DECLARE @notificationUpdatedDefaultSql NVARCHAR(500);
SELECT @notificationUpdatedDefault = dc.name
FROM sys.default_constraints dc
JOIN sys.columns c ON c.default_object_id = dc.object_id
WHERE c.object_id = OBJECT_ID('dbo.notifications') AND c.name = 'updated_at';
IF @notificationUpdatedDefault IS NOT NULL
BEGIN
    SET @notificationUpdatedDefaultSql = N'ALTER TABLE dbo.notifications DROP CONSTRAINT ' + QUOTENAME(@notificationUpdatedDefault);
    EXEC sp_executesql @notificationUpdatedDefaultSql;
END;
GO
ALTER TABLE notifications DROP COLUMN updated_at;
ALTER TABLE notifications ADD CONSTRAINT FK_notifications_actor
    FOREIGN KEY (actor_user_id) REFERENCES users(user_id);
ALTER TABLE notifications ADD CONSTRAINT FK_notifications_listing
    FOREIGN KEY (game_listing_id) REFERENCES game_listings(game_listing_id);
ALTER TABLE notifications ADD CONSTRAINT FK_notifications_join_request
    FOREIGN KEY (join_request_id) REFERENCES join_requests(join_request_id);
ALTER TABLE notifications ADD CONSTRAINT FK_notifications_match_result
    FOREIGN KEY (match_result_id) REFERENCES match_results(match_result_id);
ALTER TABLE notifications ADD CONSTRAINT CK_notifications_type CHECK (notification_type IN (
    'FOLLOW_NEW', 'JOIN_REQUEST_RECEIVED', 'JOIN_ACCEPTED', 'JOIN_REJECTED', 'JOIN_WITHDRAWN',
    'LISTING_CONFIRMED', 'LISTING_CANCELLED_INSUFFICIENT_PLAYERS',
    'MATCH_RESULT_POSTED', 'MATCH_RESULT_UPDATED', 'LISTING_CANCELLED', 'LISTING_INVITE'
));
GO

CREATE NONCLUSTERED INDEX IX_notifications_listing ON notifications(game_listing_id);
CREATE NONCLUSTERED INDEX IX_notifications_join_request ON notifications(join_request_id);
GO

-- ============================================================
-- REPORTS: real target references and reviewer information
-- ============================================================
ALTER TABLE reports ADD reported_user_id BIGINT NULL;
ALTER TABLE reports ADD reported_post_id BIGINT NULL;
ALTER TABLE reports ADD reviewed_by_user_id BIGINT NULL;
ALTER TABLE reports ADD reviewed_at DATETIME2 NULL;
ALTER TABLE reports ADD description VARCHAR(200) NULL;
GO

IF OBJECT_ID('dbo.CK_reports_status', 'C') IS NOT NULL
    ALTER TABLE reports DROP CONSTRAINT CK_reports_status;
GO

UPDATE reports
SET reported_user_id = CASE WHEN report_type = 'USER' THEN reference_id END,
    reported_post_id = CASE WHEN report_type = 'POST' THEN reference_id END,
    description = content,
    status = CASE WHEN status = 'ACTIONED' THEN 'RESOLVED' ELSE status END;
GO

-- A fresh development database has no valuable report data. Remove only invalid
-- legacy references before adding real foreign keys and the exactly-one check.
DELETE r FROM reports r
WHERE (r.reported_user_id IS NOT NULL AND NOT EXISTS (
          SELECT 1 FROM users u WHERE u.user_id = r.reported_user_id))
   OR (r.reported_post_id IS NOT NULL AND NOT EXISTS (
          SELECT 1 FROM posts p WHERE p.post_id = r.reported_post_id));
GO

IF EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.reports') AND name = 'IX_reports_reference')
    DROP INDEX IX_reports_reference ON reports;
IF OBJECT_ID('dbo.CK_reports_type', 'C') IS NOT NULL
    ALTER TABLE reports DROP CONSTRAINT CK_reports_type;
GO

ALTER TABLE reports DROP COLUMN reference_id;
ALTER TABLE reports DROP COLUMN report_type;
ALTER TABLE reports DROP COLUMN content;
GO

ALTER TABLE reports ADD CONSTRAINT FK_reports_reported_user
    FOREIGN KEY (reported_user_id) REFERENCES users(user_id);
ALTER TABLE reports ADD CONSTRAINT FK_reports_reported_post
    FOREIGN KEY (reported_post_id) REFERENCES posts(post_id);
ALTER TABLE reports ADD CONSTRAINT FK_reports_reviewer
    FOREIGN KEY (reviewed_by_user_id) REFERENCES users(user_id);
ALTER TABLE reports ADD CONSTRAINT CK_reports_exactly_one_target CHECK (
    (reported_user_id IS NOT NULL AND reported_post_id IS NULL)
    OR (reported_user_id IS NULL AND reported_post_id IS NOT NULL)
);
ALTER TABLE reports ADD CONSTRAINT CK_reports_status
    CHECK (status IN ('PENDING', 'RESOLVED', 'DISMISSED'));
GO

CREATE NONCLUSTERED INDEX IX_reports_reported_user ON reports(reported_user_id);
CREATE NONCLUSTERED INDEX IX_reports_reported_post ON reports(reported_post_id);
GO

-- The generated audit table was never used by the application.
DROP TABLE audit_log;
GO
