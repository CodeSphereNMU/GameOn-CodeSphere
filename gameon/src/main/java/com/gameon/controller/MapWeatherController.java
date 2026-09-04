package com.gameon.controller;

import com.gameon.model.dto.PlayabilityResult;
import com.gameon.model.dto.VenueResult;
import com.gameon.model.dto.WeatherForecast;
import com.gameon.model.entity.GameListing;
import com.gameon.model.enums.VenueType;
import com.gameon.security.CustomUserDetails;
import com.gameon.service.GameJoinerService;
import com.gameon.service.GameListingService;
import com.gameon.service.PlayabilityService;
import com.gameon.service.VenueSearchService;
import com.gameon.service.WeatherService;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON API backing the client-side Map + Weather + Playability features.
 *
 * <p>All endpoints require an authenticated user (enforced by the global security rules).
 * They proxy the keyless external APIs server-side so the browser never calls them directly,
 * and so results can be cached centrally.</p>
 */
@RestController
@RequestMapping("/api")
public class MapWeatherController {

    private final VenueSearchService venueSearchService;
    private final WeatherService weatherService;
    private final PlayabilityService playabilityService;
    private final GameListingService gameListingService;
    private final GameJoinerService gameJoinerService;

    public MapWeatherController(VenueSearchService venueSearchService,
                                WeatherService weatherService,
                                PlayabilityService playabilityService,
                                GameListingService gameListingService,
                                GameJoinerService gameJoinerService) {
        this.venueSearchService = venueSearchService;
        this.weatherService = weatherService;
        this.playabilityService = playabilityService;
        this.gameListingService = gameListingService;
        this.gameJoinerService = gameJoinerService;
    }

    /**
     * Recommended South African sports venues for the given sport, prioritised near the
     * supplied bias location (selected city/suburb/event location, or the user's location).
     * The {@code q} query is optional when a bias location is supplied.
     */
    @GetMapping("/venues/search")
    public List<VenueResult> searchVenues(@RequestParam(required = false, defaultValue = "") String q,
                                          @RequestParam(required = false) String sport,
                                          @RequestParam(required = false) Double lat,
                                          @RequestParam(required = false) Double lng) {
        return venueSearchService.search(q, sport, lat, lng);
    }

    /**
     * Geocode a free-text place (city / suburb / address) to coordinates, restricted to
     * South Africa. Used to centre the map and bias venue recommendations on the location
     * the user typed. Returns null fields when nothing could be resolved in South Africa.
     */
    @GetMapping("/geocode")
    public Map<String, Object> geocode(@RequestParam String q) {
        Map<String, Object> out = new LinkedHashMap<>();
        VenueResult place = venueSearchService.geocodeSouthAfrica(q);
        if (place == null) {
            out.put("found", false);
            return out;
        }
        out.put("found", true);
        out.put("name", place.getName());
        out.put("address", place.getAddress());
        out.put("lat", place.getLatitude());
        out.put("lng", place.getLongitude());
        return out;
    }

    /**
     * Reverse-geocode a coordinate (South Africa) to a real venue name + full address.
     * Used when a venue is selected so the location field shows a meaningful place rather
     * than only "South Africa". An optional {@code name} hint is preserved when meaningful.
     */
    @GetMapping("/reverse-geocode")
    public Map<String, Object> reverseGeocode(@RequestParam Double lat,
                                              @RequestParam Double lng,
                                              @RequestParam(required = false) String name) {
        Map<String, Object> out = new LinkedHashMap<>();
        VenueResult place = venueSearchService.reverseGeocode(lat, lng, name);
        if (place == null) {
            out.put("found", false);
            return out;
        }
        out.put("found", true);
        out.put("name", place.getName());
        out.put("address", place.getAddress());
        out.put("lat", place.getLatitude());
        out.put("lng", place.getLongitude());
        return out;
    }

    /**
     * Driving distance + estimated travel time between an origin and a destination.
     * Returns {@code available:false} when routing data is unavailable.
     */
    @GetMapping("/route")
    public Map<String, Object> route(@RequestParam Double fromLat,
                                     @RequestParam Double fromLng,
                                     @RequestParam Double toLat,
                                     @RequestParam Double toLng) {
        Map<String, Object> out = new LinkedHashMap<>();
        VenueSearchService.RouteSummary r = venueSearchService.route(fromLat, fromLng, toLat, toLng);
        if (r == null) {
            out.put("available", false);
            return out;
        }
        out.put("available", true);
        out.put("distanceKm", r.distanceKm());
        out.put("durationMinutes", r.durationMinutes());
        return out;
    }

