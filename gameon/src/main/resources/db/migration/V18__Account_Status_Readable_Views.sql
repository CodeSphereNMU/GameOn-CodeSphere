-- ============================================================
-- V18: READABLE VIEWS - ACCOUNT STATUS VISIBILITY
-- ------------------------------------------------------------
-- Expose account_status and warning_count (and ban/moderation metadata) through
-- human-readable views so administrators and lecturers can inspect user standing
-- directly in SSMS.
--
-- IMPORTANT: the admin-facing user views intentionally do NOT filter by
-- account_status, so BANNED and SUSPENDED users remain visible (that is the
-- whole point - a ban must be visible, not hidden).
--
-- NOTE: the users table in this schema has no separate full_name column; the
-- username is the display name. It is exposed as both username and full_name
-- so the readable output matches the requested column layout.
-- ============================================================

-- 1. vw_users_readable - one row per user with full moderation standing.
--    Columns: user_id | username | full_name | account_status | warning_count
--             | banned_at | last_moderation_action | last_moderation_at | role
CREATE OR ALTER VIEW vw_users_readable AS
SELECT
    u.user_id,
    u.username,
    u.username AS full_name,
    u.type_of_user AS role,
    u.account_status,
    u.warning_count,
    u.banned_at,
    u.last_moderation_action,
    u.last_moderation_at
FROM users u;
GO

-- 2. vw_user_profiles_readable - profile-oriented view including status + warnings
--    alongside follower/following counts and sport count. All users are shown.
CREATE OR ALTER VIEW vw_user_profiles_readable AS
SELECT
    u.user_id,
    u.username,
    u.username AS full_name,
    u.type_of_user AS role,
    u.account_status,
    u.warning_count,
    u.banned_at,
    u.last_moderation_action,
    u.last_moderation_at,
    (SELECT COUNT(*) FROM follows f WHERE f.followed_user_id = u.user_id) AS follower_count,
    (SELECT COUNT(*) FROM follows f WHERE f.follower_user_id = u.user_id) AS following_count,
    (SELECT COUNT(*) FROM user_sport_profiles usp WHERE usp.user_id = u.user_id) AS sport_count
FROM users u;
GO

-- 3. vw_user_stats - rebuilt to surface account_status and warning_count.
--    Previously filtered to ACTIVE users only; now includes ALL users so banned
--    and suspended accounts remain visible to administrators, with their status.
CREATE OR ALTER VIEW vw_user_stats AS
SELECT
    u.user_id,
    u.username,
    u.account_status,
    u.warning_count,
    u.banned_at,
    (SELECT COUNT(*) FROM follows f WHERE f.followed_user_id = u.user_id) AS follower_count,
    (SELECT COUNT(*) FROM follows f WHERE f.follower_user_id = u.user_id) AS following_count,
    (SELECT COUNT(*) FROM game_joiners gj
     WHERE gj.user_id = u.user_id AND gj.status IN ('ACCEPTED', 'LOCKED')) AS games_joined,
    (SELECT COUNT(*) FROM match_results result
     JOIN game_joiners participant ON participant.game_listing_id = result.game_listing_id
     WHERE participant.user_id = u.user_id
       AND participant.status = 'LOCKED') AS completed_games,
    (SELECT COUNT(*) FROM posts post
     WHERE post.user_id = u.user_id AND post.removed_at IS NULL) AS active_post_count,
    (SELECT COUNT(*) FROM posts post
     WHERE post.user_id = u.user_id AND post.removed_at IS NOT NULL) AS removed_post_count
FROM users u;
GO
