-- V15: Add structured venue details for game listings (Map + Weather + Playability upgrade).
--
-- The existing free-text `location` column is preserved unchanged and remains the NOT NULL,
-- human-readable location string used everywhere today. This migration ONLY ADDS four new
-- NULLable columns so that a listing can optionally carry precise venue intelligence:
--
--   venue_name  - the resolved sports-venue name (e.g. "Boardwalk Padel")
--   latitude    - WGS84 latitude  (-90..90),  DECIMAL(9,6) => ~0.11 m precision
--   longitude   - WGS84 longitude (-180..180), DECIMAL(9,6)
--   venue_type  - INDOOR / OUTDOOR indicator, drives the playability engine
--
-- All new columns are NULLable so every existing row (which only has free-text `location`)
-- stays valid and Hibernate `ddl-auto=validate` continues to pass. No existing table,
-- constraint, workflow, view, trigger, or seed data is modified. Guarded with defensive
-- existence checks so the migration is safe to re-run.

IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID('dbo.game_listings') AND name = 'venue_name'
)
    ALTER TABLE dbo.game_listings ADD venue_name VARCHAR(200) NULL;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID('dbo.game_listings') AND name = 'latitude'
)
    ALTER TABLE dbo.game_listings ADD latitude DECIMAL(9,6) NULL;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID('dbo.game_listings') AND name = 'longitude'
)
    ALTER TABLE dbo.game_listings ADD longitude DECIMAL(9,6) NULL;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID('dbo.game_listings') AND name = 'venue_type'
)
    ALTER TABLE dbo.game_listings ADD venue_type VARCHAR(10) NULL;
GO

-- Restrict venue_type to the two supported values (NULL still allowed for legacy rows).
IF NOT EXISTS (
    SELECT 1 FROM sys.check_constraints
    WHERE parent_object_id = OBJECT_ID('dbo.game_listings') AND name = 'CK_game_listings_venue_type'
)
    ALTER TABLE dbo.game_listings
        ADD CONSTRAINT CK_game_listings_venue_type
        CHECK (venue_type IS NULL OR venue_type IN ('INDOOR', 'OUTDOOR'));
GO

-- Latitude / longitude sanity ranges (NULL allowed).
IF NOT EXISTS (
    SELECT 1 FROM sys.check_constraints
    WHERE parent_object_id = OBJECT_ID('dbo.game_listings') AND name = 'CK_game_listings_lat_range'
)
    ALTER TABLE dbo.game_listings
        ADD CONSTRAINT CK_game_listings_lat_range
        CHECK (latitude IS NULL OR (latitude >= -90 AND latitude <= 90));
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.check_constraints
    WHERE parent_object_id = OBJECT_ID('dbo.game_listings') AND name = 'CK_game_listings_lng_range'
)
    ALTER TABLE dbo.game_listings
        ADD CONSTRAINT CK_game_listings_lng_range
        CHECK (longitude IS NULL OR (longitude >= -180 AND longitude <= 180));
GO
