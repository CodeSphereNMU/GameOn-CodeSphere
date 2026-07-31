-- V1: Reproduces the existing GameOnDB schema from the Spring Boot/Hibernate phase.
-- Source: docs/Database/GameOnDB-current-schema.sql (UTF-16LE reference).
-- This file is UTF-8 for Flyway compatibility.
--
-- Every statement uses IF NOT EXISTS guards so it is safe to run against:
--   (a) a completely empty database, or
--   (b) a database that already contains these tables (via baselineOnMigrate).
--
-- WARNING: IF NOT EXISTS guards do not correct structural differences.
-- If a table exists but differs from this definition, no change is made.

-- ============================================================
-- TABLES
-- ============================================================

-- dbo.users
IF OBJECT_ID('dbo.users', 'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[users] (
        [user_id] [bigint] IDENTITY(1,1) NOT NULL,
        [password] [varchar](255) NOT NULL,
        [type_of_user] [varchar](255) NULL,
        [username] [varchar](255) NOT NULL,
    PRIMARY KEY CLUSTERED ([user_id] ASC)
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF,
              ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
        ON [PRIMARY],
    CONSTRAINT [UKr43af9ap4edm43mmtq01oddj6] UNIQUE NONCLUSTERED ([username] ASC)
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF,
              ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
        ON [PRIMARY]
    ) ON [PRIMARY];
END;
GO

-- dbo.sport
IF OBJECT_ID('dbo.sport', 'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[sport] (
        [sport_id] [bigint] IDENTITY(1,1) NOT NULL,
        [sport_name] [varchar](255) NOT NULL,
    PRIMARY KEY CLUSTERED ([sport_id] ASC)
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF,
              ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
        ON [PRIMARY]
    ) ON [PRIMARY];
END;
GO

-- dbo.sport_format
IF OBJECT_ID('dbo.sport_format', 'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[sport_format] (
        [format_id] [bigint] IDENTITY(1,1) NOT NULL,
        [format_name] [varchar](255) NULL,
        [has_positions] [bit] NOT NULL,
        [no_players] [int] NULL,
        [sport_id] [bigint] NOT NULL,
    PRIMARY KEY CLUSTERED ([format_id] ASC)
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF,
              ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
        ON [PRIMARY]
    ) ON [PRIMARY];
END;
GO

-- dbo.position
IF OBJECT_ID('dbo.position', 'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[position] (
        [position_id] [bigint] IDENTITY(1,1) NOT NULL,
        [position_name] [varchar](255) NULL,
    PRIMARY KEY CLUSTERED ([position_id] ASC)
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF,
              ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
        ON [PRIMARY]
    ) ON [PRIMARY];
END;
GO

-- dbo.format_position
IF OBJECT_ID('dbo.format_position', 'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[format_position] (
        [position_id] [bigint] NOT NULL,
        [format_id] [bigint] NOT NULL,
    PRIMARY KEY CLUSTERED ([format_id] ASC, [position_id] ASC)
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF,
              ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
        ON [PRIMARY]
    ) ON [PRIMARY];
END;
GO

-- dbo.user_sport_profile
IF OBJECT_ID('dbo.user_sport_profile', 'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[user_sport_profile] (
        [losses] [int] NULL,
        [skill_level] [varchar](255) NULL,
        [wins] [int] NULL,
        [sport_id] [bigint] NOT NULL,
        [user_id] [bigint] NOT NULL,
    PRIMARY KEY CLUSTERED ([sport_id] ASC, [user_id] ASC)
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF,
              ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
        ON [PRIMARY]
    ) ON [PRIMARY];
END;
GO

-- dbo.follow
IF OBJECT_ID('dbo.follow', 'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[follow] (
        [followed_user_id] [bigint] NOT NULL,
        [follower_user_id] [bigint] NOT NULL,
    PRIMARY KEY CLUSTERED ([followed_user_id] ASC, [follower_user_id] ASC)
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF,
              ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
        ON [PRIMARY]
    ) ON [PRIMARY];
END;
GO

-- dbo.game_listing
IF OBJECT_ID('dbo.game_listing', 'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[game_listing] (
        [game_listing_id] [bigint] IDENTITY(1,1) NOT NULL,
        [date] [datetime2](7) NULL,
        [is_completed] [bit] NOT NULL,
        [is_private] [bit] NOT NULL,
        [location] [varchar](255) NULL,
        [skill_level] [varchar](255) NULL,
        [creator_id] [bigint] NOT NULL,
        [format_id] [bigint] NOT NULL,
    PRIMARY KEY CLUSTERED ([game_listing_id] ASC)
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF,
              ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
        ON [PRIMARY]
    ) ON [PRIMARY];
