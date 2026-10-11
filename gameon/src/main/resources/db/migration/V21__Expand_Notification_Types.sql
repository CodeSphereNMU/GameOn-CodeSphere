-- V21: Expand allowed notification types for GameOn notification improvements.
-- Preserve all previously supported values and allow the five new types.
-- Safe to apply to an existing database where this constraint was updated manually.

IF OBJECT_ID('dbo.CK_notifications_type', 'C') IS NOT NULL
    ALTER TABLE dbo.notifications DROP CONSTRAINT CK_notifications_type;
GO

ALTER TABLE dbo.notifications ADD CONSTRAINT CK_notifications_type
    CHECK (notification_type IN (
        'FOLLOW_NEW',
        'JOIN_REQUEST_RECEIVED',
        'JOIN_ACCEPTED',
        'JOIN_REJECTED',
        'JOIN_WITHDRAWN',
        'LISTING_CONFIRMED',
        'LISTING_CANCELLED_INSUFFICIENT_PLAYERS',
        'MATCH_RESULT_POSTED',
        'MATCH_RESULT_UPDATED',
        'LISTING_CANCELLED',
        'LISTING_INVITE',
        'ATTENDANCE_CONFIRMATION_OPEN',
        'ATTENDANCE_CONFIRMATION_DEADLINE',
        'LAST_CALL_OFFER',
        'LAST_CALL_CLAIMED',
        'LAST_CALL_FULL',
        'PLACE_RELEASED_UNCONFIRMED',
        'LISTING_CANCELLED_CREATOR_UNCONFIRMED',
        'CREATOR_CONFIRMATION_URGENT',
        'STRIKE_ISSUED',
        'ATTENDANCE_CONFIRMED',
        'ATTENDANCE_REMINDER',
        'POST_COMMENTED',
        'MATCH_RESULT_REMINDER',
        'LAST_CALL_PLACE_FILLED'
    ));
GO