    /** Weather forecast at a match time for a venue, plus the playability verdict. */
    @GetMapping("/weather")
    public Map<String, Object> weather(@RequestParam Double lat,
                                       @RequestParam Double lng,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime time,
                                       @RequestParam(required = false) String sport,
                                       @RequestParam(required = false) String venueType) {
        WeatherForecast forecast = weatherService.getForecast(lat, lng, time);
        PlayabilityResult playability =
                playabilityService.evaluate(sport, parseVenueType(venueType), forecast);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("weather", forecast);
        out.put("playability", playability);
        return out;
    }

    /** Standalone playability evaluation (used when weather is already known client-side). */
    @GetMapping("/playability")
    public PlayabilityResult playability(@RequestParam Double lat,
                                         @RequestParam Double lng,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime time,
                                         @RequestParam(required = false) String sport,
                                         @RequestParam(required = false) String venueType) {
        WeatherForecast forecast = weatherService.getForecast(lat, lng, time);
        return playabilityService.evaluate(sport, parseVenueType(venueType), forecast);
    }

    /**
     * Suggested alternative dates over the next 7 days at the same venue/time-of-day
     * where the playability is better (GOOD, or at least FAIR when the original was POOR).
     */
    @GetMapping("/weather/alternatives")
    public List<Map<String, Object>> alternatives(@RequestParam Double lat,
                                                   @RequestParam Double lng,
                                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime time,
                                                   @RequestParam(required = false) String sport,
                                                   @RequestParam(required = false) String venueType) {
        VenueType vt = parseVenueType(venueType);
        List<WeatherForecast> daily = weatherService.getDailyForecasts(lat, lng, time, 7);
        List<Map<String, Object>> suggestions = new ArrayList<>();
        for (WeatherForecast f : daily) {
            // Skip the original day.
            if (f.getDate() != null && f.getDate().equals(time.toLocalDate())) {
                continue;
            }
            PlayabilityResult p = playabilityService.evaluate(sport, vt, f);
            if (p.getRating() == PlayabilityResult.Rating.GOOD
                    || p.getRating() == PlayabilityResult.Rating.FAIR) {
                Map<String, Object> s = new LinkedHashMap<>();
                s.put("dateTime", f.getTime());
                s.put("weather", f);
                s.put("playability", p);
                suggestions.add(s);
            }
            if (suggestions.size() >= 3) {
                break;
            }
        }
        return suggestions;
    }

    /**
     * Active public listings that have map coordinates, for the Listings "Map View".
     * Reuses the same browse query as the list view (read-only; no lifecycle changes).
     */
    @GetMapping("/listings/map")
    public List<Map<String, Object>> listingsForMap(@AuthenticationPrincipal CustomUserDetails currentUser) {
        List<GameListing> listings = gameListingService
                .browseAvailableListings(currentUser.getUserId(), PageRequest.of(0, 200))
                .getContent();

        List<Map<String, Object>> out = new ArrayList<>();
        for (GameListing l : listings) {
            if (!l.hasCoordinates()) {
                continue;
            }
            long participants = gameJoinerService.countCurrentParticipants(l.getGameListingId());
            long capacity = l.getFormat().getNoPlayers();
            long remaining = Math.max(0, capacity - participants);

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", l.getGameListingId());
            m.put("sport", l.getFormat().getSport().getSportName());
            m.put("format", l.getFormat().getFormatName());
            m.put("venue", l.getVenueName() != null ? l.getVenueName() : l.getLocation());
            m.put("address", l.getLocation());
            m.put("lat", l.getLatitude());
            m.put("lng", l.getLongitude());
            m.put("venueType", l.getVenueType() != null ? l.getVenueType().name() : null);
            m.put("scheduledDate", l.getScheduledDate());
            m.put("status", l.getEffectiveStatus());
            m.put("remaining", remaining);
            m.put("capacity", capacity);
            m.put("full", remaining == 0);

            // Attach a lightweight forecast + playability for the popup.
            WeatherForecast forecast = weatherService.getForecast(
                    l.getLatitude().doubleValue(), l.getLongitude().doubleValue(), l.getScheduledDate());
            PlayabilityResult playability = playabilityService.evaluate(
                    l.getFormat().getSport().getSportName(), l.getVenueType(), forecast);
            m.put("weather", forecast);
            m.put("playability", playability);

            out.add(m);
        }
        return out;
    }

    private VenueType parseVenueType(String venueType) {
        if (venueType == null || venueType.isBlank()) {
            return null;
        }
        try {
            return VenueType.valueOf(venueType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
