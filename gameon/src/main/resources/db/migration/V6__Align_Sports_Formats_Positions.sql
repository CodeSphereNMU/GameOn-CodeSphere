-- Align reference data with the approved Game On catalogue.
-- "Any Position" is represented by NULL, not by a position row.

-- Duration belongs to the selected format and drives scheduling conflicts.
IF COL_LENGTH('dbo.sport_formats', 'duration_minutes') IS NULL
BEGIN
    ALTER TABLE sport_formats ADD duration_minutes INT NULL;
END;
GO

-- Remove the legacy Any Position representation safely.
UPDATE game_joiners
SET format_position_id = NULL
WHERE format_position_id IN (SELECT position_id FROM positions WHERE position_name = 'Any Position');

UPDATE game_joiners
SET alt_format_position_id = NULL
WHERE alt_format_position_id IN (SELECT position_id FROM positions WHERE position_name = 'Any Position');

DELETE FROM format_positions
WHERE position_id IN (SELECT position_id FROM positions WHERE position_name = 'Any Position');

DELETE FROM positions WHERE position_name = 'Any Position';

-- Normalise legacy names where no target row already exists.
UPDATE positions SET position_name = 'Centre'
WHERE position_name = 'Center' AND NOT EXISTS (SELECT 1 FROM positions WHERE position_name = 'Centre');
UPDATE positions SET position_name = 'Defender'
WHERE position_name = 'Defense' AND NOT EXISTS (SELECT 1 FROM positions WHERE position_name = 'Defender');
UPDATE positions SET position_name = 'Midfielder'
WHERE position_name = 'Midfield' AND NOT EXISTS (SELECT 1 FROM positions WHERE position_name = 'Midfielder');
UPDATE positions SET position_name = 'Scrum-half'
WHERE position_name = 'Scrumhalf' AND NOT EXISTS (SELECT 1 FROM positions WHERE position_name = 'Scrum-half');
UPDATE positions SET position_name = 'Fly-half'
WHERE position_name = 'Flyhalf' AND NOT EXISTS (SELECT 1 FROM positions WHERE position_name = 'Fly-half');

-- Attack is the old name used for the approved shared Forward position.
UPDATE game_joiners
SET format_position_id = (SELECT position_id FROM positions WHERE position_name = 'Forward')
WHERE format_position_id IN (SELECT position_id FROM positions WHERE position_name = 'Attack');
UPDATE game_joiners
SET alt_format_position_id = (SELECT position_id FROM positions WHERE position_name = 'Forward')
WHERE alt_format_position_id IN (SELECT position_id FROM positions WHERE position_name = 'Attack');
DELETE FROM format_positions
WHERE position_id IN (SELECT position_id FROM positions WHERE position_name = 'Attack');
DELETE FROM positions WHERE position_name = 'Attack';

-- Add every approved real position.
INSERT INTO positions (position_name)
SELECT v.position_name
FROM (VALUES
    ('Guard'), ('Forward'), ('Centre'), ('Point Guard'), ('Shooting Guard'),
    ('Small Forward'), ('Power Forward'), ('Prop'), ('Hooker'), ('Scrum-half'),
    ('Fly-half'), ('Wing'), ('Lock'), ('Flanker'), ('Number Eight'), ('Fullback'),
    ('Goalkeeper'), ('Defender'), ('Midfielder'), ('Centre-back'),
    ('Defensive Midfielder'), ('Central Midfielder'), ('Attacking Midfielder'),
    ('Winger'), ('Striker')
) v(position_name)
WHERE NOT EXISTS (
    SELECT 1 FROM positions p WHERE p.position_name = v.position_name
);

-- Rename the existing Rugby formats to the approved Contact formats.
UPDATE sf SET format_name = '7s Contact'
FROM sport_formats sf JOIN sports s ON s.sport_id = sf.sport_id
WHERE s.sport_name = 'Rugby' AND sf.format_name = '7s';

