-- ============================================================
-- V10: Complete Readable SQL Views
-- Purpose: Lecturer demonstrations, easier SQL inspection,
--          business-friendly reporting
-- Naming convention: vw_<table>_readable
-- ============================================================

-- ============================================================
-- VIEW: vw_game_listings_readable
-- Full game listing details with creator, sport, format info
-- ============================================================
CREATE OR ALTER VIEW vw_game_listings_readable AS
SELECT
    gl.game_listing_id,
    u.username AS creator_username,
    u.email AS creator_email,
    s.sport_name,
    sf.format_name,
    sf.no_players AS max_players,
    gl.skill_level,
    gl.scheduled_date,
    gl.session_duration,
    gl.location,
    gl.venue_name,
    gl.address,
    gl.latitude,
    gl.longitude,
    gl.privacy_setting,
    gl.is_completed,
    CASE
        WHEN gl.is_completed = 1 THEN 'COMPLETED'
        WHEN EXISTS (SELECT 1 FROM sessions sess WHERE sess.game_listing_id = gl.game_listing_id) THEN 'CONFIRMED'
        ELSE 'OPEN'
    END AS listing_status,
    (SELECT COUNT(*) FROM game_joiners gj
     WHERE gj.game_listing_id = gl.game_listing_id
       AND gj.status IN ('ACCEPTED', 'LOCKED')) AS current_players,
    sf.no_players - (SELECT COUNT(*) FROM game_joiners gj
     WHERE gj.game_listing_id = gl.game_listing_id
       AND gj.status IN ('ACCEPTED', 'LOCKED')) AS spots_remaining,
    gl.created_at,
    gl.updated_at
FROM game_listings gl
INNER JOIN users u ON u.user_id = gl.creator_id
INNER JOIN sport_formats sf ON sf.format_id = gl.format_id
INNER JOIN sports s ON s.sport_id = sf.sport_id;
GO

-- ============================================================
-- VIEW: vw_join_requests_readable
-- All join requests with user, listing, and status details
-- ============================================================
CREATE OR ALTER VIEW vw_join_requests_readable AS
SELECT
    gj.user_id,
    u.username AS player_username,
    gj.game_listing_id,
    s.sport_name,
    sf.format_name,
    creator.username AS listing_creator,
    gj.team,
    gj.status AS request_status,
    CASE
        WHEN gj.format_position_id IS NOT NULL THEN
            (SELECT p.position_name FROM positions p
             INNER JOIN format_positions fp ON fp.position_id = p.position_id
             WHERE fp.position_id = gj.format_position_id AND fp.format_id = gl.format_id)
        ELSE 'Not specified'
    END AS primary_position,
    gj.created_at AS request_date,
    gj.updated_at AS last_updated
FROM game_joiners gj
INNER JOIN users u ON u.user_id = gj.user_id
INNER JOIN game_listings gl ON gl.game_listing_id = gj.game_listing_id
INNER JOIN users creator ON creator.user_id = gl.creator_id
INNER JOIN sport_formats sf ON sf.format_id = gl.format_id
INNER JOIN sports s ON s.sport_id = sf.sport_id;
GO

-- ============================================================
-- VIEW: vw_notifications_readable
-- Notifications with recipient details and read status
-- ============================================================
CREATE OR ALTER VIEW vw_notifications_readable AS
SELECT
    n.notification_id,
    u.username AS recipient_username,
    n.text AS notification_text,
    n.notification_type,
    CASE WHEN n.is_read = 1 THEN 'Read' ELSE 'Unread' END AS read_status,
    n.created_at,
    n.updated_at
FROM notifications n
INNER JOIN users u ON u.user_id = n.recipient_id;
GO

-- ============================================================
-- VIEW: vw_match_results_readable
-- Match results with full game context
-- ============================================================
CREATE OR ALTER VIEW vw_match_results_readable AS
SELECT
    mr.match_result_id,
    mr.game_listing_id,
    s.sport_name,
    sf.format_name,
    creator.username AS listing_creator,
    gl.scheduled_date AS game_date,
    gl.location AS game_location,
    gl.venue_name,
    mr.team_a_score,
    mr.team_b_score,
    mr.winners,
    CASE mr.winners
        WHEN 'TEAM_A' THEN 'Team A Wins'
        WHEN 'TEAM_B' THEN 'Team B Wins'
        WHEN 'DRAW' THEN 'Draw'
    END AS result_description,
    mr.created_at AS recorded_at,
    mr.updated_at AS last_modified
FROM match_results mr
INNER JOIN game_listings gl ON gl.game_listing_id = mr.game_listing_id
INNER JOIN users creator ON creator.user_id = gl.creator_id
INNER JOIN sport_formats sf ON sf.format_id = gl.format_id
INNER JOIN sports s ON s.sport_id = sf.sport_id;
GO

