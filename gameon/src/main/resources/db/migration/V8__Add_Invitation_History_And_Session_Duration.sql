-- ============================================================
-- V8: Add invitation history table and session_duration column
-- Supports additional invitations after listing creation
-- and tracks invitation history for duplicate prevention
-- ============================================================

-- Add session_duration to game_listings (if not exists)
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('game_listings') AND name = 'session_duration')
BEGIN
    ALTER TABLE game_listings ADD session_duration INT NOT NULL DEFAULT 1;
END;
GO

-- ============================================================
-- LISTING_INVITATIONS: Tracks all invitations sent for a listing
-- ============================================================
CREATE TABLE listing_invitations (
    invitation_id       BIGINT IDENTITY(1,1) NOT NULL,
    game_listing_id     BIGINT               NOT NULL,
    invited_user_id     BIGINT               NOT NULL,
    invited_by_user_id  BIGINT               NOT NULL,
    status              VARCHAR(20)          NOT NULL DEFAULT 'PENDING',
    created_at          DATETIME2            NOT NULL DEFAULT GETDATE(),
    updated_at          DATETIME2            NOT NULL DEFAULT GETDATE(),

    CONSTRAINT PK_listing_invitations PRIMARY KEY (invitation_id),
    CONSTRAINT FK_listing_invitations_listing FOREIGN KEY (game_listing_id) REFERENCES game_listings(game_listing_id) ON DELETE CASCADE,
    CONSTRAINT FK_listing_invitations_invited FOREIGN KEY (invited_user_id) REFERENCES users(user_id) ON DELETE NO ACTION,
    CONSTRAINT FK_listing_invitations_inviter FOREIGN KEY (invited_by_user_id) REFERENCES users(user_id) ON DELETE NO ACTION,
    CONSTRAINT CK_listing_invitations_status CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'EXPIRED')),
    CONSTRAINT UQ_listing_invitations_unique UNIQUE (game_listing_id, invited_user_id)
);
GO

-- Index for quick lookup of invitations by listing
CREATE INDEX IX_listing_invitations_listing ON listing_invitations(game_listing_id);

-- Index for quick lookup of invitations by user
CREATE INDEX IX_listing_invitations_user ON listing_invitations(invited_user_id);
GO
