-- ============================================================
-- seed-data.sql
-- Inserts initial reference/lookup data into GameOnDb.
-- This script is idempotent — safe to re-run without causing
-- duplicate-key errors thanks to IF NOT EXISTS guards.
-- ============================================================

USE GameOnDb;
GO

-- ============================================================
-- Sport seed data
-- ============================================================

SET IDENTITY_INSERT Sport ON;

IF NOT EXISTS (SELECT 1 FROM Sport WHERE sport_id = 1)
    INSERT INTO Sport (sport_id, sport_name, description) VALUES (1, 'Football', 'Association football (soccer) played with a round ball');

IF NOT EXISTS (SELECT 1 FROM Sport WHERE sport_id = 2)
    INSERT INTO Sport (sport_id, sport_name, description) VALUES (2, 'Basketball', 'Indoor/outdoor court game played with a bouncing ball');

IF NOT EXISTS (SELECT 1 FROM Sport WHERE sport_id = 3)
    INSERT INTO Sport (sport_id, sport_name, description) VALUES (3, 'Cricket', 'Bat-and-ball game played between two teams of eleven');

IF NOT EXISTS (SELECT 1 FROM Sport WHERE sport_id = 4)
    INSERT INTO Sport (sport_id, sport_name, description) VALUES (4, 'Tennis', 'Racquet sport played individually or in pairs');

IF NOT EXISTS (SELECT 1 FROM Sport WHERE sport_id = 5)
    INSERT INTO Sport (sport_id, sport_name, description) VALUES (5, 'Rugby', 'Contact team sport originating from Rugby School');

SET IDENTITY_INSERT Sport OFF;
GO

-- ============================================================
-- SportFormat seed data
-- ============================================================

SET IDENTITY_INSERT SportFormat ON;

IF NOT EXISTS (SELECT 1 FROM SportFormat WHERE format_id = 1)
    INSERT INTO SportFormat (format_id, sport_id, format_name, min_players, max_players) VALUES (1, 1, '5-a-side', 10, 10);

IF NOT EXISTS (SELECT 1 FROM SportFormat WHERE format_id = 2)
    INSERT INTO SportFormat (format_id, sport_id, format_name, min_players, max_players) VALUES (2, 1, '7-a-side', 14, 14);

IF NOT EXISTS (SELECT 1 FROM SportFormat WHERE format_id = 3)
    INSERT INTO SportFormat (format_id, sport_id, format_name, min_players, max_players) VALUES (3, 1, '11-a-side', 22, 22);

IF NOT EXISTS (SELECT 1 FROM SportFormat WHERE format_id = 4)
    INSERT INTO SportFormat (format_id, sport_id, format_name, min_players, max_players) VALUES (4, 2, '3x3', 6, 6);

IF NOT EXISTS (SELECT 1 FROM SportFormat WHERE format_id = 5)
    INSERT INTO SportFormat (format_id, sport_id, format_name, min_players, max_players) VALUES (5, 2, '5v5', 10, 10);

IF NOT EXISTS (SELECT 1 FROM SportFormat WHERE format_id = 6)
    INSERT INTO SportFormat (format_id, sport_id, format_name, min_players, max_players) VALUES (6, 3, 'T20', 22, 22);

IF NOT EXISTS (SELECT 1 FROM SportFormat WHERE format_id = 7)
    INSERT INTO SportFormat (format_id, sport_id, format_name, min_players, max_players) VALUES (7, 3, 'ODI', 22, 22);

IF NOT EXISTS (SELECT 1 FROM SportFormat WHERE format_id = 8)
    INSERT INTO SportFormat (format_id, sport_id, format_name, min_players, max_players) VALUES (8, 4, 'Singles', 2, 2);

IF NOT EXISTS (SELECT 1 FROM SportFormat WHERE format_id = 9)
    INSERT INTO SportFormat (format_id, sport_id, format_name, min_players, max_players) VALUES (9, 4, 'Doubles', 4, 4);

IF NOT EXISTS (SELECT 1 FROM SportFormat WHERE format_id = 10)
    INSERT INTO SportFormat (format_id, sport_id, format_name, min_players, max_players) VALUES (10, 5, '7s', 14, 14);

