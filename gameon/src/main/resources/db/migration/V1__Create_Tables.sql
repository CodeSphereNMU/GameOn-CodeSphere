-- ============================================================
-- GameOn Database Schema - V1__Create_Tables.sql
-- Database: GameOnDb (SQL Server)
-- Description: Creates all 16 tables with PKs, FKs, and constraints
-- ============================================================

-- ============================================================
-- CREATE DATABASE (run manually before Flyway)
-- ============================================================
-- CREATE DATABASE GameOnDb;
-- GO
-- USE GameOnDb;
-- GO

-- ============================================================
-- 1. USERS (Parent table - no dependencies)
-- ============================================================
CREATE TABLE users (
    user_id         BIGINT IDENTITY(1,1) NOT NULL,
    username        VARCHAR(50)          NOT NULL,
    email           VARCHAR(100)         NULL,
    password        VARCHAR(255)         NOT NULL,
    user_role       VARCHAR(20)          NOT NULL DEFAULT 'USER',
    is_active       BIT                  NOT NULL DEFAULT 1,
    created_at      DATETIME2            NOT NULL DEFAULT GETDATE(),
    updated_at      DATETIME2            NOT NULL DEFAULT GETDATE(),
    created_by      VARCHAR(50)          NULL,
    updated_by      VARCHAR(50)          NULL,

    CONSTRAINT PK_users PRIMARY KEY (user_id),
    CONSTRAINT UQ_users_username UNIQUE (username),
    CONSTRAINT CK_users_role CHECK (user_role IN ('USER', 'MODERATOR', 'ADMIN'))
);

-- ============================================================
-- 2. SPORTS (Reference table - no dependencies)
-- ============================================================
CREATE TABLE sports (
    sport_id        BIGINT IDENTITY(1,1) NOT NULL,
    sport_name      VARCHAR(50)          NOT NULL,
    no_players      INT                  NOT NULL,
    created_at      DATETIME2            NOT NULL DEFAULT GETDATE(),
    updated_at      DATETIME2            NOT NULL DEFAULT GETDATE(),

    CONSTRAINT PK_sports PRIMARY KEY (sport_id),
    CONSTRAINT UQ_sports_name UNIQUE (sport_name),
    CONSTRAINT CK_sports_players CHECK (no_players > 0)
);

-- ============================================================
-- 3. POSITIONS (Reference table - no dependencies)
-- ============================================================
CREATE TABLE positions (
    position_id     BIGINT IDENTITY(1,1) NOT NULL,
    position_name   VARCHAR(50)          NOT NULL,
    created_at      DATETIME2            NOT NULL DEFAULT GETDATE(),
    updated_at      DATETIME2            NOT NULL DEFAULT GETDATE(),

    CONSTRAINT PK_positions PRIMARY KEY (position_id),
    CONSTRAINT UQ_positions_name UNIQUE (position_name)
);

-- ============================================================
-- 4. SPORT_FORMATS (Depends on: sports)
-- ============================================================
CREATE TABLE sport_formats (
    format_id       BIGINT IDENTITY(1,1) NOT NULL,
    sport_id        BIGINT               NOT NULL,
    format_name     VARCHAR(50)          NOT NULL,
    no_players      INT                  NOT NULL,
    has_positions   BIT                  NOT NULL DEFAULT 0,
    created_at      DATETIME2            NOT NULL DEFAULT GETDATE(),
    updated_at      DATETIME2            NOT NULL DEFAULT GETDATE(),

    CONSTRAINT PK_sport_formats PRIMARY KEY (format_id),
    CONSTRAINT FK_sport_formats_sport FOREIGN KEY (sport_id) REFERENCES sports(sport_id) ON DELETE CASCADE,
    CONSTRAINT CK_sport_formats_players CHECK (no_players > 0)
);

-- ============================================================
-- 5. FORMAT_POSITIONS (Junction: sport_formats <-> positions)
-- ============================================================
CREATE TABLE format_positions (
    format_id       BIGINT NOT NULL,
    position_id     BIGINT NOT NULL,

    CONSTRAINT PK_format_positions PRIMARY KEY (format_id, position_id),
    CONSTRAINT FK_format_positions_format FOREIGN KEY (format_id) REFERENCES sport_formats(format_id) ON DELETE CASCADE,
    CONSTRAINT FK_format_positions_position FOREIGN KEY (position_id) REFERENCES positions(position_id) ON DELETE CASCADE
);

