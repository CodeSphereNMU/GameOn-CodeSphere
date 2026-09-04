-- V14: Add profile picture support for users.
--
-- Adds a single nullable column to dbo.users that stores the PUBLIC URL PATH of the
-- user's uploaded profile picture (e.g. /uploads/profile-pictures/<uuid>.webp), NOT the
-- image binary. Image bytes live on the filesystem under uploads/profile-pictures/ and are
-- served as static files, matching the existing post-image approach (V12). A NULL value
-- means "no picture uploaded" and the UI falls back to a default avatar.
--
-- This migration only ADDS a column; it does not touch any existing table, constraint,
-- workflow, view, or seed data. Guarded with a defensive existence check so re-runs are safe.

IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID('dbo.users') AND name = 'profile_picture_url'
)
    ALTER TABLE dbo.users ADD profile_picture_url VARCHAR(500) NULL;
GO