END;
GO

-- dbo.game_joiner
IF OBJECT_ID('dbo.game_joiner', 'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[game_joiner] (
        [alternate_format_position] [varchar](255) NULL,
        [status] [varchar](255) NULL,
        [team] [varchar](255) NULL,
        [format_id] [bigint] NULL,
        [position_id] [bigint] NULL,
        [game_listing_id] [bigint] NOT NULL,
        [user_id] [bigint] NOT NULL,
    PRIMARY KEY CLUSTERED ([game_listing_id] ASC, [user_id] ASC)
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF,
              ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
        ON [PRIMARY]
    ) ON [PRIMARY];
END;
GO

-- dbo.match_result
IF OBJECT_ID('dbo.match_result', 'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[match_result] (
        [match_result_id] [bigint] IDENTITY(1,1) NOT NULL,
        [teamascore] [int] NULL,
        [teambscore] [int] NULL,
        [winners] [varchar](255) NULL,
        [game_listing_id] [bigint] NOT NULL,
    PRIMARY KEY CLUSTERED ([match_result_id] ASC)
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF,
              ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
        ON [PRIMARY],
    CONSTRAINT [UKdp32kabjjko32nypvrttr1pme] UNIQUE NONCLUSTERED ([game_listing_id] ASC)
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF,
              ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
        ON [PRIMARY]
    ) ON [PRIMARY];
END;
GO

-- dbo.notification
IF OBJECT_ID('dbo.notification', 'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[notification] (
        [notification_id] [bigint] IDENTITY(1,1) NOT NULL,
        [is_read] [bit] NULL,
        [text] [varchar](255) NULL,
        [type_of_notification] [varchar](255) NULL,
        [recipient_id] [bigint] NOT NULL,
    PRIMARY KEY CLUSTERED ([notification_id] ASC)
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF,
              ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
        ON [PRIMARY]
    ) ON [PRIMARY];
END;
GO

-- dbo.post
IF OBJECT_ID('dbo.post', 'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[post] (
        [post_id] [bigint] IDENTITY(1,1) NOT NULL,
        [content] [text] NULL,
        [privacy_settings] [varchar](255) NULL,
        [user_id] [bigint] NOT NULL,
    PRIMARY KEY CLUSTERED ([post_id] ASC)
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF,
              ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
        ON [PRIMARY]
    ) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY];
END;
GO

-- dbo.user_comments
IF OBJECT_ID('dbo.user_comments', 'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[user_comments] (
        [comment_id] [bigint] IDENTITY(1,1) NOT NULL,
        [text] [text] NULL,
        [post_id] [bigint] NOT NULL,
        [user_id] [bigint] NOT NULL,
    PRIMARY KEY CLUSTERED ([comment_id] ASC)
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF,
              ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
        ON [PRIMARY]
    ) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY];
END;
GO

-- dbo.user_likes
IF OBJECT_ID('dbo.user_likes', 'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[user_likes] (
        [post_id] [bigint] NOT NULL,
        [user_id] [bigint] NOT NULL,
    PRIMARY KEY CLUSTERED ([post_id] ASC, [user_id] ASC)
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF,
              ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
        ON [PRIMARY]
    ) ON [PRIMARY];
END;
GO

-- dbo.report
IF OBJECT_ID('dbo.report', 'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[report] (
        [report_id] [bigint] IDENTITY(1,1) NOT NULL,
        [content] [text] NULL,
        [report_reason] [varchar](255) NULL,
        [status] [varchar](255) NULL,
        [reported_post_id] [bigint] NULL,
        [reported_user_id] [bigint] NULL,
        [reporter_id] [bigint] NOT NULL,
    PRIMARY KEY CLUSTERED ([report_id] ASC)
        WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF,
              ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
        ON [PRIMARY]
    ) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY];
END;
GO

-- ============================================================
-- FOREIGN KEY CONSTRAINTS
-- ============================================================

