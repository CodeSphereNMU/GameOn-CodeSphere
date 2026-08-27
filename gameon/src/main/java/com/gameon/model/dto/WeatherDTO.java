package com.gameon.model.dto;

import java.time.LocalDateTime;

/**
 * DTO carrying weather forecast data for a game listing.
 * Used for REST API responses and passing weather info between layers.
 * Designed for future extensibility (notifications, cancellation suggestions, alerts).
 */
public record WeatherDTO(
        String condition,
        Double temperature,
        Integer rainChance,
        Double windSpeed,
        Integer humidity,
        String icon,
        String badgeColor,
        String warning,
        LocalDateTime forecastTime,
        boolean available
) {

    /**
     * Creates an "unavailable" weather DTO when forecast data cannot be retrieved.
     */
    public static WeatherDTO unavailable() {
        return new WeatherDTO(
                null, null, null, null, null,
                null, null, null, null, false
        );
    }

    /**
     * Creates a WeatherDTO from raw weather data with automatic badge/warning calculation.
     */
    public static WeatherDTO of(String condition, double temperature, int rainChance,
                                 double windSpeed, int humidity, LocalDateTime forecastTime) {
        String icon = resolveIcon(condition);
        String badgeColor = resolveBadgeColor(condition, temperature, rainChance, windSpeed);
        String warning = resolveWarning(condition, temperature, rainChance, windSpeed);

        return new WeatherDTO(
                condition, temperature, rainChance, windSpeed, humidity,
                icon, badgeColor, warning, forecastTime, true
        );
    }

    /**
     * Resolves weather icon based on condition.
     */
    private static String resolveIcon(String condition) {
        if (condition == null) return "bi-cloud-question";
        return switch (condition.toLowerCase()) {
            case "sunny", "clear" -> "bi-sun-fill";
            case "partly cloudy" -> "bi-cloud-sun-fill";
            case "cloudy", "overcast" -> "bi-cloud-fill";
            case "rain", "light rain", "moderate rain", "heavy rain" -> "bi-cloud-rain-fill";
            case "drizzle", "light drizzle" -> "bi-cloud-drizzle-fill";
            case "thunderstorm" -> "bi-cloud-lightning-rain-fill";
            case "snow", "light snow" -> "bi-snow";
            case "fog", "mist" -> "bi-cloud-fog-fill";
            case "windy" -> "bi-wind";
            default -> "bi-cloud";
        };
    }

    /**
     * Resolves badge color: green (good), yellow (moderate), red (poor).
     */
    private static String resolveBadgeColor(String condition, double temperature,
                                             int rainChance, double windSpeed) {
        // Poor conditions
        if (rainChance >= 70 || windSpeed >= 40 || temperature >= 40 || temperature <= 2) {
            return "danger";
        }
        if (condition != null && (condition.equalsIgnoreCase("thunderstorm")
                || condition.equalsIgnoreCase("heavy rain"))) {
            return "danger";
        }

        // Moderate conditions
        if (rainChance >= 40 || windSpeed >= 25 || temperature >= 35 || temperature <= 8) {
            return "warning";
        }
        if (condition != null && (condition.equalsIgnoreCase("rain")
                || condition.equalsIgnoreCase("moderate rain")
                || condition.equalsIgnoreCase("windy"))) {
            return "warning";
        }

        // Good conditions
        return "success";
    }

    /**
     * Resolves warning text for poor/moderate conditions.
     */
    private static String resolveWarning(String condition, double temperature,
                                          int rainChance, double windSpeed) {
        if (rainChance >= 70) return "High chance of rain";
        if (windSpeed >= 40) return "Strong winds expected";
        if (temperature >= 40) return "Extreme heat expected";
        if (temperature <= 2) return "Near-freezing temperatures";
        if (condition != null && condition.equalsIgnoreCase("thunderstorm")) {
            return "Thunderstorm expected";
        }
        if (rainChance >= 40) return "Moderate chance of rain";
        if (windSpeed >= 25) return "Moderate winds expected";
        if (temperature >= 35) return "High temperatures expected";
        return null;
    }
}
