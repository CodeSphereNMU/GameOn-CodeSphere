-- ============================================================
-- V7: Add map location fields to game_listings table
-- Supports OpenStreetMap/Leaflet.js venue selection
-- Keeps existing 'location' column for backward compatibility
-- ============================================================
ALTER TABLE game_listings ADD venue_name VARCHAR(300) NULL;
ALTER TABLE game_listings ADD address VARCHAR(500) NULL;
ALTER TABLE game_listings ADD latitude FLOAT NULL;
ALTER TABLE game_listings ADD longitude FLOAT NULL;