-- follow → users
IF OBJECT_ID('dbo.FK5591hwflkyq3kwiaij7h3wfes', 'F') IS NULL
    ALTER TABLE [dbo].[follow] WITH CHECK ADD CONSTRAINT [FK5591hwflkyq3kwiaij7h3wfes]
        FOREIGN KEY([follower_user_id]) REFERENCES [dbo].[users] ([user_id]);
GO
IF OBJECT_ID('dbo.FKelnr125h8iqx27118xlg57e0b', 'F') IS NULL
    ALTER TABLE [dbo].[follow] WITH CHECK ADD CONSTRAINT [FKelnr125h8iqx27118xlg57e0b]
        FOREIGN KEY([followed_user_id]) REFERENCES [dbo].[users] ([user_id]);
GO

-- format_position → position, sport_format
IF OBJECT_ID('dbo.FK2homa583mmipx2h37vptb1gdk', 'F') IS NULL
    ALTER TABLE [dbo].[format_position] WITH CHECK ADD CONSTRAINT [FK2homa583mmipx2h37vptb1gdk]
        FOREIGN KEY([position_id]) REFERENCES [dbo].[position] ([position_id]);
GO
IF OBJECT_ID('dbo.FK9kc3qk5m2qbuyr7irg4b5go5m', 'F') IS NULL
    ALTER TABLE [dbo].[format_position] WITH CHECK ADD CONSTRAINT [FK9kc3qk5m2qbuyr7irg4b5go5m]
        FOREIGN KEY([format_id]) REFERENCES [dbo].[sport_format] ([format_id]);
GO

-- game_joiner → game_listing, format_position, users
IF OBJECT_ID('dbo.FK14p9s7ado233gge8mc56b9v5a', 'F') IS NULL
    ALTER TABLE [dbo].[game_joiner] WITH CHECK ADD CONSTRAINT [FK14p9s7ado233gge8mc56b9v5a]
        FOREIGN KEY([game_listing_id]) REFERENCES [dbo].[game_listing] ([game_listing_id]);
GO
IF OBJECT_ID('dbo.FKflc26oacgqui82woop4gcr4me', 'F') IS NULL
    ALTER TABLE [dbo].[game_joiner] WITH CHECK ADD CONSTRAINT [FKflc26oacgqui82woop4gcr4me]
        FOREIGN KEY([format_id], [position_id]) REFERENCES [dbo].[format_position] ([format_id], [position_id]);
GO
IF OBJECT_ID('dbo.FKpuaq35b9nwve35gsfsaycgxa8', 'F') IS NULL
    ALTER TABLE [dbo].[game_joiner] WITH CHECK ADD CONSTRAINT [FKpuaq35b9nwve35gsfsaycgxa8]
        FOREIGN KEY([user_id]) REFERENCES [dbo].[users] ([user_id]);
GO

-- game_listing → users, sport_format
IF OBJECT_ID('dbo.FK6s04n1jbko1num4tunmhgd37', 'F') IS NULL
    ALTER TABLE [dbo].[game_listing] WITH CHECK ADD CONSTRAINT [FK6s04n1jbko1num4tunmhgd37]
        FOREIGN KEY([creator_id]) REFERENCES [dbo].[users] ([user_id]);
GO
IF OBJECT_ID('dbo.FKk58xg8h3s5xlecfix6c1b6b0o', 'F') IS NULL
    ALTER TABLE [dbo].[game_listing] WITH CHECK ADD CONSTRAINT [FKk58xg8h3s5xlecfix6c1b6b0o]
        FOREIGN KEY([format_id]) REFERENCES [dbo].[sport_format] ([format_id]);
GO

-- match_result → game_listing
IF OBJECT_ID('dbo.FKm2tcc64dvf3kh3sbsvqqlpmif', 'F') IS NULL
    ALTER TABLE [dbo].[match_result] WITH CHECK ADD CONSTRAINT [FKm2tcc64dvf3kh3sbsvqqlpmif]
        FOREIGN KEY([game_listing_id]) REFERENCES [dbo].[game_listing] ([game_listing_id]);
GO

-- notification → users
IF OBJECT_ID('dbo.FKfcyn9rsga73dqnorl7owfyl4a', 'F') IS NULL
    ALTER TABLE [dbo].[notification] WITH CHECK ADD CONSTRAINT [FKfcyn9rsga73dqnorl7owfyl4a]
        FOREIGN KEY([recipient_id]) REFERENCES [dbo].[users] ([user_id]);
GO

