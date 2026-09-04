package com.gameon.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.gameon.model.dto.WeatherForecast;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fetches weather forecasts from the free, keyless Open-Meteo forecast API and caches
 * results to avoid repeated calls for the same location.
 *
 * <p>Performance requirements addressed:</p>
 * <ul>
 *   <li>Results are cached per rounded (lat,lng) for a short TTL.</li>
 *   <li>Concurrent duplicate requests for the same key are de-duplicated via a per-key lock.</li>
 *   <li>A single API call returns up to 7 days of hourly data, reused for both the requested
 *       match time and the alternative-date suggestions.</li>
 * </ul>
 */
@Service
public class WeatherService {

    private static final Logger logger = LoggerFactory.getLogger(WeatherService.class);
    private static final String FORECAST_URL = "https://api.open-meteo.com/v1/forecast";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);
    private static final DateTimeFormatter API_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final RestTemplate restTemplate;

    // Cache of parsed hourly forecasts keyed by rounded location.
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final Map<String, Object> keyLocks = new ConcurrentHashMap<>();

    public WeatherService(RestTemplate externalApiRestTemplate) {
        this.restTemplate = externalApiRestTemplate;
    }

    /**
     * Forecast at (or nearest to) the given match time. Returns an "unavailable" forecast
     * when coordinates are missing or the target time is outside the forecast horizon.
     */
    public WeatherForecast getForecast(Double latitude, Double longitude, LocalDateTime matchTime) {
        if (latitude == null || longitude == null || matchTime == null) {
            return WeatherForecast.unavailable();
        }
        List<WeatherForecast> hourly = getHourlyForecast(latitude, longitude);
        return nearest(hourly, matchTime);
    }

    /**
     * Daily forecasts (one representative sample near the given time-of-day) for the next
     * {@code days} days, used for alternative-date suggestions. Reuses the cached hourly data.
     */
    public List<WeatherForecast> getDailyForecasts(Double latitude, Double longitude,
                                                   LocalDateTime referenceTime, int days) {
        List<WeatherForecast> result = new ArrayList<>();
        if (latitude == null || longitude == null || referenceTime == null) {
            return result;
        }
        List<WeatherForecast> hourly = getHourlyForecast(latitude, longitude);
        LocalDate start = referenceTime.toLocalDate();
        for (int i = 0; i <= days; i++) {
            LocalDate day = start.plusDays(i);
            LocalDateTime target = day.atTime(referenceTime.toLocalTime());
            WeatherForecast f = nearest(hourly, target);
            if (f.isAvailable()) {
                result.add(f);
            }
        }
        return result;
    }

    // ===== Internal =====

    private List<WeatherForecast> getHourlyForecast(double latitude, double longitude) {
        String key = cacheKey(latitude, longitude);

        CacheEntry cached = cache.get(key);
        if (cached != null && !cached.isExpired()) {
            return cached.forecasts;
        }

        // De-duplicate concurrent fetches for the same location.
        Object lock = keyLocks.computeIfAbsent(key, k -> new Object());
        synchronized (lock) {
            cached = cache.get(key);
            if (cached != null && !cached.isExpired()) {
                return cached.forecasts;
            }
            List<WeatherForecast> fetched = fetchFromApi(latitude, longitude);
            if (!fetched.isEmpty()) {
                cache.put(key, new CacheEntry(fetched));
            }
            keyLocks.remove(key);
            return fetched;
        }
    }

    private List<WeatherForecast> fetchFromApi(double latitude, double longitude) {
        List<WeatherForecast> out = new ArrayList<>();
        try {
            String url = UriComponentsBuilder.fromHttpUrl(FORECAST_URL)
                    .queryParam("latitude", latitude)
                    .queryParam("longitude", longitude)
                    .queryParam("hourly",
                            "temperature_2m,relative_humidity_2m,precipitation_probability,"
                                    + "wind_speed_10m,weather_code")
                    .queryParam("wind_speed_unit", "kmh")
                    .queryParam("timezone", "auto")
                    .queryParam("forecast_days", 8)
                    .build().toUriString();

            JsonNode root = restTemplate.getForObject(url, JsonNode.class);
            if (root == null || !root.has("hourly")) {
                return out;
            }
            JsonNode hourly = root.get("hourly");
            JsonNode times = hourly.get("time");
            JsonNode temps = hourly.get("temperature_2m");
            JsonNode hums = hourly.get("relative_humidity_2m");
            JsonNode rains = hourly.get("precipitation_probability");
            JsonNode winds = hourly.get("wind_speed_10m");
            JsonNode codes = hourly.get("weather_code");
            if (times == null) {
                return out;
            }
            for (int i = 0; i < times.size(); i++) {
                WeatherForecast f = new WeatherForecast();
                f.setAvailable(true);
                f.setTime(LocalDateTime.parse(times.get(i).asText(), API_TIME));
                f.setTemperatureC(valAsDouble(temps, i));
                f.setHumidityPercent(valAsInt(hums, i));
                f.setRainChancePercent(valAsInt(rains, i));
                f.setWindSpeedKmh(valAsDouble(winds, i));
                Integer code = valAsInt(codes, i);
                f.setWeatherCode(code);
                f.setCondition(conditionFor(code));
                f.setIcon(iconFor(code));
                out.add(f);
            }
        } catch (Exception e) {
            logger.warn("Weather fetch failed for {},{}: {}", latitude, longitude, e.getMessage());
        }
        return out;
    }

    private WeatherForecast nearest(List<WeatherForecast> hourly, LocalDateTime target) {
        if (hourly == null || hourly.isEmpty()) {
            return WeatherForecast.unavailable();
        }
        WeatherForecast best = null;
        long bestDiff = Long.MAX_VALUE;
        for (WeatherForecast f : hourly) {
            if (f.getTime() == null) continue;
            long diff = Math.abs(Duration.between(f.getTime(), target).toMinutes());
            if (diff < bestDiff) {
                bestDiff = diff;
                best = f;
            }
        }
        // If the closest sample is more than 12h away, the time is outside the useful horizon.
        if (best == null || bestDiff > 12 * 60) {
            return WeatherForecast.unavailable();
        }
        return best;
    }

    private static Double valAsDouble(JsonNode arr, int i) {
        if (arr == null || i >= arr.size() || arr.get(i).isNull()) return null;
        return arr.get(i).asDouble();
    }

    private static Integer valAsInt(JsonNode arr, int i) {
        if (arr == null || i >= arr.size() || arr.get(i).isNull()) return null;
        return arr.get(i).asInt();
    }

    // Round to ~1km grid so nearby lookups share a cache entry.
    private static String cacheKey(double lat, double lng) {
        return String.format("%.2f,%.2f", lat, lng);
    }

    /** Maps WMO weather codes to a human-readable condition. */
    static String conditionFor(Integer code) {
        if (code == null) return "Unknown";
        return switch (code) {
            case 0 -> "Clear";
            case 1 -> "Mainly Clear";
            case 2 -> "Partly Cloudy";
            case 3 -> "Overcast";
            case 45, 48 -> "Fog";
            case 51, 53, 55 -> "Drizzle";
            case 56, 57 -> "Freezing Drizzle";
            case 61, 63, 65 -> "Rain";
            case 66, 67 -> "Freezing Rain";
            case 71, 73, 75, 77 -> "Snow";
            case 80, 81, 82 -> "Rain Showers";
            case 85, 86 -> "Snow Showers";
            case 95 -> "Thunderstorm";
            case 96, 99 -> "Thunderstorm with Hail";
            default -> "Unknown";
        };
    }

    /** Maps WMO weather codes to a Bootstrap-icons class. */
    static String iconFor(Integer code) {
        if (code == null) return "bi-cloud";
        return switch (code) {
            case 0, 1 -> "bi-sun";
            case 2 -> "bi-cloud-sun";
            case 3 -> "bi-clouds";
            case 45, 48 -> "bi-cloud-fog";
            case 51, 53, 55, 56, 57 -> "bi-cloud-drizzle";
            case 61, 63, 65, 66, 67, 80, 81, 82 -> "bi-cloud-rain";
            case 71, 73, 75, 77, 85, 86 -> "bi-cloud-snow";
            case 95, 96, 99 -> "bi-cloud-lightning-rain";
            default -> "bi-cloud";
        };
    }

    /** Cache entry with TTL. */
    private static final class CacheEntry {
        final List<WeatherForecast> forecasts;
        final Instant expiresAt;

        CacheEntry(List<WeatherForecast> forecasts) {
            this.forecasts = forecasts;
            this.expiresAt = Instant.now().plus(CACHE_TTL);
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
