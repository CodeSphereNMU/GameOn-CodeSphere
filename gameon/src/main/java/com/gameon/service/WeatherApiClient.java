package com.gameon.service;

import com.gameon.model.dto.WeatherDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * HTTP client for the Open-Meteo weather API (free, no API key required).
 * Fetches hourly forecast data for given coordinates and time.
 *
 * Open-Meteo provides up to 16 days of forecast data.
 * API docs: https://open-meteo.com/en/docs
 *
 * Designed for future extensibility:
 * - Weather-based notifications
 * - Match cancellation suggestions
 * - Severe weather alerts
 * - Recommended indoor alternatives
 */
@Component
public class WeatherApiClient {

    private static final Logger logger = LoggerFactory.getLogger(WeatherApiClient.class);
    private static final String BASE_URL = "https://api.open-meteo.com/v1/forecast";
    private static final int MAX_FORECAST_DAYS = 16;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public WeatherApiClient() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Fetches weather forecast for the given coordinates and target date/time.
     * Returns WeatherDTO.unavailable() if the forecast cannot be retrieved.
     *
     * @param latitude  venue latitude
     * @param longitude venue longitude
     * @param targetDateTime the scheduled date/time of the game
     * @return WeatherDTO with forecast data or unavailable marker
     */
    public WeatherDTO fetchForecast(double latitude, double longitude, LocalDateTime targetDateTime) {
        try {
            // Open-Meteo only supports forecasts up to 16 days ahead
            long daysAhead = java.time.Duration.between(LocalDateTime.now(), targetDateTime).toDays();
            if (daysAhead > MAX_FORECAST_DAYS || daysAhead < 0) {
                logger.debug("Target date {} is outside forecast range ({} days ahead)", targetDateTime, daysAhead);
                return WeatherDTO.unavailable();
            }

            String dateStr = targetDateTime.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
            int targetHour = targetDateTime.getHour();

            // Build the API URL with hourly parameters (Locale.US ensures '.' decimal separators)
            String url = String.format(java.util.Locale.US,
                    "%s?latitude=%.4f&longitude=%.4f&hourly=temperature_2m,relative_humidity_2m," +
                    "precipitation_probability,weather_code,wind_speed_10m&start_date=%s&end_date=%s" +
                    "&timezone=auto",
                    BASE_URL, latitude, longitude, dateStr, dateStr
            );

            logger.info("Weather API request URL: {}", url);

            String response = restTemplate.getForObject(url, String.class);
            if (response == null) {
                logger.warn("Empty response from Open-Meteo API");
                return WeatherDTO.unavailable();
            }

            JsonNode root = objectMapper.readTree(response);
            JsonNode hourly = root.path("hourly");

            if (hourly.isMissingNode()) {
                logger.warn("No hourly data in Open-Meteo response");
                return WeatherDTO.unavailable();
            }

            // Extract data for the target hour
            JsonNode temperatures = hourly.path("temperature_2m");
            JsonNode humidities = hourly.path("relative_humidity_2m");
            JsonNode precipProbabilities = hourly.path("precipitation_probability");
            JsonNode weatherCodes = hourly.path("weather_code");
            JsonNode windSpeeds = hourly.path("wind_speed_10m");

            if (targetHour >= temperatures.size()) {
                logger.warn("Target hour {} out of range (data has {} entries)", targetHour, temperatures.size());
                return WeatherDTO.unavailable();
            }

            double temperature = temperatures.get(targetHour).asDouble();
            int humidity = humidities.get(targetHour).asInt();
            int rainChance = precipProbabilities.get(targetHour).asInt();
            int weatherCode = weatherCodes.get(targetHour).asInt();
            double windSpeed = windSpeeds.get(targetHour).asDouble();

            String condition = mapWeatherCode(weatherCode);

            logger.info("Weather forecast retrieved: {}°C, {}, rain={}%, wind={} km/h, humidity={}%",
                    temperature, condition, rainChance, windSpeed, humidity);

            return WeatherDTO.of(condition, temperature, rainChance, windSpeed, humidity, LocalDateTime.now());

        } catch (Exception e) {
            logger.error("Failed to fetch weather forecast for lat={}, lng={}, date={}: {}",
                    latitude, longitude, targetDateTime, e.getMessage());
            return WeatherDTO.unavailable();
        }
    }

    /**
     * Maps WMO weather codes to human-readable condition strings.
     * Reference: https://open-meteo.com/en/docs (WMO Weather interpretation codes)
     */
    private String mapWeatherCode(int code) {
        return switch (code) {
            case 0 -> "Clear";
            case 1 -> "Sunny";
            case 2 -> "Partly Cloudy";
            case 3 -> "Overcast";
            case 45, 48 -> "Fog";
            case 51, 53 -> "Light Drizzle";
            case 55 -> "Drizzle";
            case 56, 57 -> "Drizzle";
            case 61 -> "Light Rain";
            case 63 -> "Moderate Rain";
            case 65 -> "Heavy Rain";
            case 66, 67 -> "Rain";
            case 71, 73 -> "Light Snow";
            case 75, 77 -> "Snow";
            case 80, 81 -> "Rain";
            case 82 -> "Heavy Rain";
            case 85, 86 -> "Snow";
            case 95 -> "Thunderstorm";
            case 96, 99 -> "Thunderstorm";
            default -> "Cloudy";
        };
    }
}