IF NOT EXISTS (SELECT 1 FROM SportFormat WHERE format_id = 11)
    INSERT INTO SportFormat (format_id, sport_id, format_name, min_players, max_players) VALUES (11, 5, '15s', 30, 30);

SET IDENTITY_INSERT SportFormat OFF;
GO

-- ============================================================
-- Position seed data
-- ============================================================

SET IDENTITY_INSERT Position ON;

IF NOT EXISTS (SELECT 1 FROM Position WHERE position_id = 1)
    INSERT INTO Position (position_id, position_name) VALUES (1, 'Goalkeeper');

IF NOT EXISTS (SELECT 1 FROM Position WHERE position_id = 2)
    INSERT INTO Position (position_id, position_name) VALUES (2, 'Defender');

IF NOT EXISTS (SELECT 1 FROM Position WHERE position_id = 3)
    INSERT INTO Position (position_id, position_name) VALUES (3, 'Midfielder');

IF NOT EXISTS (SELECT 1 FROM Position WHERE position_id = 4)
    INSERT INTO Position (position_id, position_name) VALUES (4, 'Forward');

IF NOT EXISTS (SELECT 1 FROM Position WHERE position_id = 5)
    INSERT INTO Position (position_id, position_name) VALUES (5, 'Winger');

IF NOT EXISTS (SELECT 1 FROM Position WHERE position_id = 6)
    INSERT INTO Position (position_id, position_name) VALUES (6, 'Point Guard');

IF NOT EXISTS (SELECT 1 FROM Position WHERE position_id = 7)
    INSERT INTO Position (position_id, position_name) VALUES (7, 'Shooting Guard');

IF NOT EXISTS (SELECT 1 FROM Position WHERE position_id = 8)
    INSERT INTO Position (position_id, position_name) VALUES (8, 'Small Forward');

IF NOT EXISTS (SELECT 1 FROM Position WHERE position_id = 9)
    INSERT INTO Position (position_id, position_name) VALUES (9, 'Power Forward');

IF NOT EXISTS (SELECT 1 FROM Position WHERE position_id = 10)
    INSERT INTO Position (position_id, position_name) VALUES (10, 'Center');

IF NOT EXISTS (SELECT 1 FROM Position WHERE position_id = 11)
    INSERT INTO Position (position_id, position_name) VALUES (11, 'Bowler');

IF NOT EXISTS (SELECT 1 FROM Position WHERE position_id = 12)
    INSERT INTO Position (position_id, position_name) VALUES (12, 'Batsman');

IF NOT EXISTS (SELECT 1 FROM Position WHERE position_id = 13)
    INSERT INTO Position (position_id, position_name) VALUES (13, 'All-Rounder');

IF NOT EXISTS (SELECT 1 FROM Position WHERE position_id = 14)
    INSERT INTO Position (position_id, position_name) VALUES (14, 'Wicket-Keeper');

IF NOT EXISTS (SELECT 1 FROM Position WHERE position_id = 15)
    INSERT INTO Position (position_id, position_name) VALUES (15, 'Server');

IF NOT EXISTS (SELECT 1 FROM Position WHERE position_id = 16)
    INSERT INTO Position (position_id, position_name) VALUES (16, 'Returner');

IF NOT EXISTS (SELECT 1 FROM Position WHERE position_id = 17)
    INSERT INTO Position (position_id, position_name) VALUES (17, 'Fly-Half');

IF NOT EXISTS (SELECT 1 FROM Position WHERE position_id = 18)
    INSERT INTO Position (position_id, position_name) VALUES (18, 'Scrum-Half');

IF NOT EXISTS (SELECT 1 FROM Position WHERE position_id = 19)
    INSERT INTO Position (position_id, position_name) VALUES (19, 'Hooker');

IF NOT EXISTS (SELECT 1 FROM Position WHERE position_id = 20)
    INSERT INTO Position (position_id, position_name) VALUES (20, 'Prop');

IF NOT EXISTS (SELECT 1 FROM Position WHERE position_id = 21)
    INSERT INTO Position (position_id, position_name) VALUES (21, 'Lock');

