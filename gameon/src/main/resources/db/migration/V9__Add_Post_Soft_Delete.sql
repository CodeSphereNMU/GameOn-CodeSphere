-- ============================================================
-- V9: Add soft-delete support for posts (Image Retention Policy)
-- Posts are marked as removed instead of hard-deleted.
-- Images are retained for moderation evidence.
-- Only explicit hard-delete removes the image file and DB reference.
-- ============================================================

-- Add is_removed flag to posts
ALTER TABLE posts ADD is_removed BIT NOT NULL DEFAULT 0;
GO

-- Add removed_by to track who removed (author username or 'MODERATOR')
ALTER TABLE posts ADD removed_by VARCHAR(50) NULL;
GO

-- Add removed_at timestamp
ALTER TABLE posts ADD removed_at DATETIME2 NULL;
GO

-- Drop the content not-empty constraint since posts can be image-only
-- (V5 added CK_posts_content_notempty but V6 made content nullable for image-only posts)
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_posts_content_notempty')
BEGIN
    ALTER TABLE posts DROP CONSTRAINT CK_posts_content_notempty;
END;
GO

-- Index for filtering active posts efficiently
CREATE INDEX IX_posts_is_removed ON posts(is_removed) WHERE is_removed = 0;
GO
