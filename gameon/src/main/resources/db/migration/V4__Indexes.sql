-- ============================================================
-- GameOn Database - V4__Indexes.sql
-- Description: Performance indexes for frequently queried columns
-- ============================================================

-- ============================================================
-- USERS INDEXES
-- ============================================================
CREATE NONCLUSTERED INDEX IX_users_username ON users(username);
CREATE NONCLUSTERED INDEX IX_users_role ON users(user_role);
CREATE NONCLUSTERED INDEX IX_users_active ON users(is_active);

-- ============================================================
-- USER_SPORT_PROFILES INDEXES
-- ============================================================
CREATE NONCLUSTERED INDEX IX_user_sport_profiles_user ON user_sport_profiles(user_id);
CREATE NONCLUSTERED INDEX IX_user_sport_profiles_sport ON user_sport_profiles(sport_id);
CREATE NONCLUSTERED INDEX IX_user_sport_profiles_winpct ON user_sport_profiles(win_percentage DESC);
CREATE NONCLUSTERED INDEX IX_user_sport_profiles_sport_winpct ON user_sport_profiles(sport_id, win_percentage DESC);

-- ============================================================
-- SPORT_FORMATS INDEXES
-- ============================================================
CREATE NONCLUSTERED INDEX IX_sport_formats_sport ON sport_formats(sport_id);

-- ============================================================
-- GAME_LISTINGS INDEXES
-- ============================================================
CREATE NONCLUSTERED INDEX IX_game_listings_creator ON game_listings(creator_id);
CREATE NONCLUSTERED INDEX IX_game_listings_format ON game_listings(format_id);
CREATE NONCLUSTERED INDEX IX_game_listings_date ON game_listings(scheduled_date);
CREATE NONCLUSTERED INDEX IX_game_listings_active ON game_listings(is_completed, scheduled_date)
    WHERE is_completed = 0;
CREATE NONCLUSTERED INDEX IX_game_listings_creator_active ON game_listings(creator_id, is_completed)
    WHERE is_completed = 0;

-- ============================================================
-- GAME_JOINERS INDEXES
-- ============================================================
CREATE NONCLUSTERED INDEX IX_game_joiners_listing ON game_joiners(game_listing_id);
CREATE NONCLUSTERED INDEX IX_game_joiners_user ON game_joiners(user_id);
CREATE NONCLUSTERED INDEX IX_game_joiners_status ON game_joiners(game_listing_id, status);
CREATE NONCLUSTERED INDEX IX_game_joiners_listing_team_status ON game_joiners(game_listing_id, team, status);

-- ============================================================
-- POSTS INDEXES
-- ============================================================
CREATE NONCLUSTERED INDEX IX_posts_user ON posts(user_id);
CREATE NONCLUSTERED INDEX IX_posts_created ON posts(created_at DESC);
CREATE NONCLUSTERED INDEX IX_posts_user_created ON posts(user_id, created_at DESC);
CREATE NONCLUSTERED INDEX IX_posts_privacy_created ON posts(privacy_setting, created_at DESC);

-- ============================================================
-- COMMENTS INDEXES
-- ============================================================
CREATE NONCLUSTERED INDEX IX_comments_post ON comments(post_id);
CREATE NONCLUSTERED INDEX IX_comments_user ON comments(user_id);
CREATE NONCLUSTERED INDEX IX_comments_post_created ON comments(post_id, created_at ASC);

-- ============================================================
-- LIKES INDEXES
-- ============================================================
CREATE NONCLUSTERED INDEX IX_likes_post ON likes(post_id);
CREATE NONCLUSTERED INDEX IX_likes_user ON likes(user_id);

-- ============================================================
-- FOLLOWS INDEXES
-- ============================================================
CREATE NONCLUSTERED INDEX IX_follows_follower ON follows(follower_user_id);
CREATE NONCLUSTERED INDEX IX_follows_followed ON follows(followed_user_id);

-- ============================================================
-- NOTIFICATIONS INDEXES
-- ============================================================
CREATE NONCLUSTERED INDEX IX_notifications_recipient ON notifications(recipient_id);
CREATE NONCLUSTERED INDEX IX_notifications_recipient_read ON notifications(recipient_id, is_read);
CREATE NONCLUSTERED INDEX IX_notifications_recipient_created ON notifications(recipient_id, created_at DESC);
CREATE NONCLUSTERED INDEX IX_notifications_unread ON notifications(recipient_id, is_read)
    WHERE is_read = 0;

-- ============================================================
-- REPORTS INDEXES
-- ============================================================
CREATE NONCLUSTERED INDEX IX_reports_status ON reports(status);
CREATE NONCLUSTERED INDEX IX_reports_reporter ON reports(reporter_id);
CREATE NONCLUSTERED INDEX IX_reports_reference ON reports(reference_id, report_type);
CREATE NONCLUSTERED INDEX IX_reports_pending ON reports(status)
    WHERE status = 'PENDING';

-- ============================================================
-- SESSIONS INDEXES
-- ============================================================
CREATE NONCLUSTERED INDEX IX_sessions_listing ON sessions(game_listing_id);
CREATE NONCLUSTERED INDEX IX_sessions_date ON sessions(session_date);

-- ============================================================
-- MATCH_RESULTS INDEXES
-- ============================================================
CREATE NONCLUSTERED INDEX IX_match_results_listing ON match_results(game_listing_id);

-- ============================================================
-- AUDIT_LOG INDEXES
-- ============================================================
CREATE NONCLUSTERED INDEX IX_audit_log_entity ON audit_log(entity_type, entity_id);
CREATE NONCLUSTERED INDEX IX_audit_log_performed ON audit_log(performed_by, performed_at DESC);
CREATE NONCLUSTERED INDEX IX_audit_log_date ON audit_log(performed_at DESC);