-- ============================================================
-- VIEW: vw_user_stats (Enhanced - replaces V5 version)
-- Comprehensive user statistics including sport profiles
-- ============================================================
CREATE OR ALTER VIEW vw_user_stats AS
SELECT
    u.user_id,
    u.username,
    u.email,
    u.user_role,
    u.is_active,
    (SELECT COUNT(*) FROM follows f WHERE f.followed_user_id = u.user_id) AS follower_count,
    (SELECT COUNT(*) FROM follows f WHERE f.follower_user_id = u.user_id) AS following_count,
    (SELECT COUNT(*) FROM game_joiners gj
     WHERE gj.user_id = u.user_id AND gj.status IN ('ACCEPTED', 'LOCKED')) AS games_participated,
    (SELECT COUNT(*) FROM game_listings gl WHERE gl.creator_id = u.user_id) AS listings_created,
    (SELECT COUNT(*) FROM posts p WHERE p.user_id = u.user_id AND (p.is_removed = 0 OR p.is_removed IS NULL)) AS post_count,
    (SELECT COALESCE(SUM(usp.wins), 0) FROM user_sport_profiles usp WHERE usp.user_id = u.user_id) AS total_wins,
    (SELECT COALESCE(SUM(usp.losses), 0) FROM user_sport_profiles usp WHERE usp.user_id = u.user_id) AS total_losses,
    (SELECT COUNT(*) FROM user_sport_profiles usp WHERE usp.user_id = u.user_id) AS sports_count,
    u.created_at AS member_since
FROM users u;
GO

-- ============================================================
-- VIEW: vw_user_sport_profiles_readable
-- User sport profiles with calculated stats
-- ============================================================
CREATE OR ALTER VIEW vw_user_sport_profiles_readable AS
SELECT
    u.username,
    s.sport_name,
    usp.skill_level,
    usp.wins,
    usp.losses,
    (usp.wins + usp.losses) AS total_games,
    usp.win_percentage,
    usp.created_at AS profile_created,
    usp.updated_at AS last_updated
FROM user_sport_profiles usp
INNER JOIN users u ON u.user_id = usp.user_id
INNER JOIN sports s ON s.sport_id = usp.sport_id;
GO

-- ============================================================
-- VIEW: vw_posts_readable
-- Posts with author info and engagement metrics
-- ============================================================
CREATE OR ALTER VIEW vw_posts_readable AS
SELECT
    p.post_id,
    u.username AS author,
    p.content,
    p.image_path,
    p.privacy_setting,
    p.is_removed,
    p.removed_by,
    p.removed_at,
    (SELECT COUNT(*) FROM likes l WHERE l.post_id = p.post_id) AS like_count,
    (SELECT COUNT(*) FROM comments c WHERE c.post_id = p.post_id) AS comment_count,
    p.created_at AS posted_at,
    p.updated_at
FROM posts p
INNER JOIN users u ON u.user_id = p.user_id;
GO

-- ============================================================
-- VIEW: vw_follows_readable
-- Follow relationships in human-readable format
-- ============================================================
CREATE OR ALTER VIEW vw_follows_readable AS
SELECT
    follower.username AS follower_username,
    followed.username AS followed_username,
    f.created_at AS followed_since
FROM follows f
INNER JOIN users follower ON follower.user_id = f.follower_user_id
INNER JOIN users followed ON followed.user_id = f.followed_user_id;
GO

-- ============================================================
-- VIEW: vw_reports_readable
-- Reports with reporter details and status
-- ============================================================
CREATE OR ALTER VIEW vw_reports_readable AS
SELECT
    r.report_id,
    reporter.username AS reporter_username,
    r.report_type,
    r.reference_id,
    CASE r.report_type
        WHEN 'USER' THEN (SELECT username FROM users WHERE user_id = r.reference_id)
        WHEN 'POST' THEN (SELECT u2.username FROM posts p2 INNER JOIN users u2 ON u2.user_id = p2.user_id WHERE p2.post_id = r.reference_id)
    END AS reported_item_owner,
    r.report_reason,
    r.content AS additional_details,
    r.status AS report_status,
    r.created_at AS reported_at,
    r.updated_at
FROM reports r
INNER JOIN users reporter ON reporter.user_id = r.reporter_id;
GO

-- ============================================================
-- VIEW: vw_sessions_readable
-- Confirmed sessions with full game context
-- ============================================================
CREATE OR ALTER VIEW vw_sessions_readable AS
SELECT
    sess.session_id,
    sess.game_listing_id,
    s.sport_name,
    sf.format_name,
    creator.username AS listing_creator,
    sess.session_date,
    sess.location AS session_location,
    gl.venue_name,
    gl.skill_level,
    sf.no_players AS max_players,
    (SELECT COUNT(*) FROM game_joiners gj
     WHERE gj.game_listing_id = gl.game_listing_id
       AND gj.status = 'LOCKED') AS locked_players,
    sess.created_at AS confirmed_at
FROM sessions sess
INNER JOIN game_listings gl ON gl.game_listing_id = sess.game_listing_id
INNER JOIN users creator ON creator.user_id = gl.creator_id
INNER JOIN sport_formats sf ON sf.format_id = gl.format_id
INNER JOIN sports s ON s.sport_id = sf.sport_id;
GO

-- ============================================================
-- VIEW: vw_listing_invitations_readable
-- Invitation history with user details
-- ============================================================
CREATE OR ALTER VIEW vw_listing_invitations_readable AS
SELECT
    li.invitation_id,
    li.game_listing_id,
    s.sport_name,
    sf.format_name,
    inviter.username AS invited_by,
    invited.username AS invited_user,
    li.status AS invitation_status,
    li.created_at AS invited_at,
    li.updated_at
FROM listing_invitations li
INNER JOIN game_listings gl ON gl.game_listing_id = li.game_listing_id
INNER JOIN users inviter ON inviter.user_id = li.invited_by_user_id
INNER JOIN users invited ON invited.user_id = li.invited_user_id
INNER JOIN sport_formats sf ON sf.format_id = gl.format_id
INNER JOIN sports s ON s.sport_id = sf.sport_id;
GO