IF NOT EXISTS (SELECT 1 FROM Position WHERE position_id = 22)
    INSERT INTO Position (position_id, position_name) VALUES (22, 'Flanker');

IF NOT EXISTS (SELECT 1 FROM Position WHERE position_id = 23)
    INSERT INTO Position (position_id, position_name) VALUES (23, 'Number 8');

IF NOT EXISTS (SELECT 1 FROM Position WHERE position_id = 24)
    INSERT INTO Position (position_id, position_name) VALUES (24, 'Fullback');

SET IDENTITY_INSERT Position OFF;
GO

-- ============================================================
-- FormatPosition seed data
-- ============================================================

SET IDENTITY_INSERT FormatPosition ON;

-- Football 5-a-side (format_id=1): Goalkeeper, Defender, Midfielder, Forward, Winger
IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 1)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (1, 1, 1);

IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 2)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (2, 1, 2);

IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 3)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (3, 1, 3);

IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 4)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (4, 1, 4);

IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 5)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (5, 1, 5);

-- Football 7-a-side (format_id=2): Goalkeeper, Defender, Midfielder, Forward, Winger
IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 6)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (6, 2, 1);

IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 7)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (7, 2, 2);

IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 8)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (8, 2, 3);

IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 9)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (9, 2, 4);

IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 10)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (10, 2, 5);

-- Football 11-a-side (format_id=3): Goalkeeper, Defender, Midfielder, Forward, Winger
IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 11)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (11, 3, 1);

IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 12)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (12, 3, 2);

IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 13)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (13, 3, 3);

IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 14)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (14, 3, 4);

IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 15)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (15, 3, 5);

-- Basketball 3x3 (format_id=4): Point Guard, Shooting Guard, Small Forward, Power Forward, Center
IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 16)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (16, 4, 6);

IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 17)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (17, 4, 7);

IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 18)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (18, 4, 8);

IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 19)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (19, 4, 9);

IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 20)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (20, 4, 10);

-- Basketball 5v5 (format_id=5): Point Guard, Shooting Guard, Small Forward, Power Forward, Center
IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 21)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (21, 5, 6);

IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 22)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (22, 5, 7);

IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 23)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (23, 5, 8);

IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 24)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (24, 5, 9);

IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 25)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (25, 5, 10);

-- Cricket T20 (format_id=6): Bowler, Batsman, All-Rounder, Wicket-Keeper
IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 26)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (26, 6, 11);

IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 27)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (27, 6, 12);

IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 28)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (28, 6, 13);

IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 29)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (29, 6, 14);

-- Cricket ODI (format_id=7): Bowler, Batsman, All-Rounder, Wicket-Keeper
IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 30)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (30, 7, 11);

IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 31)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (31, 7, 12);

IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 32)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (32, 7, 13);

IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 33)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (33, 7, 14);

-- Tennis Singles (format_id=8): Server, Returner
IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 34)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (34, 8, 15);

IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 35)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (35, 8, 16);

-- Tennis Doubles (format_id=9): Server, Returner
IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 36)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (36, 9, 15);

IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 37)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (37, 9, 16);

-- Rugby 7s (format_id=10): Fly-Half, Scrum-Half, Hooker, Prop, Lock, Flanker, Fullback
IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 38)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (38, 10, 17);

IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 39)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (39, 10, 18);

IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 40)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (40, 10, 19);

IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 41)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (41, 10, 20);

IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 42)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (42, 10, 21);

IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 43)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (43, 10, 22);

IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 44)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (44, 10, 24);

-- Rugby 15s (format_id=11): Fly-Half, Scrum-Half, Hooker, Prop, Lock, Flanker, Number 8, Fullback
IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 45)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (45, 11, 17);

IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 46)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (46, 11, 18);

IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 47)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (47, 11, 19);

IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 48)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (48, 11, 20);

IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 49)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (49, 11, 21);

IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 50)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (50, 11, 22);

IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 51)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (51, 11, 23);

IF NOT EXISTS (SELECT 1 FROM FormatPosition WHERE format_position_id = 52)
    INSERT INTO FormatPosition (format_position_id, format_id, position_id) VALUES (52, 11, 24);

SET IDENTITY_INSERT FormatPosition OFF;
GO