-- ============================================================
-- 6. USER_SPORT_PROFILES (Junction: users <-> sports + stats)
-- ============================================================
CREATE TABLE user_sport_profiles (
    user_id         BIGINT      NOT NULL,
    sport_id        BIGINT      NOT NULL,
    skill_level     VARCHAR(20) NOT NULL,
    wins            INT         NOT NULL DEFAULT 0,
    losses          INT         NOT NULL DEFAULT 0,
    win_percentage  FLOAT       NOT NULL DEFAULT 0.0,
    created_at      DATETIME2   NOT NULL DEFAULT GETDATE(),
    updated_at      DATETIME2   NOT NULL DEFAULT GETDATE(),

    CONSTRAINT PK_user_sport_profiles PRIMARY KEY (user_id, sport_id),
    CONSTRAINT FK_user_sport_profiles_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT FK_user_sport_profiles_sport FOREIGN KEY (sport_id) REFERENCES sports(sport_id) ON DELETE NO ACTION,
    CONSTRAINT CK_user_sport_profiles_skill CHECK (skill_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    CONSTRAINT CK_user_sport_profiles_wins CHECK (wins >= 0),
    CONSTRAINT CK_user_sport_profiles_losses CHECK (losses >= 0),
    CONSTRAINT CK_user_sport_profiles_winpct CHECK (win_percentage >= 0.0 AND win_percentage <= 100.0)
);

-- ============================================================
-- 7. GAME_LISTINGS (Depends on: users, sport_formats)
-- ============================================================
CREATE TABLE game_listings (
    game_listing_id BIGINT IDENTITY(1,1) NOT NULL,
    creator_id      BIGINT               NOT NULL,
    format_id       BIGINT               NOT NULL,
    skill_level     VARCHAR(20)          NOT NULL,
    scheduled_date  DATETIME2            NOT NULL,
    is_completed    BIT                  NOT NULL DEFAULT 0,
    location        VARCHAR(200)         NOT NULL,
    privacy_setting VARCHAR(10)          NOT NULL DEFAULT 'PUBLIC',
    created_at      DATETIME2            NOT NULL DEFAULT GETDATE(),
    updated_at      DATETIME2            NOT NULL DEFAULT GETDATE(),
    created_by      VARCHAR(50)          NULL,
    updated_by      VARCHAR(50)          NULL,

    CONSTRAINT PK_game_listings PRIMARY KEY (game_listing_id),
    CONSTRAINT FK_game_listings_creator FOREIGN KEY (creator_id) REFERENCES users(user_id) ON DELETE NO ACTION,
    CONSTRAINT FK_game_listings_format FOREIGN KEY (format_id) REFERENCES sport_formats(format_id) ON DELETE NO ACTION,
    CONSTRAINT CK_game_listings_skill CHECK (skill_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    CONSTRAINT CK_game_listings_privacy CHECK (privacy_setting IN ('PUBLIC', 'PRIVATE'))
);

-- ============================================================
-- 8. GAME_JOINERS (Junction: users <-> game_listings + team info)
-- ============================================================
CREATE TABLE game_joiners (
    user_id                     BIGINT      NOT NULL,
    game_listing_id             BIGINT      NOT NULL,
    team                        CHAR(1)  NOT NULL,
    format_position_id          BIGINT      NULL,
    alt_format_position_id      BIGINT      NULL,
    status                      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at                  DATETIME2   NOT NULL DEFAULT GETDATE(),
    updated_at                  DATETIME2   NOT NULL DEFAULT GETDATE(),

    CONSTRAINT PK_game_joiners PRIMARY KEY (user_id, game_listing_id),
    CONSTRAINT FK_game_joiners_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT FK_game_joiners_listing FOREIGN KEY (game_listing_id) REFERENCES game_listings(game_listing_id) ON DELETE CASCADE,
    CONSTRAINT CK_game_joiners_team CHECK (team IN ('A', 'B')),
    CONSTRAINT CK_game_joiners_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'LOCKED', 'LEFT'))
);

