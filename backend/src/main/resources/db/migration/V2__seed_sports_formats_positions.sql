-- V2: Seed confirmed sports, formats, positions, and format-position links.
-- All inserts use IF NOT EXISTS checks for safety.
-- Sports identified by sport_name.
-- Formats identified by sport + format_name (names like 'Doubles' occur under multiple sports).
-- Positions identified by position_name (reused across formats via format_position).

-- ============================================================
-- SPORTS (5)
-- ============================================================

IF NOT EXISTS (SELECT 1 FROM [dbo].[sport] WHERE [sport_name] = 'Padel')
    INSERT INTO [dbo].[sport] ([sport_name]) VALUES ('Padel');
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[sport] WHERE [sport_name] = 'Tennis')
    INSERT INTO [dbo].[sport] ([sport_name]) VALUES ('Tennis');
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[sport] WHERE [sport_name] = 'Basketball')
    INSERT INTO [dbo].[sport] ([sport_name]) VALUES ('Basketball');
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[sport] WHERE [sport_name] = 'Rugby')
    INSERT INTO [dbo].[sport] ([sport_name]) VALUES ('Rugby');
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[sport] WHERE [sport_name] = 'Football')
    INSERT INTO [dbo].[sport] ([sport_name]) VALUES ('Football');
GO

-- ============================================================
-- FORMATS (15)
-- ============================================================

-- Padel (1 format)
IF NOT EXISTS (SELECT 1 FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id WHERE s.sport_name = 'Padel' AND sf.format_name = 'Doubles')
    INSERT INTO [dbo].[sport_format] ([format_name], [has_positions], [no_players], [sport_id])
    SELECT 'Doubles', 0, 4, sport_id FROM [dbo].[sport] WHERE sport_name = 'Padel';
GO

-- Tennis (2 formats)
IF NOT EXISTS (SELECT 1 FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id WHERE s.sport_name = 'Tennis' AND sf.format_name = 'Singles')
    INSERT INTO [dbo].[sport_format] ([format_name], [has_positions], [no_players], [sport_id])
    SELECT 'Singles', 0, 2, sport_id FROM [dbo].[sport] WHERE sport_name = 'Tennis';
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id WHERE s.sport_name = 'Tennis' AND sf.format_name = 'Doubles')
    INSERT INTO [dbo].[sport_format] ([format_name], [has_positions], [no_players], [sport_id])
    SELECT 'Doubles', 0, 4, sport_id FROM [dbo].[sport] WHERE sport_name = 'Tennis';
GO

-- Basketball (5 formats)
IF NOT EXISTS (SELECT 1 FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id WHERE s.sport_name = 'Basketball' AND sf.format_name = '1v1')
    INSERT INTO [dbo].[sport_format] ([format_name], [has_positions], [no_players], [sport_id])
    SELECT '1v1', 0, 2, sport_id FROM [dbo].[sport] WHERE sport_name = 'Basketball';
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id WHERE s.sport_name = 'Basketball' AND sf.format_name = '2v2')
    INSERT INTO [dbo].[sport_format] ([format_name], [has_positions], [no_players], [sport_id])
    SELECT '2v2', 0, 4, sport_id FROM [dbo].[sport] WHERE sport_name = 'Basketball';
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id WHERE s.sport_name = 'Basketball' AND sf.format_name = '3v3')
    INSERT INTO [dbo].[sport_format] ([format_name], [has_positions], [no_players], [sport_id])
    SELECT '3v3', 0, 6, sport_id FROM [dbo].[sport] WHERE sport_name = 'Basketball';
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id WHERE s.sport_name = 'Basketball' AND sf.format_name = '4v4')
    INSERT INTO [dbo].[sport_format] ([format_name], [has_positions], [no_players], [sport_id])
    SELECT '4v4', 1, 8, sport_id FROM [dbo].[sport] WHERE sport_name = 'Basketball';
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id WHERE s.sport_name = 'Basketball' AND sf.format_name = '5v5')
    INSERT INTO [dbo].[sport_format] ([format_name], [has_positions], [no_players], [sport_id])
    SELECT '5v5', 1, 10, sport_id FROM [dbo].[sport] WHERE sport_name = 'Basketball';