UPDATE sf SET format_name = '15s Contact'
FROM sport_formats sf JOIN sports s ON s.sport_id = sf.sport_id
WHERE s.sport_name = 'Rugby' AND sf.format_name = '15s';

-- Add missing approved formats.
INSERT INTO sport_formats (sport_id, format_name, no_players, has_positions)
SELECT s.sport_id, v.format_name, v.no_players, v.has_positions
FROM sports s
JOIN (VALUES
    ('Basketball', '1v1', 2, 0),
    ('Basketball', '2v2', 4, 0),
    ('Basketball', '4v4', 8, 1),
    ('Football', '3v3', 6, 0),
    ('Rugby', '7s Touch', 14, 0),
    ('Rugby', '15s Touch', 30, 0)
) v(sport_name, format_name, no_players, has_positions)
    ON s.sport_name = v.sport_name
WHERE NOT EXISTS (
    SELECT 1 FROM sport_formats sf
    WHERE sf.sport_id = s.sport_id AND sf.format_name = v.format_name
);

-- Correct format flags and capacities.
UPDATE sf SET no_players = v.no_players, has_positions = v.has_positions
FROM sport_formats sf
JOIN sports s ON s.sport_id = sf.sport_id
JOIN (VALUES
    ('Padel', 'Doubles', 4, 0),
    ('Tennis', 'Singles', 2, 0), ('Tennis', 'Doubles', 4, 0),
    ('Basketball', '1v1', 2, 0), ('Basketball', '2v2', 4, 0),
    ('Basketball', '3v3', 6, 0), ('Basketball', '4v4', 8, 1),
    ('Basketball', '5v5', 10, 1),
    ('Football', '3v3', 6, 0), ('Football', '5v5', 10, 1),
    ('Football', '11v11', 22, 1),
    ('Rugby', '7s Contact', 14, 1), ('Rugby', '7s Touch', 14, 0),
    ('Rugby', '15s Contact', 30, 1), ('Rugby', '15s Touch', 30, 0)
) v(sport_name, format_name, no_players, has_positions)
    ON s.sport_name = v.sport_name AND sf.format_name = v.format_name;

UPDATE sf SET duration_minutes = v.duration_minutes
FROM sport_formats sf
JOIN sports s ON s.sport_id = sf.sport_id
JOIN (VALUES
    ('Padel', 'Doubles', 60),
    ('Tennis', 'Singles', 60), ('Tennis', 'Doubles', 60),
    ('Basketball', '1v1', 60), ('Basketball', '2v2', 60),
    ('Basketball', '3v3', 60), ('Basketball', '4v4', 60), ('Basketball', '5v5', 60),
    ('Football', '3v3', 60), ('Football', '5v5', 60), ('Football', '7v7', 60),
    ('Football', '11v11', 120),
    ('Rugby', '7s Contact', 60), ('Rugby', '7s Touch', 60),
    ('Rugby', '15s Contact', 120), ('Rugby', '15s Touch', 120)
) v(sport_name, format_name, duration_minutes)
    ON s.sport_name = v.sport_name AND sf.format_name = v.format_name;

IF EXISTS (SELECT 1 FROM sport_formats WHERE duration_minutes IS NULL OR duration_minutes <= 0)
BEGIN
    RAISERROR('Every sport format must have a positive duration_minutes value.', 16, 1);
    RETURN;
END;

ALTER TABLE sport_formats ALTER COLUMN duration_minutes INT NOT NULL;

IF OBJECT_ID('dbo.CK_sport_formats_duration_positive', 'C') IS NULL
    ALTER TABLE sport_formats ADD CONSTRAINT CK_sport_formats_duration_positive CHECK (duration_minutes > 0);

-- Remove the unapproved Football 7v7 only when no listing depends on it.
DELETE fp
FROM format_positions fp
JOIN sport_formats sf ON sf.format_id = fp.format_id
JOIN sports s ON s.sport_id = sf.sport_id
WHERE s.sport_name = 'Football' AND sf.format_name = '7v7'
  AND NOT EXISTS (SELECT 1 FROM game_listings gl WHERE gl.format_id = sf.format_id);