-- ============================================================
-- 9. SESSIONS (Depends on: game_listings) - 1:1 relationship
-- ============================================================
CREATE TABLE sessions (
    session_id      BIGINT IDENTITY(1,1) NOT NULL,
    game_listing_id BIGINT               NOT NULL,
    session_date    DATETIME2            NOT NULL,
    location        VARCHAR(200)         NOT NULL,
    created_at      DATETIME2            NOT NULL DEFAULT GETDATE(),
    updated_at      DATETIME2            NOT NULL DEFAULT GETDATE(),

    CONSTRAINT PK_sessions PRIMARY KEY (session_id),
    CONSTRAINT FK_sessions_listing FOREIGN KEY (game_listing_id) REFERENCES game_listings(game_listing_id) ON DELETE CASCADE,
    CONSTRAINT UQ_sessions_listing UNIQUE (game_listing_id)
);

-- ============================================================
-- 10. MATCH_RESULTS (Depends on: game_listings) - 1:1 relationship
-- ============================================================
CREATE TABLE match_results (
    match_result_id BIGINT IDENTITY(1,1) NOT NULL,
    game_listing_id BIGINT               NOT NULL,
    team_a_score    INT                  NOT NULL,
    team_b_score    INT                  NOT NULL,
    winners         VARCHAR(10)          NOT NULL,
    created_at      DATETIME2            NOT NULL DEFAULT GETDATE(),
    updated_at      DATETIME2            NOT NULL DEFAULT GETDATE(),
    created_by      VARCHAR(50)          NULL,
    updated_by      VARCHAR(50)          NULL,

    CONSTRAINT PK_match_results PRIMARY KEY (match_result_id),
    CONSTRAINT FK_match_results_listing FOREIGN KEY (game_listing_id) REFERENCES game_listings(game_listing_id) ON DELETE CASCADE,
    CONSTRAINT UQ_match_results_listing UNIQUE (game_listing_id),
    CONSTRAINT CK_match_results_score_a CHECK (team_a_score >= 0),
    CONSTRAINT CK_match_results_score_b CHECK (team_b_score >= 0),
    CONSTRAINT CK_match_results_winners CHECK (winners IN ('TEAM_A', 'TEAM_B', 'DRAW'))
);

-- ============================================================
-- 11. POSTS (Depends on: users)
-- ============================================================
CREATE TABLE posts (
    post_id         BIGINT IDENTITY(1,1) NOT NULL,
    user_id         BIGINT               NOT NULL,
    content         VARCHAR(500)         NOT NULL,
    privacy_setting VARCHAR(10)          NOT NULL DEFAULT 'PUBLIC',
    created_at      DATETIME2            NOT NULL DEFAULT GETDATE(),
    updated_at      DATETIME2            NOT NULL DEFAULT GETDATE(),
    created_by      VARCHAR(50)          NULL,
    updated_by      VARCHAR(50)          NULL,

    CONSTRAINT PK_posts PRIMARY KEY (post_id),
    CONSTRAINT FK_posts_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT CK_posts_privacy CHECK (privacy_setting IN ('PUBLIC', 'FOLLOWERS'))
);

-- ============================================================
-- 12. COMMENTS (Depends on: users, posts)
-- ============================================================
CREATE TABLE comments (
    comment_id      BIGINT IDENTITY(1,1) NOT NULL,
    user_id         BIGINT               NOT NULL,
    post_id         BIGINT               NOT NULL,
    text            VARCHAR(250)         NOT NULL,
    created_at      DATETIME2            NOT NULL DEFAULT GETDATE(),
    updated_at      DATETIME2            NOT NULL DEFAULT GETDATE(),

    CONSTRAINT PK_comments PRIMARY KEY (comment_id),
    CONSTRAINT FK_comments_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE NO ACTION,
    CONSTRAINT FK_comments_post FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE CASCADE
);

