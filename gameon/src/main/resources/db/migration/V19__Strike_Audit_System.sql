-- ============================================================
-- V19: STRIKE / WARNING DATABASE AUDIT SYSTEM
-- ------------------------------------------------------------
-- Every strike a moderator issues is stored permanently as a row in user_warnings.
-- A strike must never exist only as a notification, a UI message, or the
-- users.warning_count field. These records are NEVER deleted, even when a user is
-- banned - the moderation history must remain available for audit/demonstration.
--
-- This migration also:
--   * adds ban attribution to users (banned_by_user_id, ban_reason)
--   * extends the notification type check with STRIKE_ISSUED
--   * creates the readable view vw_user_warnings_readable
--
-- Existing report workflow, soft-delete logic, readable views and moderation
-- functionality are all preserved. No existing migration is modified.
-- ============================================================

-- ------------------------------------------------------------
-- 1. USERS: ban attribution
-- ------------------------------------------------------------
IF COL_LENGTH('dbo.users', 'banned_by_user_id') IS NULL
    ALTER TABLE users ADD banned_by_user_id BIGINT NULL;
GO

IF COL_LENGTH('dbo.users', 'ban_reason') IS NULL
    ALTER TABLE users ADD ban_reason VARCHAR(300) NULL;
GO

IF OBJECT_ID('dbo.FK_users_banned_by', 'F') IS NULL
    ALTER TABLE users ADD CONSTRAINT FK_users_banned_by
        FOREIGN KEY (banned_by_user_id) REFERENCES users(user_id) ON DELETE NO ACTION;
GO

-- ------------------------------------------------------------
-- 2. USER_WARNINGS: permanent strike audit table
-- ------------------------------------------------------------
IF OBJECT_ID('dbo.user_warnings', 'U') IS NULL
BEGIN
    CREATE TABLE user_warnings (
        warning_id      BIGINT IDENTITY(1,1) NOT NULL,
        user_id         BIGINT               NOT NULL,
        report_id       BIGINT               NULL,
        moderator_id    BIGINT               NOT NULL,
        warning_reason  VARCHAR(300)         NOT NULL,
        content_type    VARCHAR(20)          NOT NULL CONSTRAINT DF_user_warnings_content_type DEFAULT 'OTHER',
        content_id      BIGINT               NULL,
        strike_number   INT                  NOT NULL,
        warning_date    DATE                 NOT NULL CONSTRAINT DF_user_warnings_warning_date DEFAULT CAST(GETDATE() AS DATE),
        created_at      DATETIME2            NOT NULL CONSTRAINT DF_user_warnings_created_at DEFAULT GETDATE(),

        CONSTRAINT PK_user_warnings PRIMARY KEY (warning_id),
        CONSTRAINT FK_user_warnings_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE NO ACTION,
        CONSTRAINT FK_user_warnings_moderator FOREIGN KEY (moderator_id) REFERENCES users(user_id) ON DELETE NO ACTION,
        CONSTRAINT FK_user_warnings_report FOREIGN KEY (report_id) REFERENCES reports(report_id) ON DELETE NO ACTION,
        CONSTRAINT CK_user_warnings_content_type CHECK (content_type IN ('POST', 'COMMENT', 'USER', 'OTHER')),
        CONSTRAINT CK_user_warnings_strike_number CHECK (strike_number >= 1)
    );

    CREATE NONCLUSTERED INDEX IX_user_warnings_user ON user_warnings(user_id);
    CREATE NONCLUSTERED INDEX IX_user_warnings_moderator ON user_warnings(moderator_id);
    CREATE NONCLUSTERED INDEX IX_user_warnings_date ON user_warnings(warning_date DESC);
END;
GO

-- ------------------------------------------------------------
-- 3. NOTIFICATIONS: allow the STRIKE_ISSUED type
-- ------------------------------------------------------------
IF OBJECT_ID('dbo.CK_notifications_type', 'C') IS NOT NULL
    ALTER TABLE notifications DROP CONSTRAINT CK_notifications_type;
GO

ALTER TABLE notifications ADD CONSTRAINT CK_notifications_type CHECK (notification_type IN (
    'FOLLOW_NEW', 'JOIN_REQUEST_RECEIVED', 'JOIN_ACCEPTED', 'JOIN_REJECTED', 'JOIN_WITHDRAWN',
    'LISTING_CONFIRMED', 'LISTING_CANCELLED_INSUFFICIENT_PLAYERS',
    'MATCH_RESULT_POSTED', 'MATCH_RESULT_UPDATED', 'LISTING_CANCELLED', 'LISTING_INVITE',
    'ATTENDANCE_CONFIRMATION_OPEN', 'ATTENDANCE_CONFIRMATION_DEADLINE',
    'LAST_CALL_OFFER', 'LAST_CALL_CLAIMED', 'LAST_CALL_FULL',
    'PLACE_RELEASED_UNCONFIRMED', 'LISTING_CANCELLED_CREATOR_UNCONFIRMED',
    'CREATOR_CONFIRMATION_URGENT', 'STRIKE_ISSUED'
));
GO

-- ------------------------------------------------------------
-- 4. READABLE VIEW: vw_user_warnings_readable
--    Warning ID | Username | Moderator | Strike Number | Content Type | Reason | Date
-- ------------------------------------------------------------
CREATE OR ALTER VIEW vw_user_warnings_readable AS
SELECT
    w.warning_id,
    struck.username        AS username,
    moderator.username     AS moderator_username,
    w.strike_number,
    w.content_type,
    w.content_id,
    w.warning_reason       AS reason,
    w.report_id,
    w.warning_date,
    w.created_at
FROM user_warnings w
JOIN users struck    ON struck.user_id    = w.user_id
JOIN users moderator ON moderator.user_id = w.moderator_id;
GO
