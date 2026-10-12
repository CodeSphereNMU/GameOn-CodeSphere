-- V22: Link comment notifications to posts and allow the organiser player-short reminder.
ALTER TABLE dbo.notifications ADD post_id BIGINT NULL;
GO

ALTER TABLE dbo.notifications ADD CONSTRAINT FK_notifications_post
FOREIGN KEY (post_id) REFERENCES dbo.posts(post_id) ON DELETE NO ACTION;
GO

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
        'LAST_CALL_PLACE_FILLED',
        'LISTING_NEEDS_PLAYERS'
    ));
GO
