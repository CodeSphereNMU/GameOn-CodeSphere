-- V11: Attendance Confirmation Lifecycle
-- Replaces the T-2h assumption-based lock-in with explicit attendance confirmation.
-- New lifecycle: T-24h confirmation opens, T-2h deadline, T-2h→T-1h late withdrawal/replacement, T-1h finalisation.

-- ============================================================
-- GAME JOINERS: add attendance confirmation tracking
-- ============================================================
ALTER TABLE game_joiners ADD attendance_confirmed_at DATETIME2 NULL;
ALTER TABLE game_joiners ADD is_late_withdrawal BIT NOT NULL CONSTRAINT DF_game_joiners_late_withdrawal DEFAULT 0;
GO

-- Update the status constraint to allow CONFIRMED_ATTENDANCE status
IF OBJECT_ID('dbo.CK_game_joiners_status', 'C') IS NOT NULL
    ALTER TABLE game_joiners DROP CONSTRAINT CK_game_joiners_status;
GO

ALTER TABLE game_joiners ADD CONSTRAINT CK_game_joiners_status
    CHECK (status IN ('ACCEPTED', 'CONFIRMED_ATTENDANCE', 'LOCKED', 'LEFT'));
GO

-- Migrate existing LOCKED joiners: they implicitly confirmed attendance under old rules
UPDATE game_joiners
SET attendance_confirmed_at = DATEADD(HOUR, -2, gl.scheduled_date)
FROM game_joiners gj
JOIN game_listings gl ON gl.game_listing_id = gj.game_listing_id
WHERE gj.status = 'LOCKED' AND gj.attendance_confirmed_at IS NULL;
GO

-- ============================================================
-- JOIN REQUESTS: add last-call approval flag
-- ============================================================
ALTER TABLE join_requests ADD is_last_call_approved BIT NOT NULL CONSTRAINT DF_join_requests_last_call DEFAULT 0;
GO

-- ============================================================
-- NOTIFICATIONS: add new lifecycle notification types
-- ============================================================
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
    'CREATOR_CONFIRMATION_URGENT'
));
GO

-- Index for efficient lifecycle queries on game_joiners by status and confirmation
CREATE NONCLUSTERED INDEX IX_game_joiners_status_confirmed
    ON game_joiners(status, attendance_confirmed_at)
    WHERE status IN ('ACCEPTED', 'CONFIRMED_ATTENDANCE');
GO
