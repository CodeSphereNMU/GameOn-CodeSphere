-- ============================================================
-- V20: MODERATION ACTIONS AUDIT TRAIL
-- ------------------------------------------------------------
-- Records every moderator/administrator action (dismiss report, issue warning,
-- remove content + warning, suspend, ban, reverse warning, restore content,
-- unban, reset warnings) as a permanent moderation_actions row. Combined with
-- user_warnings (V19) this forms the full moderation audit trail.
--
-- Also refreshes vw_users_readable to surface ban_reason / banned_by (added to
-- users in V19) and adds vw_moderation_actions_readable.
--
-- Existing migrations, report workflow, soft-delete logic, prior readable views,
-- lifecycle logic and authentication are all preserved. No existing migration is
-- modified.
-- ============================================================

-- ------------------------------------------------------------
-- 1. moderation_actions: permanent moderator action audit log
-- ------------------------------------------------------------
IF OBJECT_ID('dbo.moderation_actions', 'U') IS NULL
BEGIN
    CREATE TABLE moderation_actions (
        moderation_action_id BIGINT IDENTITY(1,1) NOT NULL,
        moderator_id         BIGINT               NOT NULL,
        target_user_id       BIGINT               NULL,
        report_id            BIGINT               NULL,
        action_type          VARCHAR(40)          NOT NULL,
        reason               VARCHAR(300)         NULL,
        content_type         VARCHAR(20)          NULL,
        content_id           BIGINT               NULL,
        created_at           DATETIME2            NOT NULL CONSTRAINT DF_moderation_actions_created_at DEFAULT GETDATE(),

        CONSTRAINT PK_moderation_actions PRIMARY KEY (moderation_action_id),
        CONSTRAINT FK_moderation_actions_moderator FOREIGN KEY (moderator_id) REFERENCES users(user_id) ON DELETE NO ACTION,
        CONSTRAINT FK_moderation_actions_target FOREIGN KEY (target_user_id) REFERENCES users(user_id) ON DELETE NO ACTION,
        CONSTRAINT FK_moderation_actions_report FOREIGN KEY (report_id) REFERENCES reports(report_id) ON DELETE NO ACTION,
        CONSTRAINT CK_moderation_actions_type CHECK (action_type IN (
            'DISMISS_REPORT', 'ISSUE_WARNING', 'REMOVE_CONTENT_AND_WARNING',
            'SUSPEND', 'BAN', 'REVERSE_WARNING', 'RESTORE_CONTENT', 'UNBAN', 'RESET_WARNINGS'
        )),
        CONSTRAINT CK_moderation_actions_content_type CHECK (content_type IS NULL OR content_type IN ('POST', 'COMMENT', 'USER', 'OTHER'))
    );

    CREATE NONCLUSTERED INDEX IX_moderation_actions_moderator ON moderation_actions(moderator_id);
    CREATE NONCLUSTERED INDEX IX_moderation_actions_target ON moderation_actions(target_user_id);
    CREATE NONCLUSTERED INDEX IX_moderation_actions_created ON moderation_actions(created_at DESC);
END;
GO

-- ------------------------------------------------------------
-- 2. vw_users_readable - refreshed to include ban attribution.
--    Username | Status | Warning Count | Ban Reason | banned by | ...
-- ------------------------------------------------------------
CREATE OR ALTER VIEW vw_users_readable AS
SELECT
    u.user_id,
    u.username,
    u.username AS full_name,
    u.type_of_user AS role,
    u.account_status,
    u.warning_count,
    u.ban_reason,
    u.banned_at,
    banning_mod.username AS banned_by_username,
    u.last_moderation_action,
    u.last_moderation_at
FROM users u
LEFT JOIN users banning_mod ON banning_mod.user_id = u.banned_by_user_id;
GO

-- ------------------------------------------------------------
-- 3. vw_moderation_actions_readable - human-readable moderation history.
--    Action | Moderator | Target user | Reason | Content | Report | Date
-- ------------------------------------------------------------
CREATE OR ALTER VIEW vw_moderation_actions_readable AS
SELECT
    a.moderation_action_id,
    a.action_type,
    moderator.username AS moderator_username,
    target.username    AS target_username,
    a.reason,
    a.content_type,
    a.content_id,
    a.report_id,
    a.created_at
FROM moderation_actions a
JOIN users moderator      ON moderator.user_id = a.moderator_id
LEFT JOIN users target    ON target.user_id    = a.target_user_id;
GO