GO

-- Rugby (4 formats)
IF NOT EXISTS (SELECT 1 FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id WHERE s.sport_name = 'Rugby' AND sf.format_name = '7s Contact')
    INSERT INTO [dbo].[sport_format] ([format_name], [has_positions], [no_players], [sport_id])
    SELECT '7s Contact', 1, 14, sport_id FROM [dbo].[sport] WHERE sport_name = 'Rugby';
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id WHERE s.sport_name = 'Rugby' AND sf.format_name = '7s Touch')
    INSERT INTO [dbo].[sport_format] ([format_name], [has_positions], [no_players], [sport_id])
    SELECT '7s Touch', 0, 14, sport_id FROM [dbo].[sport] WHERE sport_name = 'Rugby';
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id WHERE s.sport_name = 'Rugby' AND sf.format_name = '15s Contact')
    INSERT INTO [dbo].[sport_format] ([format_name], [has_positions], [no_players], [sport_id])
    SELECT '15s Contact', 1, 30, sport_id FROM [dbo].[sport] WHERE sport_name = 'Rugby';
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id WHERE s.sport_name = 'Rugby' AND sf.format_name = '15s Touch')
    INSERT INTO [dbo].[sport_format] ([format_name], [has_positions], [no_players], [sport_id])
    SELECT '15s Touch', 0, 30, sport_id FROM [dbo].[sport] WHERE sport_name = 'Rugby';
GO

-- Football (3 formats)
IF NOT EXISTS (SELECT 1 FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id WHERE s.sport_name = 'Football' AND sf.format_name = '3v3')
    INSERT INTO [dbo].[sport_format] ([format_name], [has_positions], [no_players], [sport_id])
    SELECT '3v3', 0, 6, sport_id FROM [dbo].[sport] WHERE sport_name = 'Football';
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id WHERE s.sport_name = 'Football' AND sf.format_name = '5v5')
    INSERT INTO [dbo].[sport_format] ([format_name], [has_positions], [no_players], [sport_id])
    SELECT '5v5', 1, 10, sport_id FROM [dbo].[sport] WHERE sport_name = 'Football';
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id WHERE s.sport_name = 'Football' AND sf.format_name = '11v11')
    INSERT INTO [dbo].[sport_format] ([format_name], [has_positions], [no_players], [sport_id])
    SELECT '11v11', 1, 22, sport_id FROM [dbo].[sport] WHERE sport_name = 'Football';
GO

-- ============================================================
-- POSITIONS (25 distinct)
-- ============================================================

-- Basketball positions
IF NOT EXISTS (SELECT 1 FROM [dbo].[position] WHERE [position_name] = 'Guard')
    INSERT INTO [dbo].[position] ([position_name]) VALUES ('Guard');
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[position] WHERE [position_name] = 'Forward')
    INSERT INTO [dbo].[position] ([position_name]) VALUES ('Forward');
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[position] WHERE [position_name] = 'Centre')
    INSERT INTO [dbo].[position] ([position_name]) VALUES ('Centre');
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[position] WHERE [position_name] = 'Point Guard')
    INSERT INTO [dbo].[position] ([position_name]) VALUES ('Point Guard');
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[position] WHERE [position_name] = 'Shooting Guard')
    INSERT INTO [dbo].[position] ([position_name]) VALUES ('Shooting Guard');
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[position] WHERE [position_name] = 'Small Forward')
    INSERT INTO [dbo].[position] ([position_name]) VALUES ('Small Forward');
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[position] WHERE [position_name] = 'Power Forward')
    INSERT INTO [dbo].[position] ([position_name]) VALUES ('Power Forward');
GO

