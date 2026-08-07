-- V4: Add account_status column to users table.
-- Purpose: Database preparation for future account suspension/banning enforcement.
-- This migration does NOT implement application-level enforcement; it only adds the
-- column, backfills existing rows as 'ACTIVE', and constrains allowed values.

-- ============================================================
-- 1. Add users.account_status as nullable initially
-- ============================================================

IF COL_LENGTH('dbo.users', 'account_status') IS NULL
BEGIN
    ALTER TABLE [dbo].[users] ADD [account_status] VARCHAR(20) NULL;
END;
GO

-- ============================================================
-- 2. Backfill only NULL rows to 'ACTIVE' (never overwrite existing values)
-- ============================================================

UPDATE [dbo].[users] SET [account_status] = 'ACTIVE' WHERE [account_status] IS NULL;
GO

-- ============================================================
-- 3. Make column NOT NULL now that all rows have a value
-- ============================================================

ALTER TABLE [dbo].[users] ALTER COLUMN [account_status] VARCHAR(20) NOT NULL;
GO

-- ============================================================
-- 4. Named DEFAULT constraint for future inserts
-- ============================================================

IF OBJECT_ID('dbo.DF_users_account_status', 'D') IS NULL
    ALTER TABLE [dbo].[users] ADD CONSTRAINT DF_users_account_status DEFAULT 'ACTIVE' FOR [account_status];
GO

-- ============================================================
-- 5. Named CHECK constraint: only ACTIVE, SUSPENDED, BANNED allowed
-- ============================================================

IF OBJECT_ID('dbo.CK_users_account_status', 'C') IS NULL
    ALTER TABLE [dbo].[users] ADD CONSTRAINT CK_users_account_status
        CHECK ([account_status] IN ('ACTIVE', 'SUSPENDED', 'BANNED'));
GO
