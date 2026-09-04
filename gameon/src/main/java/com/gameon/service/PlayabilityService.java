package com.gameon.service;

import com.gameon.model.dto.PlayabilityResult;
import com.gameon.model.dto.WeatherForecast;
import com.gameon.model.enums.VenueType;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Playability engine.
 *
 * <p>Determines whether weather conditions are suitable for a chosen sport at a chosen
 * venue. Indoor sports/venues are unaffected by weather. Outdoor conditions are graded
 * GOOD / FAIR / POOR using the agreed thresholds.</p>
 *
 * <p>Rules (outdoor):</p>
 * <ul>
 *   <li>POOR when rain probability &gt; 50%, wind &gt; 35 km/h, temp &gt; 38&deg;C, or temp &lt; 5&deg;C</li>
 *   <li>FAIR when conditions are borderline (moderate wind/rain/heat/cold) but still playable</li>
 *   <li>GOOD otherwise</li>
 * </ul>
 */
@Service
public class PlayabilityService {

    // Sports that are inherently indoor regardless of the venue type flag.
    private static final Set<String> ALWAYS_INDOOR_SPORTS = Set.of(
            "SQUASH", "INDOOR PADEL", "INDOOR TENNIS", "INDOOR BASKETBALL");

    // POOR thresholds
    private static final int RAIN_POOR = 50;      // %
    private static final double WIND_POOR = 35.0; // km/h
    private static final double TEMP_HOT_POOR = 38.0;
    private static final double TEMP_COLD_POOR = 5.0;

    // FAIR (borderline) thresholds
    private static final int RAIN_FAIR = 30;      // %
    private static final double WIND_FAIR = 25.0; // km/h
    private static final double TEMP_HOT_FAIR = 33.0;
    private static final double TEMP_COLD_FAIR = 8.0;

    /**
     * Evaluate playability for a sport at a venue given a forecast.
     *
     * @param sportName the sport name (case-insensitive), may be null
     * @param venueType INDOOR / OUTDOOR; null is treated as OUTDOOR (weather-sensitive)
     * @param forecast  the forecast at match time; may be null/unavailable
     */
    public PlayabilityResult evaluate(String sportName, VenueType venueType, WeatherForecast forecast) {
        boolean indoor = isIndoor(sportName, venueType);

        if (indoor) {
            return new PlayabilityResult(
                    PlayabilityResult.Rating.GOOD,
                    "Indoor venue selected. Weather conditions do not impact playability.",
                    true);
        }

        if (forecast == null || !forecast.isAvailable()) {
            return new PlayabilityResult(
                    PlayabilityResult.Rating.UNKNOWN,
                    "Forecast unavailable. Check conditions closer to match time.",
                    false);
        }

        Double temp = forecast.getTemperatureC();
        Integer rain = forecast.getRainChancePercent();
        Double wind = forecast.getWindSpeedKmh();

        // ----- POOR checks -----
        if (rain != null && rain > RAIN_POOR) {
            return poor("High probability of rain. Outdoor play may not be enjoyable.");
        }
        if (wind != null && wind > WIND_POOR) {
            return poor("Strong winds expected. Outdoor play may be difficult.");
        }
        if (temp != null && temp > TEMP_HOT_POOR) {
            return poor("Extreme heat expected. Outdoor play is not advisable.");
        }
        if (temp != null && temp < TEMP_COLD_POOR) {
            return poor("Very cold conditions expected. Outdoor play may be uncomfortable.");
        }

        // ----- FAIR (borderline) checks -----
        if (rain != null && rain >= RAIN_FAIR) {
            return fair("Some chance of rain. Players should prepare accordingly.");
        }
        if (wind != null && wind >= WIND_FAIR) {
            return fair("Moderate wind expected. Players should prepare accordingly.");
        }
        if (temp != null && temp >= TEMP_HOT_FAIR) {
            return fair("Warm conditions expected. Stay hydrated and take breaks.");
        }
        if (temp != null && temp <= TEMP_COLD_FAIR) {
            return fair("Cool conditions expected. Players should dress warmly.");
        }

        // ----- GOOD -----
        return new PlayabilityResult(
                PlayabilityResult.Rating.GOOD,
                "Conditions look suitable for outdoor play.",
                false);
    }

    /** True if the sport is inherently indoor OR the venue is flagged INDOOR. */
    public boolean isIndoor(String sportName, VenueType venueType) {
        if (venueType == VenueType.INDOOR) {
            return true;
        }
        if (sportName != null && ALWAYS_INDOOR_SPORTS.contains(sportName.trim().toUpperCase())) {
            return true;
        }
        return false;
    }

    private PlayabilityResult poor(String message) {
        return new PlayabilityResult(PlayabilityResult.Rating.POOR, message, false);
    }

    private PlayabilityResult fair(String message) {
        return new PlayabilityResult(PlayabilityResult.Rating.FAIR, message, false);
    }
}