-- Rugby positions
IF NOT EXISTS (SELECT 1 FROM [dbo].[position] WHERE [position_name] = 'Prop')
    INSERT INTO [dbo].[position] ([position_name]) VALUES ('Prop');
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[position] WHERE [position_name] = 'Hooker')
    INSERT INTO [dbo].[position] ([position_name]) VALUES ('Hooker');
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[position] WHERE [position_name] = 'Scrum-half')
    INSERT INTO [dbo].[position] ([position_name]) VALUES ('Scrum-half');
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[position] WHERE [position_name] = 'Fly-half')
    INSERT INTO [dbo].[position] ([position_name]) VALUES ('Fly-half');
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[position] WHERE [position_name] = 'Wing')
    INSERT INTO [dbo].[position] ([position_name]) VALUES ('Wing');
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[position] WHERE [position_name] = 'Lock')
    INSERT INTO [dbo].[position] ([position_name]) VALUES ('Lock');
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[position] WHERE [position_name] = 'Flanker')
    INSERT INTO [dbo].[position] ([position_name]) VALUES ('Flanker');
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[position] WHERE [position_name] = 'Number Eight')
    INSERT INTO [dbo].[position] ([position_name]) VALUES ('Number Eight');
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[position] WHERE [position_name] = 'Fullback')
    INSERT INTO [dbo].[position] ([position_name]) VALUES ('Fullback');
GO

-- Football positions
IF NOT EXISTS (SELECT 1 FROM [dbo].[position] WHERE [position_name] = 'Goalkeeper')
    INSERT INTO [dbo].[position] ([position_name]) VALUES ('Goalkeeper');
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[position] WHERE [position_name] = 'Defender')
    INSERT INTO [dbo].[position] ([position_name]) VALUES ('Defender');
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[position] WHERE [position_name] = 'Midfielder')
    INSERT INTO [dbo].[position] ([position_name]) VALUES ('Midfielder');
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[position] WHERE [position_name] = 'Centre-back')
    INSERT INTO [dbo].[position] ([position_name]) VALUES ('Centre-back');
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[position] WHERE [position_name] = 'Defensive Midfielder')
    INSERT INTO [dbo].[position] ([position_name]) VALUES ('Defensive Midfielder');
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[position] WHERE [position_name] = 'Central Midfielder')
    INSERT INTO [dbo].[position] ([position_name]) VALUES ('Central Midfielder');
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[position] WHERE [position_name] = 'Attacking Midfielder')
    INSERT INTO [dbo].[position] ([position_name]) VALUES ('Attacking Midfielder');
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[position] WHERE [position_name] = 'Winger')
    INSERT INTO [dbo].[position] ([position_name]) VALUES ('Winger');
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[position] WHERE [position_name] = 'Striker')
    INSERT INTO [dbo].[position] ([position_name]) VALUES ('Striker');
GO

-- ============================================================
-- FORMAT-POSITION LINKS
-- Links each format (has_positions = true) to its valid positions.
-- Identified by format (sport + format_name) + position (position_name).
-- ============================================================

-- Basketball 4v4: Guard, Forward, Centre
IF NOT EXISTS (SELECT 1 FROM [dbo].[format_position] fp
    JOIN [dbo].[sport_format] sf ON fp.format_id = sf.format_id
    JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id
    JOIN [dbo].[position] p ON fp.position_id = p.position_id
    WHERE s.sport_name = 'Basketball' AND sf.format_name = '4v4' AND p.position_name = 'Guard')
    INSERT INTO [dbo].[format_position] ([format_id], [position_id])
    SELECT sf.format_id, p.position_id
    FROM [dbo].[sport_format] sf
    JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id
    CROSS JOIN [dbo].[position] p
    WHERE s.sport_name = 'Basketball' AND sf.format_name = '4v4' AND p.position_name = 'Guard';
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[format_position] fp
    JOIN [dbo].[sport_format] sf ON fp.format_id = sf.format_id
    JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id
    JOIN [dbo].[position] p ON fp.position_id = p.position_id
    WHERE s.sport_name = 'Basketball' AND sf.format_name = '4v4' AND p.position_name = 'Forward')
    INSERT INTO [dbo].[format_position] ([format_id], [position_id])
    SELECT sf.format_id, p.position_id
    FROM [dbo].[sport_format] sf
    JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id
    CROSS JOIN [dbo].[position] p
    WHERE s.sport_name = 'Basketball' AND sf.format_name = '4v4' AND p.position_name = 'Forward';
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[format_position] fp
    JOIN [dbo].[sport_format] sf ON fp.format_id = sf.format_id
    JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id
    JOIN [dbo].[position] p ON fp.position_id = p.position_id
    WHERE s.sport_name = 'Basketball' AND sf.format_name = '4v4' AND p.position_name = 'Centre')
    INSERT INTO [dbo].[format_position] ([format_id], [position_id])
    SELECT sf.format_id, p.position_id
    FROM [dbo].[sport_format] sf
    JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id
    CROSS JOIN [dbo].[position] p
    WHERE s.sport_name = 'Basketball' AND sf.format_name = '4v4' AND p.position_name = 'Centre';
