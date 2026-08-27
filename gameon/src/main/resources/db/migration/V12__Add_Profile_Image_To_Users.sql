-- ============================================================
-- V12: Add profile_image_path column to users table
-- Stores the URL path to the user's uploaded profile picture
-- ============================================================

ALTER TABLE users ADD profile_image_path NVARCHAR(500) NULL;
