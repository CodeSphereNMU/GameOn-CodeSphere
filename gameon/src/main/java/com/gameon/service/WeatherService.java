package com.gameon.service;

import com.gameon.model.dto.WeatherDTO;
import com.gameon.model.entity.GameListing;
import com.gameon.repository.GameListingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Business logic layer for weather forecast integration.
 * Orchestrates fetching, caching, and refreshing weather data for game listings.
 *
 * Future support:
 * - Weather-based notifications
 * - Match cancellation suggestions
 * - Severe weather alerts
 * - Recommended indoor alternatives
 */
@Service
public class WeatherService {

    private static final Logger logger = LoggerFactory.getLogger(WeatherService.class);

    private final WeatherApiClient weatherApiClient;
    private final GameListingRepository gameListingRepository;

    public WeatherService(WeatherApiClient weatherApiClient,
                          GameListingRepository gameListingRepository) {
        this.weatherApiClient = weatherApiClient;
        this.gameListingRepository = gameListingRepository;
    }

    /**
     * Fetches a live weather forecast for given coordinates and time.
     * Does NOT persist data — used for preview during listing creation.
     *
     * @param latitude  venue latitude
     * @param longitude venue longitude
     * @param scheduledDate the date/time of the game
     * @return WeatherDTO with forecast data or unavailable marker
     */
    public WeatherDTO getForecast(double latitude, double longitude, LocalDateTime scheduledDate) {
        return weatherApiClient.fetchForecast(latitude, longitude, scheduledDate);
    }

    /**
     * Fetches weather forecast and persists it to the given listing.
     * Called when a listing is created or when the scheduled refresh job runs.
     *
     * @param listing the game listing to update
     * @return WeatherDTO representing the fetched forecast
     */
    @Transactional
    public WeatherDTO fetchAndStoreWeather(GameListing listing) {
        if (listing.getLatitude() == null || listing.getLongitude() == null) {
            logger.debug("Listing {} has no coordinates, skipping weather fetch", listing.getGameListingId());
            return WeatherDTO.unavailable();
        }

        WeatherDTO forecast = weatherApiClient.fetchForecast(
                listing.getLatitude(), listing.getLongitude(), listing.getScheduledDate());

        if (forecast.available()) {
            listing.setWeatherCondition(forecast.condition());
            listing.setWeatherTemperature(forecast.temperature());
            listing.setWeatherRainChance(forecast.rainChance());
            listing.setWeatherWindSpeed(forecast.windSpeed());
            listing.setWeatherHumidity(forecast.humidity());
            listing.setWeatherForecastTime(forecast.forecastTime());
            gameListingRepository.save(listing);
            logger.info("Weather stored for listing {}: {} {}°C",
                    listing.getGameListingId(), forecast.condition(), forecast.temperature());
        } else {
            logger.warn("Weather unavailable for listing {}, not updating stored data",
                    listing.getGameListingId());
        }

        return forecast;
    }

    /**
     * Builds a WeatherDTO from a listing's stored weather fields.
     * Returns unavailable if no weather data is stored.
     *
     * @param listing the game listing with stored weather data
     * @return WeatherDTO built from persisted fields
     */
    public WeatherDTO getStoredWeather(GameListing listing) {
        if (listing.getWeatherCondition() == null || listing.getWeatherTemperature() == null) {
            return WeatherDTO.unavailable();
        }

        return WeatherDTO.of(
                listing.getWeatherCondition(),
                listing.getWeatherTemperature(),
                listing.getWeatherRainChance() != null ? listing.getWeatherRainChance() : 0,
                listing.getWeatherWindSpeed() != null ? listing.getWeatherWindSpeed() : 0.0,
                listing.getWeatherHumidity() != null ? listing.getWeatherHumidity() : 0,
                listing.getWeatherForecastTime()
        );
    }

    /**
     * Refreshes weather forecasts for all upcoming listings within the next 7 days.
     * Does NOT modify completed or cancelled listings.
     * Called by the scheduled job every 6 hours.
     *
     * @return number of listings updated
     */
    @Transactional
    public int refreshUpcomingForecasts() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysAhead = now.plusDays(7);

        List<GameListing> upcomingListings = gameListingRepository
                .findUpcomingListingsForWeatherRefresh(now, sevenDaysAhead);

        logger.info("Weather refresh: found {} upcoming listings to update", upcomingListings.size());

        int updated = 0;
        for (GameListing listing : upcomingListings) {
            try {
                WeatherDTO result = fetchAndStoreWeather(listing);
                if (result.available()) {
                    updated++;
                }
            } catch (Exception e) {
                logger.error("Weather refresh failed for listing {}: {}",
                        listing.getGameListingId(), e.getMessage());
            }
        }

        logger.info("Weather refresh complete: {}/{} listings updated", updated, upcomingListings.size());
        return updated;
    }
}