GO

-- Basketball 5v5: Point Guard, Shooting Guard, Small Forward, Power Forward, Centre
IF NOT EXISTS (SELECT 1 FROM [dbo].[format_position] fp JOIN [dbo].[sport_format] sf ON fp.format_id = sf.format_id JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id JOIN [dbo].[position] p ON fp.position_id = p.position_id WHERE s.sport_name = 'Basketball' AND sf.format_name = '5v5' AND p.position_name = 'Point Guard')
    INSERT INTO [dbo].[format_position] ([format_id], [position_id]) SELECT sf.format_id, p.position_id FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id CROSS JOIN [dbo].[position] p WHERE s.sport_name = 'Basketball' AND sf.format_name = '5v5' AND p.position_name = 'Point Guard';
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[format_position] fp JOIN [dbo].[sport_format] sf ON fp.format_id = sf.format_id JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id JOIN [dbo].[position] p ON fp.position_id = p.position_id WHERE s.sport_name = 'Basketball' AND sf.format_name = '5v5' AND p.position_name = 'Shooting Guard')
    INSERT INTO [dbo].[format_position] ([format_id], [position_id]) SELECT sf.format_id, p.position_id FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id CROSS JOIN [dbo].[position] p WHERE s.sport_name = 'Basketball' AND sf.format_name = '5v5' AND p.position_name = 'Shooting Guard';
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[format_position] fp JOIN [dbo].[sport_format] sf ON fp.format_id = sf.format_id JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id JOIN [dbo].[position] p ON fp.position_id = p.position_id WHERE s.sport_name = 'Basketball' AND sf.format_name = '5v5' AND p.position_name = 'Small Forward')
    INSERT INTO [dbo].[format_position] ([format_id], [position_id]) SELECT sf.format_id, p.position_id FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id CROSS JOIN [dbo].[position] p WHERE s.sport_name = 'Basketball' AND sf.format_name = '5v5' AND p.position_name = 'Small Forward';
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[format_position] fp JOIN [dbo].[sport_format] sf ON fp.format_id = sf.format_id JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id JOIN [dbo].[position] p ON fp.position_id = p.position_id WHERE s.sport_name = 'Basketball' AND sf.format_name = '5v5' AND p.position_name = 'Power Forward')
    INSERT INTO [dbo].[format_position] ([format_id], [position_id]) SELECT sf.format_id, p.position_id FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id CROSS JOIN [dbo].[position] p WHERE s.sport_name = 'Basketball' AND sf.format_name = '5v5' AND p.position_name = 'Power Forward';
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[format_position] fp JOIN [dbo].[sport_format] sf ON fp.format_id = sf.format_id JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id JOIN [dbo].[position] p ON fp.position_id = p.position_id WHERE s.sport_name = 'Basketball' AND sf.format_name = '5v5' AND p.position_name = 'Centre')
    INSERT INTO [dbo].[format_position] ([format_id], [position_id]) SELECT sf.format_id, p.position_id FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id CROSS JOIN [dbo].[position] p WHERE s.sport_name = 'Basketball' AND sf.format_name = '5v5' AND p.position_name = 'Centre';
GO