DELETE sf
FROM sport_formats sf
JOIN sports s ON s.sport_id = sf.sport_id
WHERE s.sport_name = 'Football' AND sf.format_name = '7v7'
  AND NOT EXISTS (SELECT 1 FROM game_listings gl WHERE gl.format_id = sf.format_id);

-- Rebuild mappings for every approved positional format.
DELETE fp
FROM format_positions fp
JOIN sport_formats sf ON sf.format_id = fp.format_id
JOIN sports s ON s.sport_id = sf.sport_id
WHERE (s.sport_name = 'Basketball' AND sf.format_name IN ('3v3','4v4','5v5'))
   OR (s.sport_name = 'Football' AND sf.format_name IN ('5v5','11v11'))
   OR (s.sport_name = 'Rugby' AND sf.format_name IN ('7s Contact','7s Touch','15s Contact','15s Touch'));

INSERT INTO format_positions (format_id, position_id)
SELECT sf.format_id, p.position_id
FROM sport_formats sf
JOIN sports s ON s.sport_id = sf.sport_id
JOIN (VALUES
    ('Basketball', '4v4', 'Guard'), ('Basketball', '4v4', 'Forward'),
    ('Basketball', '4v4', 'Centre'),
    ('Basketball', '5v5', 'Point Guard'), ('Basketball', '5v5', 'Shooting Guard'),
    ('Basketball', '5v5', 'Small Forward'), ('Basketball', '5v5', 'Power Forward'),
    ('Basketball', '5v5', 'Centre'),
    ('Football', '5v5', 'Goalkeeper'), ('Football', '5v5', 'Defender'),
    ('Football', '5v5', 'Midfielder'), ('Football', '5v5', 'Forward'),
    ('Football', '11v11', 'Goalkeeper'), ('Football', '11v11', 'Fullback'),
    ('Football', '11v11', 'Centre-back'), ('Football', '11v11', 'Defensive Midfielder'),
    ('Football', '11v11', 'Central Midfielder'), ('Football', '11v11', 'Attacking Midfielder'),
    ('Football', '11v11', 'Winger'), ('Football', '11v11', 'Striker'),
    ('Rugby', '7s Contact', 'Prop'), ('Rugby', '7s Contact', 'Hooker'),
    ('Rugby', '7s Contact', 'Scrum-half'), ('Rugby', '7s Contact', 'Fly-half'),
    ('Rugby', '7s Contact', 'Centre'), ('Rugby', '7s Contact', 'Wing'),
    ('Rugby', '15s Contact', 'Prop'), ('Rugby', '15s Contact', 'Hooker'),
    ('Rugby', '15s Contact', 'Lock'), ('Rugby', '15s Contact', 'Flanker'),
    ('Rugby', '15s Contact', 'Number Eight'), ('Rugby', '15s Contact', 'Scrum-half'),
    ('Rugby', '15s Contact', 'Fly-half'), ('Rugby', '15s Contact', 'Centre'),
    ('Rugby', '15s Contact', 'Wing'), ('Rugby', '15s Contact', 'Fullback')
) v(sport_name, format_name, position_name)
    ON s.sport_name = v.sport_name AND sf.format_name = v.format_name
JOIN positions p ON p.position_name = v.position_name;

-- Existing selections that are no longer valid for their format become Any Position (NULL).
UPDATE gj
SET format_position_id = NULL
FROM game_joiners gj
JOIN game_listings gl ON gl.game_listing_id = gj.game_listing_id
WHERE gj.format_position_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM format_positions fp
      WHERE fp.format_id = gl.format_id AND fp.position_id = gj.format_position_id
  );

UPDATE gj
SET alt_format_position_id = NULL
FROM game_joiners gj
JOIN game_listings gl ON gl.game_listing_id = gj.game_listing_id
WHERE gj.alt_format_position_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM format_positions fp
      WHERE fp.format_id = gl.format_id AND fp.position_id = gj.alt_format_position_id
  );