-- ============================================================
-- 13. LIKES (Junction: users <-> posts)
-- ============================================================
CREATE TABLE likes (
    user_id     BIGINT NOT NULL,
    post_id     BIGINT NOT NULL,
    created_at  DATETIME2 NOT NULL DEFAULT GETDATE(),

    CONSTRAINT PK_likes PRIMARY KEY (user_id, post_id),
    CONSTRAINT FK_likes_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE NO ACTION,
    CONSTRAINT FK_likes_post FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE CASCADE
);

-- ============================================================
-- 14. FOLLOWS (Self-referencing junction on users)
-- ============================================================
CREATE TABLE follows (
    follower_user_id    BIGINT    NOT NULL,
    followed_user_id    BIGINT    NOT NULL,
    created_at          DATETIME2 NOT NULL DEFAULT GETDATE(),

    CONSTRAINT PK_follows PRIMARY KEY (follower_user_id, followed_user_id),
    CONSTRAINT FK_follows_follower FOREIGN KEY (follower_user_id) REFERENCES users(user_id) ON DELETE NO ACTION,
    CONSTRAINT FK_follows_followed FOREIGN KEY (followed_user_id) REFERENCES users(user_id) ON DELETE NO ACTION,
    CONSTRAINT CK_follows_no_self CHECK (follower_user_id != followed_user_id)
);

-- ============================================================
-- 15. NOTIFICATIONS (Depends on: users)
-- ============================================================
CREATE TABLE notifications (
    notification_id     BIGINT IDENTITY(1,1) NOT NULL,
    recipient_id        BIGINT               NOT NULL,
    text                VARCHAR(300)         NOT NULL,
    notification_type   VARCHAR(30)          NOT NULL,
    is_read             BIT                  NOT NULL DEFAULT 0,
    created_at          DATETIME2            NOT NULL DEFAULT GETDATE(),
    updated_at          DATETIME2            NOT NULL DEFAULT GETDATE(),

    CONSTRAINT PK_notifications PRIMARY KEY (notification_id),
    CONSTRAINT FK_notifications_recipient FOREIGN KEY (recipient_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT CK_notifications_type CHECK (notification_type IN (
        'FOLLOW_NEW', 'JOIN_REQUEST_RECEIVED', 'JOIN_ACCEPTED', 'JOIN_REJECTED',
        'GAME_REMINDER', 'MATCH_RESULT_POSTED', 'LISTING_CANCELLED', 'LISTING_INVITE'
    ))
);

-- ============================================================
-- 16. REPORTS (Depends on: users)
-- ============================================================
CREATE TABLE reports (
    report_id       BIGINT IDENTITY(1,1) NOT NULL,
    reporter_id     BIGINT               NOT NULL,
    reference_id    BIGINT               NOT NULL,
    report_type     VARCHAR(10)          NOT NULL,
    report_reason   VARCHAR(50)          NOT NULL,
    content         VARCHAR(200)         NULL,
    status          VARCHAR(20)          NOT NULL DEFAULT 'PENDING',
    created_at      DATETIME2            NOT NULL DEFAULT GETDATE(),
    updated_at      DATETIME2            NOT NULL DEFAULT GETDATE(),

    CONSTRAINT PK_reports PRIMARY KEY (report_id),
    CONSTRAINT FK_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users(user_id) ON DELETE NO ACTION,
    CONSTRAINT CK_reports_type CHECK (report_type IN ('USER', 'POST')),
    CONSTRAINT CK_reports_status CHECK (status IN ('PENDING', 'DISMISSED', 'ACTIONED'))
);

-- ============================================================
-- AUDIT LOG TABLE (Cross-cutting concern)
-- ============================================================
CREATE TABLE audit_log (
    audit_id        BIGINT IDENTITY(1,1) NOT NULL,
    entity_type     VARCHAR(50)          NOT NULL,
    entity_id       BIGINT               NOT NULL,
    action          VARCHAR(20)          NOT NULL,
    performed_by    VARCHAR(50)          NOT NULL,
    performed_at    DATETIME2            NOT NULL DEFAULT GETDATE(),
    old_values      NVARCHAR(MAX)        NULL,
    new_values      NVARCHAR(MAX)        NULL,

    CONSTRAINT PK_audit_log PRIMARY KEY (audit_id),
    CONSTRAINT CK_audit_log_action CHECK (action IN ('CREATE', 'UPDATE', 'DELETE'))
);