-- Rugby 7s Contact: Prop, Hooker, Scrum-half, Fly-half, Centre, Wing
IF NOT EXISTS (SELECT 1 FROM [dbo].[format_position] fp JOIN [dbo].[sport_format] sf ON fp.format_id = sf.format_id JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id JOIN [dbo].[position] p ON fp.position_id = p.position_id WHERE s.sport_name = 'Rugby' AND sf.format_name = '7s Contact' AND p.position_name = 'Prop')
    INSERT INTO [dbo].[format_position] ([format_id], [position_id]) SELECT sf.format_id, p.position_id FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id CROSS JOIN [dbo].[position] p WHERE s.sport_name = 'Rugby' AND sf.format_name = '7s Contact' AND p.position_name = 'Prop';
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[format_position] fp JOIN [dbo].[sport_format] sf ON fp.format_id = sf.format_id JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id JOIN [dbo].[position] p ON fp.position_id = p.position_id WHERE s.sport_name = 'Rugby' AND sf.format_name = '7s Contact' AND p.position_name = 'Hooker')
    INSERT INTO [dbo].[format_position] ([format_id], [position_id]) SELECT sf.format_id, p.position_id FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id CROSS JOIN [dbo].[position] p WHERE s.sport_name = 'Rugby' AND sf.format_name = '7s Contact' AND p.position_name = 'Hooker';
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[format_position] fp JOIN [dbo].[sport_format] sf ON fp.format_id = sf.format_id JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id JOIN [dbo].[position] p ON fp.position_id = p.position_id WHERE s.sport_name = 'Rugby' AND sf.format_name = '7s Contact' AND p.position_name = 'Scrum-half')
    INSERT INTO [dbo].[format_position] ([format_id], [position_id]) SELECT sf.format_id, p.position_id FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id CROSS JOIN [dbo].[position] p WHERE s.sport_name = 'Rugby' AND sf.format_name = '7s Contact' AND p.position_name = 'Scrum-half';
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[format_position] fp JOIN [dbo].[sport_format] sf ON fp.format_id = sf.format_id JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id JOIN [dbo].[position] p ON fp.position_id = p.position_id WHERE s.sport_name = 'Rugby' AND sf.format_name = '7s Contact' AND p.position_name = 'Fly-half')
    INSERT INTO [dbo].[format_position] ([format_id], [position_id]) SELECT sf.format_id, p.position_id FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id CROSS JOIN [dbo].[position] p WHERE s.sport_name = 'Rugby' AND sf.format_name = '7s Contact' AND p.position_name = 'Fly-half';
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[format_position] fp JOIN [dbo].[sport_format] sf ON fp.format_id = sf.format_id JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id JOIN [dbo].[position] p ON fp.position_id = p.position_id WHERE s.sport_name = 'Rugby' AND sf.format_name = '7s Contact' AND p.position_name = 'Centre')
    INSERT INTO [dbo].[format_position] ([format_id], [position_id]) SELECT sf.format_id, p.position_id FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id CROSS JOIN [dbo].[position] p WHERE s.sport_name = 'Rugby' AND sf.format_name = '7s Contact' AND p.position_name = 'Centre';
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[format_position] fp JOIN [dbo].[sport_format] sf ON fp.format_id = sf.format_id JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id JOIN [dbo].[position] p ON fp.position_id = p.position_id WHERE s.sport_name = 'Rugby' AND sf.format_name = '7s Contact' AND p.position_name = 'Wing')
    INSERT INTO [dbo].[format_position] ([format_id], [position_id]) SELECT sf.format_id, p.position_id FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id CROSS JOIN [dbo].[position] p WHERE s.sport_name = 'Rugby' AND sf.format_name = '7s Contact' AND p.position_name = 'Wing';
GO

