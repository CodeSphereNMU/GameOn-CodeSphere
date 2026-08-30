-- V13: Allow empty post content (image-only posts)
-- Manual testing on SQL Server surfaced a CHECK constraint (CK_posts_content_notempty)
-- on dbo.posts.content that rejects the empty string. That constraint blocks image-only
-- posts, which the application intends to allow (a post is valid if it has non-blank text
-- OR at least one image; that rule is enforced in PostService).
--
-- posts.content stays NOT NULL. Image-only posts store an empty string. This migration only
-- removes the "content must be non-empty" CHECK so the empty string is accepted at the DB level.
-- The constraint was applied directly to the database (it is not part of the Flyway chain),
-- so we drop it defensively only if present.

IF OBJECT_ID('dbo.CK_posts_content_notempty', 'C') IS NOT NULL
    ALTER TABLE dbo.posts DROP CONSTRAINT CK_posts_content_notempty;
GO
