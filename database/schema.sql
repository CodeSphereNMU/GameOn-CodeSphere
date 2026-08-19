-- ============================================================
-- GameOnDb Schema Script
-- Purpose: Creates all database tables for the GameOn-CodeSphere
--          application on Microsoft SQL Server.
-- Usage:   Run drop-all.sql first, then execute this script.
-- Order:   Tables are created in dependency order (Wave 1-5)
--          so that referenced tables exist before referencing tables.
-- ============================================================

-- ============================================================
-- Wave 1: Independent tables (no foreign key dependencies)
-- ============================================================

-- [User] table (bracket-escaped: "User" is a reserved word)
CREATE TABLE [User] (
    user_id INT IDENTITY(1,1) PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    bio VARCHAR(500) NULL,
    profile_image_url VARCHAR(255) NULL,
    created_at DATETIME DEFAULT GETDATE(),
    is_active BIT DEFAULT 1,
    CONSTRAINT UQ_User_Username UNIQUE (username),
    CONSTRAINT UQ_User_Email UNIQUE (email)
);

-- Sport table
CREATE TABLE Sport (
    sport_id INT IDENTITY(1,1) PRIMARY KEY,
    sport_name VARCHAR(50) NOT NULL,
    description VARCHAR(255) NULL,
    CONSTRAINT UQ_Sport_SportName UNIQUE (sport_name)
);

-- Position table
CREATE TABLE Position (
    position_id INT IDENTITY(1,1) PRIMARY KEY,
    position_name VARCHAR(50) NOT NULL
);

-- ============================================================
-- Wave 2: Tables depending on Wave 1
-- ============================================================

-- SportFormat table
CREATE TABLE SportFormat (
    format_id INT IDENTITY(1,1) PRIMARY KEY,
    sport_id INT NOT NULL,
    format_name VARCHAR(50) NOT NULL,
    max_players INT NOT NULL,
    min_players INT NOT NULL,
    CONSTRAINT FK_SportFormat_Sport FOREIGN KEY (sport_id)
        REFERENCES Sport(sport_id)
);

-- ============================================================
-- Wave 3: Tables depending on Wave 1 and Wave 2
-- ============================================================

-- FormatPosition table
CREATE TABLE FormatPosition (
    format_position_id INT IDENTITY(1,1) PRIMARY KEY,
    format_id INT NOT NULL,
    position_id INT NOT NULL,
    CONSTRAINT FK_FormatPosition_SportFormat FOREIGN KEY (format_id)
        REFERENCES SportFormat(format_id),
    CONSTRAINT FK_FormatPosition_Position FOREIGN KEY (position_id)
        REFERENCES Position(position_id)
);

-- UserSportProfile table
CREATE TABLE UserSportProfile (
    user_sport_id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL,
    sport_id INT NOT NULL,
    skill_level VARCHAR(20) NULL,
    preferred_position_id INT NULL,
    CONSTRAINT FK_UserSportProfile_User FOREIGN KEY (user_id)
        REFERENCES [User](user_id),
    CONSTRAINT FK_UserSportProfile_Sport FOREIGN KEY (sport_id)
        REFERENCES Sport(sport_id),
    CONSTRAINT FK_UserSportProfile_Position FOREIGN KEY (preferred_position_id)
        REFERENCES Position(position_id)
);

-- GameListing table
CREATE TABLE GameListing (
    listing_id INT IDENTITY(1,1) PRIMARY KEY,
    host_user_id INT NOT NULL,
    sport_id INT NOT NULL,
    format_id INT NOT NULL,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    location VARCHAR(200) NOT NULL,
    game_date DATE NOT NULL,
    game_time TIME NOT NULL,
    max_players INT NOT NULL,
    status VARCHAR(20) DEFAULT 'Open',
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_GameListing_User FOREIGN KEY (host_user_id)
        REFERENCES [User](user_id),
    CONSTRAINT FK_GameListing_Sport FOREIGN KEY (sport_id)
        REFERENCES Sport(sport_id),
    CONSTRAINT FK_GameListing_SportFormat FOREIGN KEY (format_id)
        REFERENCES SportFormat(format_id)
);

-- Post table
CREATE TABLE Post (
    post_id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL,
    content NVARCHAR(MAX) NOT NULL,
    image_url VARCHAR(255) NULL,
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME NULL,
    CONSTRAINT FK_Post_User FOREIGN KEY (user_id)
        REFERENCES [User](user_id)
);

