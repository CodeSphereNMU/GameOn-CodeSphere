-- ============================================================
-- GameOn Database - V2__Seed_Data.sql
-- Description: Seed reference data for Sports, Formats, Positions, and Format-Position mappings
-- ============================================================

-- ============================================================
-- SPORTS (5 Sports)
-- ============================================================
SET IDENTITY_INSERT sports ON;

INSERT INTO sports (sport_id, sport_name, no_players) VALUES (1, 'Padel', 4);
INSERT INTO sports (sport_id, sport_name, no_players) VALUES (2, 'Tennis', 4);
INSERT INTO sports (sport_id, sport_name, no_players) VALUES (3, 'Basketball', 10);
INSERT INTO sports (sport_id, sport_name, no_players) VALUES (4, 'Football', 22);
INSERT INTO sports (sport_id, sport_name, no_players) VALUES (5, 'Rugby', 30);

SET IDENTITY_INSERT sports OFF;

-- ============================================================
-- POSITIONS (12 Positions)
-- ============================================================
SET IDENTITY_INSERT positions ON;

INSERT INTO positions (position_id, position_name) VALUES (1, 'Any Position');
INSERT INTO positions (position_id, position_name) VALUES (2, 'Goalkeeper');
INSERT INTO positions (position_id, position_name) VALUES (3, 'Defense');
INSERT INTO positions (position_id, position_name) VALUES (4, 'Midfield');
INSERT INTO positions (position_id, position_name) VALUES (5, 'Attack');
INSERT INTO positions (position_id, position_name) VALUES (6, 'Guard');
INSERT INTO positions (position_id, position_name) VALUES (7, 'Forward');
INSERT INTO positions (position_id, position_name) VALUES (8, 'Center');
INSERT INTO positions (position_id, position_name) VALUES (9, 'Scrumhalf');
INSERT INTO positions (position_id, position_name) VALUES (10, 'Flyhalf');
INSERT INTO positions (position_id, position_name) VALUES (11, 'Wing');
INSERT INTO positions (position_id, position_name) VALUES (12, 'Fullback');

SET IDENTITY_INSERT positions OFF;

-- ============================================================
-- SPORT FORMATS (10 Formats)
-- ============================================================
SET IDENTITY_INSERT sport_formats ON;

-- Padel Formats
INSERT INTO sport_formats (format_id, sport_id, format_name, no_players, has_positions) VALUES (1, 1, 'Doubles', 4, 0);

-- Tennis Formats
INSERT INTO sport_formats (format_id, sport_id, format_name, no_players, has_positions) VALUES (2, 2, 'Singles', 2, 0);
INSERT INTO sport_formats (format_id, sport_id, format_name, no_players, has_positions) VALUES (3, 2, 'Doubles', 4, 0);

-- Basketball Formats
INSERT INTO sport_formats (format_id, sport_id, format_name, no_players, has_positions) VALUES (4, 3, '3v3', 6, 1);
INSERT INTO sport_formats (format_id, sport_id, format_name, no_players, has_positions) VALUES (5, 3, '5v5', 10, 1);

-- Football Formats
INSERT INTO sport_formats (format_id, sport_id, format_name, no_players, has_positions) VALUES (6, 4, '5v5', 10, 1);
INSERT INTO sport_formats (format_id, sport_id, format_name, no_players, has_positions) VALUES (7, 4, '7v7', 14, 1);
INSERT INTO sport_formats (format_id, sport_id, format_name, no_players, has_positions) VALUES (8, 4, '11v11', 22, 1);

-- Rugby Formats
INSERT INTO sport_formats (format_id, sport_id, format_name, no_players, has_positions) VALUES (9, 5, '7s', 14, 1);
INSERT INTO sport_formats (format_id, sport_id, format_name, no_players, has_positions) VALUES (10, 5, '15s', 30, 1);

SET IDENTITY_INSERT sport_formats OFF;

-- ============================================================
-- FORMAT_POSITIONS (Which positions apply to which formats)
-- ============================================================

-- Basketball 3v3 (format_id = 4): Any, Guard, Forward, Center
INSERT INTO format_positions (format_id, position_id) VALUES (4, 1);
INSERT INTO format_positions (format_id, position_id) VALUES (4, 6);
INSERT INTO format_positions (format_id, position_id) VALUES (4, 7);
INSERT INTO format_positions (format_id, position_id) VALUES (4, 8);

-- Basketball 5v5 (format_id = 5): Any, Guard, Forward, Center
INSERT INTO format_positions (format_id, position_id) VALUES (5, 1);
INSERT INTO format_positions (format_id, position_id) VALUES (5, 6);
INSERT INTO format_positions (format_id, position_id) VALUES (5, 7);
INSERT INTO format_positions (format_id, position_id) VALUES (5, 8);

-- Football 5v5 (format_id = 6): Any, Goalkeeper, Defense, Midfield, Attack
INSERT INTO format_positions (format_id, position_id) VALUES (6, 1);
INSERT INTO format_positions (format_id, position_id) VALUES (6, 2);
INSERT INTO format_positions (format_id, position_id) VALUES (6, 3);
INSERT INTO format_positions (format_id, position_id) VALUES (6, 4);
INSERT INTO format_positions (format_id, position_id) VALUES (6, 5);

-- Football 7v7 (format_id = 7): Any, Goalkeeper, Defense, Midfield, Attack
INSERT INTO format_positions (format_id, position_id) VALUES (7, 1);
INSERT INTO format_positions (format_id, position_id) VALUES (7, 2);
INSERT INTO format_positions (format_id, position_id) VALUES (7, 3);
INSERT INTO format_positions (format_id, position_id) VALUES (7, 4);
INSERT INTO format_positions (format_id, position_id) VALUES (7, 5);

-- Football 11v11 (format_id = 8): Any, Goalkeeper, Defense, Midfield, Attack
INSERT INTO format_positions (format_id, position_id) VALUES (8, 1);
INSERT INTO format_positions (format_id, position_id) VALUES (8, 2);
INSERT INTO format_positions (format_id, position_id) VALUES (8, 3);
INSERT INTO format_positions (format_id, position_id) VALUES (8, 4);
INSERT INTO format_positions (format_id, position_id) VALUES (8, 5);

-- Rugby 7s (format_id = 9): Any, Scrumhalf, Flyhalf, Wing, Fullback
INSERT INTO format_positions (format_id, position_id) VALUES (9, 1);
INSERT INTO format_positions (format_id, position_id) VALUES (9, 9);
INSERT INTO format_positions (format_id, position_id) VALUES (9, 10);
INSERT INTO format_positions (format_id, position_id) VALUES (9, 11);
INSERT INTO format_positions (format_id, position_id) VALUES (9, 12);

-- Rugby 15s (format_id = 10): Any, Defense, Scrumhalf, Flyhalf, Wing, Fullback
INSERT INTO format_positions (format_id, position_id) VALUES (10, 1);
INSERT INTO format_positions (format_id, position_id) VALUES (10, 3);
INSERT INTO format_positions (format_id, position_id) VALUES (10, 9);
INSERT INTO format_positions (format_id, position_id) VALUES (10, 10);
INSERT INTO format_positions (format_id, position_id) VALUES (10, 11);
INSERT INTO format_positions (format_id, position_id) VALUES (10, 12);