-- Rugby 15s Contact: Prop, Hooker, Lock, Flanker, Number Eight, Scrum-half, Fly-half, Centre, Wing, Fullback
IF NOT EXISTS (SELECT 1 FROM [dbo].[format_position] fp JOIN [dbo].[sport_format] sf ON fp.format_id = sf.format_id JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id JOIN [dbo].[position] p ON fp.position_id = p.position_id WHERE s.sport_name = 'Rugby' AND sf.format_name = '15s Contact' AND p.position_name = 'Prop')
    INSERT INTO [dbo].[format_position] ([format_id], [position_id]) SELECT sf.format_id, p.position_id FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id CROSS JOIN [dbo].[position] p WHERE s.sport_name = 'Rugby' AND sf.format_name = '15s Contact' AND p.position_name = 'Prop';
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[format_position] fp JOIN [dbo].[sport_format] sf ON fp.format_id = sf.format_id JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id JOIN [dbo].[position] p ON fp.position_id = p.position_id WHERE s.sport_name = 'Rugby' AND sf.format_name = '15s Contact' AND p.position_name = 'Hooker')
    INSERT INTO [dbo].[format_position] ([format_id], [position_id]) SELECT sf.format_id, p.position_id FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id CROSS JOIN [dbo].[position] p WHERE s.sport_name = 'Rugby' AND sf.format_name = '15s Contact' AND p.position_name = 'Hooker';
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[format_position] fp JOIN [dbo].[sport_format] sf ON fp.format_id = sf.format_id JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id JOIN [dbo].[position] p ON fp.position_id = p.position_id WHERE s.sport_name = 'Rugby' AND sf.format_name = '15s Contact' AND p.position_name = 'Lock')
    INSERT INTO [dbo].[format_position] ([format_id], [position_id]) SELECT sf.format_id, p.position_id FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id CROSS JOIN [dbo].[position] p WHERE s.sport_name = 'Rugby' AND sf.format_name = '15s Contact' AND p.position_name = 'Lock';
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[format_position] fp JOIN [dbo].[sport_format] sf ON fp.format_id = sf.format_id JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id JOIN [dbo].[position] p ON fp.position_id = p.position_id WHERE s.sport_name = 'Rugby' AND sf.format_name = '15s Contact' AND p.position_name = 'Flanker')
    INSERT INTO [dbo].[format_position] ([format_id], [position_id]) SELECT sf.format_id, p.position_id FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id CROSS JOIN [dbo].[position] p WHERE s.sport_name = 'Rugby' AND sf.format_name = '15s Contact' AND p.position_name = 'Flanker';
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[format_position] fp JOIN [dbo].[sport_format] sf ON fp.format_id = sf.format_id JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id JOIN [dbo].[position] p ON fp.position_id = p.position_id WHERE s.sport_name = 'Rugby' AND sf.format_name = '15s Contact' AND p.position_name = 'Number Eight')
    INSERT INTO [dbo].[format_position] ([format_id], [position_id]) SELECT sf.format_id, p.position_id FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id CROSS JOIN [dbo].[position] p WHERE s.sport_name = 'Rugby' AND sf.format_name = '15s Contact' AND p.position_name = 'Number Eight';
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[format_position] fp JOIN [dbo].[sport_format] sf ON fp.format_id = sf.format_id JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id JOIN [dbo].[position] p ON fp.position_id = p.position_id WHERE s.sport_name = 'Rugby' AND sf.format_name = '15s Contact' AND p.position_name = 'Scrum-half')
    INSERT INTO [dbo].[format_position] ([format_id], [position_id]) SELECT sf.format_id, p.position_id FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id CROSS JOIN [dbo].[position] p WHERE s.sport_name = 'Rugby' AND sf.format_name = '15s Contact' AND p.position_name = 'Scrum-half';
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[format_position] fp JOIN [dbo].[sport_format] sf ON fp.format_id = sf.format_id JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id JOIN [dbo].[position] p ON fp.position_id = p.position_id WHERE s.sport_name = 'Rugby' AND sf.format_name = '15s Contact' AND p.position_name = 'Fly-half')
    INSERT INTO [dbo].[format_position] ([format_id], [position_id]) SELECT sf.format_id, p.position_id FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id CROSS JOIN [dbo].[position] p WHERE s.sport_name = 'Rugby' AND sf.format_name = '15s Contact' AND p.position_name = 'Fly-half';
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[format_position] fp JOIN [dbo].[sport_format] sf ON fp.format_id = sf.format_id JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id JOIN [dbo].[position] p ON fp.position_id = p.position_id WHERE s.sport_name = 'Rugby' AND sf.format_name = '15s Contact' AND p.position_name = 'Centre')
    INSERT INTO [dbo].[format_position] ([format_id], [position_id]) SELECT sf.format_id, p.position_id FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id CROSS JOIN [dbo].[position] p WHERE s.sport_name = 'Rugby' AND sf.format_name = '15s Contact' AND p.position_name = 'Centre';
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[format_position] fp JOIN [dbo].[sport_format] sf ON fp.format_id = sf.format_id JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id JOIN [dbo].[position] p ON fp.position_id = p.position_id WHERE s.sport_name = 'Rugby' AND sf.format_name = '15s Contact' AND p.position_name = 'Wing')
    INSERT INTO [dbo].[format_position] ([format_id], [position_id]) SELECT sf.format_id, p.position_id FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id CROSS JOIN [dbo].[position] p WHERE s.sport_name = 'Rugby' AND sf.format_name = '15s Contact' AND p.position_name = 'Wing';
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[format_position] fp JOIN [dbo].[sport_format] sf ON fp.format_id = sf.format_id JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id JOIN [dbo].[position] p ON fp.position_id = p.position_id WHERE s.sport_name = 'Rugby' AND sf.format_name = '15s Contact' AND p.position_name = 'Fullback')
    INSERT INTO [dbo].[format_position] ([format_id], [position_id]) SELECT sf.format_id, p.position_id FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id CROSS JOIN [dbo].[position] p WHERE s.sport_name = 'Rugby' AND sf.format_name = '15s Contact' AND p.position_name = 'Fullback';