-- ============================================================
-- Wave 4: Tables depending on Wave 1-3 (GameListing, Post, [User])
-- ============================================================

-- GameJoiner table
CREATE TABLE GameJoiner (
    joiner_id INT IDENTITY(1,1) PRIMARY KEY,
    listing_id INT NOT NULL,
    user_id INT NOT NULL,
    status VARCHAR(20) DEFAULT 'Pending',
    requested_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_GameJoiner_GameListing FOREIGN KEY (listing_id)
        REFERENCES GameListing(listing_id),
    CONSTRAINT FK_GameJoiner_User FOREIGN KEY (user_id)
        REFERENCES [User](user_id)
);

-- Session table
CREATE TABLE Session (
    session_id INT IDENTITY(1,1) PRIMARY KEY,
    listing_id INT NOT NULL,
    confirmed_at DATETIME DEFAULT GETDATE(),
    status VARCHAR(20) DEFAULT 'Scheduled',
    CONSTRAINT FK_Session_GameListing FOREIGN KEY (listing_id)
        REFERENCES GameListing(listing_id)
);

-- Comment table
CREATE TABLE Comment (
    comment_id INT IDENTITY(1,1) PRIMARY KEY,
    post_id INT NOT NULL,
    user_id INT NOT NULL,
    content VARCHAR(500) NOT NULL,
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_Comment_Post FOREIGN KEY (post_id)
        REFERENCES Post(post_id),
    CONSTRAINT FK_Comment_User FOREIGN KEY (user_id)
        REFERENCES [User](user_id)
);

-- [Like] table (bracket-escaped reserved word)
CREATE TABLE [Like] (
    like_id INT IDENTITY(1,1) PRIMARY KEY,
    post_id INT NOT NULL,
    user_id INT NOT NULL,
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_Like_Post FOREIGN KEY (post_id)
        REFERENCES Post(post_id),
    CONSTRAINT FK_Like_User FOREIGN KEY (user_id)
        REFERENCES [User](user_id),
    CONSTRAINT UQ_Like_PostId_UserId UNIQUE (post_id, user_id)
);

-- [Follow] table (bracket-escaped reserved word)
CREATE TABLE [Follow] (
    follow_id INT IDENTITY(1,1) PRIMARY KEY,
    follower_id INT NOT NULL,
    following_id INT NOT NULL,
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_Follow_User_Follower FOREIGN KEY (follower_id)
        REFERENCES [User](user_id),
    CONSTRAINT FK_Follow_User_Following FOREIGN KEY (following_id)
        REFERENCES [User](user_id),
    CONSTRAINT UQ_Follow_FollowerId_FollowingId UNIQUE (follower_id, following_id),
    CONSTRAINT CHK_Follow_NoSelfFollow CHECK (follower_id <> following_id)
);

-- Notification table
CREATE TABLE Notification (
    notification_id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL,
    type VARCHAR(50) NOT NULL,
    reference_id INT NULL,
    message VARCHAR(255) NOT NULL,
    is_read BIT DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_Notification_User FOREIGN KEY (user_id)
        REFERENCES [User](user_id)
);

-- Report table
CREATE TABLE Report (
    report_id INT IDENTITY(1,1) PRIMARY KEY,
    reporter_id INT NOT NULL,
    reported_user_id INT NULL,
    reported_post_id INT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) DEFAULT 'Pending',
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_Report_User_Reporter FOREIGN KEY (reporter_id)
        REFERENCES [User](user_id),
    CONSTRAINT FK_Report_User_Reported FOREIGN KEY (reported_user_id)
        REFERENCES [User](user_id),
    CONSTRAINT FK_Report_Post FOREIGN KEY (reported_post_id)
        REFERENCES Post(post_id)
);

-- ============================================================
-- Wave 5: Tables depending on Wave 4 (Session)
-- ============================================================

-- MatchResult table
CREATE TABLE MatchResult (
    result_id INT IDENTITY(1,1) PRIMARY KEY,
    session_id INT NOT NULL,
    recorded_by INT NOT NULL,
    result_data NVARCHAR(MAX) NOT NULL,
    recorded_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME NULL,
    CONSTRAINT FK_MatchResult_Session FOREIGN KEY (session_id)
        REFERENCES Session(session_id),
    CONSTRAINT FK_MatchResult_User FOREIGN KEY (recorded_by)
        REFERENCES [User](user_id)
);
