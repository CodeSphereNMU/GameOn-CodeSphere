-- V11: Add weather forecast fields to game_listings table
-- Stores cached weather forecast data for each listing's venue, date, and time.

ALTER TABLE game_listings ADD weather_condition VARCHAR(50) NULL;
ALTER TABLE game_listings ADD weather_temperature FLOAT NULL;
ALTER TABLE game_listings ADD weather_rain_chance INT NULL;
ALTER TABLE game_listings ADD weather_wind_speed FLOAT NULL;
ALTER TABLE game_listings ADD weather_humidity INT NULL;
ALTER TABLE game_listings ADD weather_forecast_time DATETIME2 NULL;