-- post → users
IF OBJECT_ID('dbo.FK7ky67sgi7k0ayf22652f7763r', 'F') IS NULL
    ALTER TABLE [dbo].[post] WITH CHECK ADD CONSTRAINT [FK7ky67sgi7k0ayf22652f7763r]
        FOREIGN KEY([user_id]) REFERENCES [dbo].[users] ([user_id]);
GO

-- report → users, post
IF OBJECT_ID('dbo.FK2fm8nu7yscahr6sbhhgw082mp', 'F') IS NULL
    ALTER TABLE [dbo].[report] WITH CHECK ADD CONSTRAINT [FK2fm8nu7yscahr6sbhhgw082mp]
        FOREIGN KEY([reported_user_id]) REFERENCES [dbo].[users] ([user_id]);
GO
IF OBJECT_ID('dbo.FKqbhdxqd3ly7fkhly5nrl2j93k', 'F') IS NULL
    ALTER TABLE [dbo].[report] WITH CHECK ADD CONSTRAINT [FKqbhdxqd3ly7fkhly5nrl2j93k]
        FOREIGN KEY([reporter_id]) REFERENCES [dbo].[users] ([user_id]);
GO
IF OBJECT_ID('dbo.FKtc6m2e9bsw8xsy4una2hpboa', 'F') IS NULL
    ALTER TABLE [dbo].[report] WITH CHECK ADD CONSTRAINT [FKtc6m2e9bsw8xsy4una2hpboa]
        FOREIGN KEY([reported_post_id]) REFERENCES [dbo].[post] ([post_id]);
GO

-- sport_format → sport
IF OBJECT_ID('dbo.FKe62ks4kd0nuisw4nbp95169ck', 'F') IS NULL
    ALTER TABLE [dbo].[sport_format] WITH CHECK ADD CONSTRAINT [FKe62ks4kd0nuisw4nbp95169ck]
        FOREIGN KEY([sport_id]) REFERENCES [dbo].[sport] ([sport_id]);
GO

-- user_comments → post, users
IF OBJECT_ID('dbo.FKlx37drs82c71voerm0vs12312', 'F') IS NULL
    ALTER TABLE [dbo].[user_comments] WITH CHECK ADD CONSTRAINT [FKlx37drs82c71voerm0vs12312]
        FOREIGN KEY([post_id]) REFERENCES [dbo].[post] ([post_id]);
GO
IF OBJECT_ID('dbo.FKosv4oqe19o8flc6ps9yk535tx', 'F') IS NULL
    ALTER TABLE [dbo].[user_comments] WITH CHECK ADD CONSTRAINT [FKosv4oqe19o8flc6ps9yk535tx]
        FOREIGN KEY([user_id]) REFERENCES [dbo].[users] ([user_id]);
GO

-- user_likes → users, post
IF OBJECT_ID('dbo.FK6aog39hkl1hs1amxef5i9g4fv', 'F') IS NULL
    ALTER TABLE [dbo].[user_likes] WITH CHECK ADD CONSTRAINT [FK6aog39hkl1hs1amxef5i9g4fv]
        FOREIGN KEY([user_id]) REFERENCES [dbo].[users] ([user_id]);
GO
IF OBJECT_ID('dbo.FKqs24xm1lynicrn46yomgykywS', 'F') IS NULL
    ALTER TABLE [dbo].[user_likes] WITH CHECK ADD CONSTRAINT [FKqs24xm1lynicrn46yomgykywS]
        FOREIGN KEY([post_id]) REFERENCES [dbo].[post] ([post_id]);
GO

-- user_sport_profile → sport, users
IF OBJECT_ID('dbo.FKk8y1usmkc74ru76wlorvii9wd', 'F') IS NULL
    ALTER TABLE [dbo].[user_sport_profile] WITH CHECK ADD CONSTRAINT [FKk8y1usmkc74ru76wlorvii9wd]
        FOREIGN KEY([sport_id]) REFERENCES [dbo].[sport] ([sport_id]);
GO
IF OBJECT_ID('dbo.FKqve0xkqqyec5ruepgv1pvwcj8', 'F') IS NULL
    ALTER TABLE [dbo].[user_sport_profile] WITH CHECK ADD CONSTRAINT [FKqve0xkqqyec5ruepgv1pvwcj8]
        FOREIGN KEY([user_id]) REFERENCES [dbo].[users] ([user_id]);
GO
