-- ============================================================
-- GameOn Database - V5__Constraints.sql
-- Description: Additional business rule constraints and triggers
-- ============================================================

-- ============================================================
-- ADDITIONAL CHECK CONSTRAINTS
-- ============================================================

-- Users: Username length validation
ALTER TABLE users ADD CONSTRAINT CK_users_username_length
    CHECK (LEN(username) >= 3 AND LEN(username) <= 50);

-- Users: Password must not be empty
ALTER TABLE users ADD CONSTRAINT CK_users_password_notempty
    CHECK (LEN(password) > 0);

-- Game Listings: Location must not be empty
ALTER TABLE game_listings ADD CONSTRAINT CK_game_listings_location_notempty
    CHECK (LEN(location) > 0);

-- Posts: Content must not be empty
ALTER TABLE posts ADD CONSTRAINT CK_posts_content_notempty
    CHECK (LEN(content) > 0);

-- Comments: Text must not be empty
ALTER TABLE comments ADD CONSTRAINT CK_comments_text_notempty
    CHECK (LEN(text) > 0);

-- Reports: Report reason must not be empty
ALTER TABLE reports ADD CONSTRAINT CK_reports_reason_notempty
    CHECK (LEN(report_reason) > 0);

-- ============================================================
-- DEFAULT VALUE CONSTRAINTS (Additional defaults)
-- ============================================================

-- Ensure win_percentage is recalculated correctly
-- This is handled in the application service layer, but we add a computed column for reference
-- ALTER TABLE user_sport_profiles ADD win_percentage_computed AS
--     CASE WHEN (wins + losses) > 0 THEN CAST(wins AS FLOAT) / CAST((wins + losses) AS FLOAT) * 100.0 ELSE 0.0 END;

-- ============================================================
-- STORED PROCEDURE: Calculate Win Percentage
-- ============================================================
GO
CREATE OR ALTER PROCEDURE sp_CalculateWinPercentage
    @userId BIGINT,
    @sportId BIGINT
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @wins INT, @losses INT, @percentage FLOAT;

    SELECT @wins = wins, @losses = losses
    FROM user_sport_profiles
    WHERE user_id = @userId AND sport_id = @sportId;

    IF (@wins + @losses) > 0
        SET @percentage = CAST(@wins AS FLOAT) / CAST((@wins + @losses) AS FLOAT) * 100.0;
    ELSE
        SET @percentage = 0.0;

    UPDATE user_sport_profiles
    SET win_percentage = @percentage, updated_at = GETDATE()
    WHERE user_id = @userId AND sport_id = @sportId;
END;
GO

-- ============================================================
-- STORED PROCEDURE: Get Leaderboard by Sport
-- ============================================================
CREATE OR ALTER PROCEDURE sp_GetLeaderboard
    @sportId BIGINT,
    @topN INT = 50
AS
BEGIN
    SET NOCOUNT ON;

    SELECT TOP (@topN)
        u.user_id,
        u.username,
        usp.sport_id,
        s.sport_name,
        usp.skill_level,
        usp.wins,
        usp.losses,
        usp.win_percentage
    FROM user_sport_profiles usp
    INNER JOIN users u ON u.user_id = usp.user_id
    INNER JOIN sports s ON s.sport_id = usp.sport_id
    WHERE usp.sport_id = @sportId
      AND u.is_active = 1
      AND (usp.wins + usp.losses) > 0
    ORDER BY usp.win_percentage DESC, usp.wins DESC;
END;
GO

-- ============================================================
-- STORED PROCEDURE: Get Pending Reports Count
-- ============================================================
CREATE OR ALTER PROCEDURE sp_GetPendingReportsCount
AS
BEGIN
    SET NOCOUNT ON;

    SELECT COUNT(*) AS pending_count
    FROM reports
    WHERE status = 'PENDING';
END;
GO

-- ============================================================
-- TRIGGER: Update updated_at on users modification
-- ============================================================
CREATE OR ALTER TRIGGER trg_users_updated_at
ON users
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE users
    SET updated_at = GETDATE()
    FROM users u
    INNER JOIN inserted i ON u.user_id = i.user_id;
END;
GO

-- ============================================================
-- TRIGGER: Update updated_at on game_listings modification
-- ============================================================
CREATE OR ALTER TRIGGER trg_game_listings_updated_at
ON game_listings
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE game_listings
    SET updated_at = GETDATE()
    FROM game_listings gl
    INNER JOIN inserted i ON gl.game_listing_id = i.game_listing_id;
END;
GO

-- ============================================================
-- TRIGGER: Update updated_at on posts modification
-- ============================================================
CREATE OR ALTER TRIGGER trg_posts_updated_at
ON posts
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE posts
    SET updated_at = GETDATE()
    FROM posts p
    INNER JOIN inserted i ON p.post_id = i.post_id;
END;
GO

-- ============================================================
-- VIEW: Active Game Listings (convenience view)
-- ============================================================
CREATE OR ALTER VIEW vw_active_game_listings AS
SELECT
    gl.game_listing_id,
    gl.creator_id,
    u.username AS creator_username,
    gl.format_id,
    sf.format_name,
    s.sport_name,
    sf.no_players AS max_players,
    gl.skill_level,
    gl.scheduled_date,
    gl.location,
    gl.privacy_setting,
    gl.created_at,
    (SELECT COUNT(*) FROM game_joiners gj
     WHERE gj.game_listing_id = gl.game_listing_id
       AND gj.status IN ('ACCEPTED', 'LOCKED')) AS current_players
FROM game_listings gl
INNER JOIN users u ON u.user_id = gl.creator_id
INNER JOIN sport_formats sf ON sf.format_id = gl.format_id
INNER JOIN sports s ON s.sport_id = sf.sport_id
WHERE gl.is_completed = 0
  AND gl.scheduled_date > GETDATE();
GO

-- ============================================================
-- VIEW: User Stats Summary (convenience view)
-- ============================================================
CREATE OR ALTER VIEW vw_user_stats AS
SELECT
    u.user_id,
    u.username,
    (SELECT COUNT(*) FROM follows f WHERE f.followed_user_id = u.user_id) AS follower_count,
    (SELECT COUNT(*) FROM follows f WHERE f.follower_user_id = u.user_id) AS following_count,
    (SELECT COUNT(*) FROM game_joiners gj
     WHERE gj.user_id = u.user_id AND gj.status IN ('ACCEPTED', 'LOCKED')) AS games_played,
    (SELECT COUNT(*) FROM posts p WHERE p.user_id = u.user_id) AS post_count
FROM users u
WHERE u.is_active = 1;
GO
