-- ============================================================
-- GameOn Database - V3__Security_Data.sql
-- Description: Seed security-related data (roles, admin/moderator accounts, test users)
-- Passwords are BCrypt encoded: Test123 = $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
-- Admin123 = $2a$10$EqKcp1WFKDUmB2ZVXpFBQeFa0bYWfBq7kYUFKPKxDC2VjDKmMaPqW
-- ============================================================

-- ============================================================
-- TEST USER ACCOUNTS
-- All test passwords are BCrypt encoded
-- ============================================================

SET IDENTITY_INSERT users ON;

-- Regular Users (password: Test123)
INSERT INTO users (user_id, username, email, password, user_role, is_active, created_at, updated_at)
VALUES (1, 'Zane', 'zane@gameon.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'USER', 1, GETDATE(), GETDATE());

INSERT INTO users (user_id, username, email, password, user_role, is_active, created_at, updated_at)
VALUES (2, 'Lihlumelo', 'lihlumelo@gameon.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'USER', 1, GETDATE(), GETDATE());

INSERT INTO users (user_id, username, email, password, user_role, is_active, created_at, updated_at)
VALUES (3, 'Gerard', 'gerard@gameon.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'USER', 1, GETDATE(), GETDATE());

INSERT INTO users (user_id, username, email, password, user_role, is_active, created_at, updated_at)
VALUES (4, 'Robert', 'robert@gameon.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'USER', 1, GETDATE(), GETDATE());

-- Moderator Account (password: Admin123)
INSERT INTO users (user_id, username, email, password, user_role, is_active, created_at, updated_at)
VALUES (5, 'Moderator', 'moderator@gameon.com', '$2a$10$EqKcp1WFKDUmB2ZVXpFBQeFa0bYWfBq7kYUFKPKxDC2VjDKmMaPqW', 'MODERATOR', 1, GETDATE(), GETDATE());

-- Admin Account (password: Admin123)
INSERT INTO users (user_id, username, email, password, user_role, is_active, created_at, updated_at)
VALUES (6, 'Admin', 'admin@gameon.com', '$2a$10$EqKcp1WFKDUmB2ZVXpFBQeFa0bYWfBq7kYUFKPKxDC2VjDKmMaPqW', 'ADMIN', 1, GETDATE(), GETDATE());

SET IDENTITY_INSERT users OFF;

-- ============================================================
-- USER SPORT PROFILES (Test users have sports assigned)
-- ============================================================

-- Zane: Tennis (Advanced), Football (Intermediate)
INSERT INTO user_sport_profiles (user_id, sport_id, skill_level, wins, losses, win_percentage)
VALUES (1, 2, 'ADVANCED', 15, 5, 75.0);
INSERT INTO user_sport_profiles (user_id, sport_id, skill_level, wins, losses, win_percentage)
VALUES (1, 4, 'INTERMEDIATE', 8, 7, 53.3);

-- Lihlumelo: Football (Advanced), Basketball (Beginner)
INSERT INTO user_sport_profiles (user_id, sport_id, skill_level, wins, losses, win_percentage)
VALUES (2, 4, 'ADVANCED', 20, 3, 87.0);
INSERT INTO user_sport_profiles (user_id, sport_id, skill_level, wins, losses, win_percentage)
VALUES (2, 3, 'BEGINNER', 2, 5, 28.6);

-- Gerard: Basketball (Intermediate), Padel (Advanced)
INSERT INTO user_sport_profiles (user_id, sport_id, skill_level, wins, losses, win_percentage)
VALUES (3, 3, 'INTERMEDIATE', 10, 8, 55.6);
INSERT INTO user_sport_profiles (user_id, sport_id, skill_level, wins, losses, win_percentage)
VALUES (3, 1, 'ADVANCED', 12, 4, 75.0);

-- Robert: Tennis (Beginner), Padel (Intermediate)
INSERT INTO user_sport_profiles (user_id, sport_id, skill_level, wins, losses, win_percentage)
VALUES (4, 2, 'BEGINNER', 3, 10, 23.1);
INSERT INTO user_sport_profiles (user_id, sport_id, skill_level, wins, losses, win_percentage)
VALUES (4, 1, 'INTERMEDIATE', 6, 6, 50.0);

-- ============================================================
-- SAMPLE FOLLOW RELATIONSHIPS
-- ============================================================
INSERT INTO follows (follower_user_id, followed_user_id) VALUES (1, 2);
INSERT INTO follows (follower_user_id, followed_user_id) VALUES (1, 3);
INSERT INTO follows (follower_user_id, followed_user_id) VALUES (2, 1);
INSERT INTO follows (follower_user_id, followed_user_id) VALUES (2, 4);
INSERT INTO follows (follower_user_id, followed_user_id) VALUES (3, 1);
INSERT INTO follows (follower_user_id, followed_user_id) VALUES (3, 2);
INSERT INTO follows (follower_user_id, followed_user_id) VALUES (4, 1);
INSERT INTO follows (follower_user_id, followed_user_id) VALUES (4, 2);
INSERT INTO follows (follower_user_id, followed_user_id) VALUES (4, 3);