GO

-- Football 5v5: Goalkeeper, Defender, Midfielder, Forward
IF NOT EXISTS (SELECT 1 FROM [dbo].[format_position] fp JOIN [dbo].[sport_format] sf ON fp.format_id = sf.format_id JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id JOIN [dbo].[position] p ON fp.position_id = p.position_id WHERE s.sport_name = 'Football' AND sf.format_name = '5v5' AND p.position_name = 'Goalkeeper')
    INSERT INTO [dbo].[format_position] ([format_id], [position_id]) SELECT sf.format_id, p.position_id FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id CROSS JOIN [dbo].[position] p WHERE s.sport_name = 'Football' AND sf.format_name = '5v5' AND p.position_name = 'Goalkeeper';
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[format_position] fp JOIN [dbo].[sport_format] sf ON fp.format_id = sf.format_id JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id JOIN [dbo].[position] p ON fp.position_id = p.position_id WHERE s.sport_name = 'Football' AND sf.format_name = '5v5' AND p.position_name = 'Defender')
    INSERT INTO [dbo].[format_position] ([format_id], [position_id]) SELECT sf.format_id, p.position_id FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id CROSS JOIN [dbo].[position] p WHERE s.sport_name = 'Football' AND sf.format_name = '5v5' AND p.position_name = 'Defender';
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[format_position] fp JOIN [dbo].[sport_format] sf ON fp.format_id = sf.format_id JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id JOIN [dbo].[position] p ON fp.position_id = p.position_id WHERE s.sport_name = 'Football' AND sf.format_name = '5v5' AND p.position_name = 'Midfielder')
    INSERT INTO [dbo].[format_position] ([format_id], [position_id]) SELECT sf.format_id, p.position_id FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id CROSS JOIN [dbo].[position] p WHERE s.sport_name = 'Football' AND sf.format_name = '5v5' AND p.position_name = 'Midfielder';
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[format_position] fp JOIN [dbo].[sport_format] sf ON fp.format_id = sf.format_id JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id JOIN [dbo].[position] p ON fp.position_id = p.position_id WHERE s.sport_name = 'Football' AND sf.format_name = '5v5' AND p.position_name = 'Forward')
    INSERT INTO [dbo].[format_position] ([format_id], [position_id]) SELECT sf.format_id, p.position_id FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id CROSS JOIN [dbo].[position] p WHERE s.sport_name = 'Football' AND sf.format_name = '5v5' AND p.position_name = 'Forward';
GO

