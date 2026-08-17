-- Bring the SQL Server schema into line with the existing GameListing entity.
-- Existing listings inherit their duration from their selected sport format.

IF COL_LENGTH('dbo.game_listings', 'session_duration') IS NULL
BEGIN
    ALTER TABLE game_listings ADD session_duration INT NULL;
END;
GO

UPDATE gl
SET session_duration = (sf.duration_minutes + 59) / 60
FROM game_listings gl
JOIN sport_formats sf ON sf.format_id = gl.format_id
WHERE gl.session_duration IS NULL;

IF EXISTS (
    SELECT 1
    FROM game_listings
    WHERE session_duration IS NULL OR session_duration < 1 OR session_duration > 8
)
BEGIN
    RAISERROR('Every game listing must have a session duration between 1 and 8 hours.', 16, 1);
    RETURN;
END;

ALTER TABLE game_listings ALTER COLUMN session_duration INT NOT NULL;

IF OBJECT_ID('dbo.DF_game_listings_session_duration', 'D') IS NULL
    ALTER TABLE game_listings
        ADD CONSTRAINT DF_game_listings_session_duration DEFAULT 1 FOR session_duration;

IF OBJECT_ID('dbo.CK_game_listings_session_duration', 'C') IS NULL
    ALTER TABLE game_listings
        ADD CONSTRAINT CK_game_listings_session_duration
        CHECK (session_duration BETWEEN 1 AND 8);
