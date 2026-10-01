-- ============================================================
-- V17: ACCOUNT STATUS VISIBILITY + WARNING/STRIKE SYSTEM
-- ------------------------------------------------------------
-- account_status already exists (V8): VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
-- with CHECK (account_status IN ('ACTIVE','SUSPENDED','BANNED')).
--
-- This migration adds the moderation tracking columns so a user's standing is
-- fully visible to administrators in the database, readable views and dashboards:
--   * warning_count          - number of strikes (3 strikes => BANNED)
--   * banned_at              - timestamp the account was banned (NULL if not banned)
--   * last_moderation_action - human-readable label of the last moderation action
--   * last_moderation_at     - timestamp of the last moderation action
--
-- Users are NEVER physically deleted; a ban only changes account_status = 'BANNED'
-- and records banned_at, preserving all reports, listings, statistics and history.
-- ============================================================

IF COL_LENGTH('dbo.users', 'warning_count') IS NULL
    ALTER TABLE users ADD warning_count INT NOT NULL CONSTRAINT DF_users_warning_count DEFAULT 0;
GO

IF COL_LENGTH('dbo.users', 'banned_at') IS NULL
    ALTER TABLE users ADD banned_at DATETIME2 NULL;
GO

IF COL_LENGTH('dbo.users', 'last_moderation_action') IS NULL
    ALTER TABLE users ADD last_moderation_action VARCHAR(100) NULL;
GO

IF COL_LENGTH('dbo.users', 'last_moderation_at') IS NULL
    ALTER TABLE users ADD last_moderation_at DATETIME2 NULL;
GO

-- Every existing user must have a concrete warning count and status.
UPDATE users SET warning_count = 0 WHERE warning_count IS NULL;
GO

-- Keep banned_at consistent for any users already banned before this migration.
UPDATE users SET banned_at = GETDATE()
WHERE account_status = 'BANNED' AND banned_at IS NULL;
GO

-- Guard rail: warning_count can never be negative.
IF OBJECT_ID('dbo.CK_users_warning_count', 'C') IS NULL
    ALTER TABLE users ADD CONSTRAINT CK_users_warning_count CHECK (warning_count >= 0);
GO