-- Football 11v11: Goalkeeper, Fullback, Centre-back, Defensive Midfielder, Central Midfielder, Attacking Midfielder, Winger, Striker
IF NOT EXISTS (SELECT 1 FROM [dbo].[format_position] fp JOIN [dbo].[sport_format] sf ON fp.format_id = sf.format_id JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id JOIN [dbo].[position] p ON fp.position_id = p.position_id WHERE s.sport_name = 'Football' AND sf.format_name = '11v11' AND p.position_name = 'Goalkeeper')
    INSERT INTO [dbo].[format_position] ([format_id], [position_id]) SELECT sf.format_id, p.position_id FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id CROSS JOIN [dbo].[position] p WHERE s.sport_name = 'Football' AND sf.format_name = '11v11' AND p.position_name = 'Goalkeeper';
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[format_position] fp JOIN [dbo].[sport_format] sf ON fp.format_id = sf.format_id JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id JOIN [dbo].[position] p ON fp.position_id = p.position_id WHERE s.sport_name = 'Football' AND sf.format_name = '11v11' AND p.position_name = 'Fullback')
    INSERT INTO [dbo].[format_position] ([format_id], [position_id]) SELECT sf.format_id, p.position_id FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id CROSS JOIN [dbo].[position] p WHERE s.sport_name = 'Football' AND sf.format_name = '11v11' AND p.position_name = 'Fullback';
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[format_position] fp JOIN [dbo].[sport_format] sf ON fp.format_id = sf.format_id JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id JOIN [dbo].[position] p ON fp.position_id = p.position_id WHERE s.sport_name = 'Football' AND sf.format_name = '11v11' AND p.position_name = 'Centre-back')
    INSERT INTO [dbo].[format_position] ([format_id], [position_id]) SELECT sf.format_id, p.position_id FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id CROSS JOIN [dbo].[position] p WHERE s.sport_name = 'Football' AND sf.format_name = '11v11' AND p.position_name = 'Centre-back';
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[format_position] fp JOIN [dbo].[sport_format] sf ON fp.format_id = sf.format_id JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id JOIN [dbo].[position] p ON fp.position_id = p.position_id WHERE s.sport_name = 'Football' AND sf.format_name = '11v11' AND p.position_name = 'Defensive Midfielder')
    INSERT INTO [dbo].[format_position] ([format_id], [position_id]) SELECT sf.format_id, p.position_id FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id CROSS JOIN [dbo].[position] p WHERE s.sport_name = 'Football' AND sf.format_name = '11v11' AND p.position_name = 'Defensive Midfielder';
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[format_position] fp JOIN [dbo].[sport_format] sf ON fp.format_id = sf.format_id JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id JOIN [dbo].[position] p ON fp.position_id = p.position_id WHERE s.sport_name = 'Football' AND sf.format_name = '11v11' AND p.position_name = 'Central Midfielder')
    INSERT INTO [dbo].[format_position] ([format_id], [position_id]) SELECT sf.format_id, p.position_id FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id CROSS JOIN [dbo].[position] p WHERE s.sport_name = 'Football' AND sf.format_name = '11v11' AND p.position_name = 'Central Midfielder';
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[format_position] fp JOIN [dbo].[sport_format] sf ON fp.format_id = sf.format_id JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id JOIN [dbo].[position] p ON fp.position_id = p.position_id WHERE s.sport_name = 'Football' AND sf.format_name = '11v11' AND p.position_name = 'Attacking Midfielder')
    INSERT INTO [dbo].[format_position] ([format_id], [position_id]) SELECT sf.format_id, p.position_id FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id CROSS JOIN [dbo].[position] p WHERE s.sport_name = 'Football' AND sf.format_name = '11v11' AND p.position_name = 'Attacking Midfielder';
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[format_position] fp JOIN [dbo].[sport_format] sf ON fp.format_id = sf.format_id JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id JOIN [dbo].[position] p ON fp.position_id = p.position_id WHERE s.sport_name = 'Football' AND sf.format_name = '11v11' AND p.position_name = 'Winger')
    INSERT INTO [dbo].[format_position] ([format_id], [position_id]) SELECT sf.format_id, p.position_id FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id CROSS JOIN [dbo].[position] p WHERE s.sport_name = 'Football' AND sf.format_name = '11v11' AND p.position_name = 'Winger';
GO
IF NOT EXISTS (SELECT 1 FROM [dbo].[format_position] fp JOIN [dbo].[sport_format] sf ON fp.format_id = sf.format_id JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id JOIN [dbo].[position] p ON fp.position_id = p.position_id WHERE s.sport_name = 'Football' AND sf.format_name = '11v11' AND p.position_name = 'Striker')
    INSERT INTO [dbo].[format_position] ([format_id], [position_id]) SELECT sf.format_id, p.position_id FROM [dbo].[sport_format] sf JOIN [dbo].[sport] s ON sf.sport_id = s.sport_id CROSS JOIN [dbo].[position] p WHERE s.sport_name = 'Football' AND sf.format_name = '11v11' AND p.position_name = 'Striker';
GO
