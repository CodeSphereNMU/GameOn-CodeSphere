package com.gameon.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.gameon.model.dto.VenueResult;
import com.gameon.model.dto.VenueSearchOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Searches for real sports venues in SOUTH AFRICA using the free OpenStreetMap Nominatim API.
 *
 * <p>The search is strictly restricted to South Africa (ISO country code {@code za} plus a
 * bounded viewbox covering the country). Results outside South Africa are discarded.</p>
 *
 * <p>Venue intelligence: the query is expanded with sport-specific keywords and OSM feature
 * classes (leisure / sport) so that the correct kind of venue surfaces for each sport
 * (football fields for football, tennis courts for tennis, pools for swimming, rugby grounds
 * for rugby, etc.). Each result is scored for suitability, tagged INDOOR/OUTDOOR, and enriched
 * with facilities, a website/booking link and a phone number where OSM provides them.</p>
 *
 * <p>Results are cached briefly and duplicate concurrent lookups de-duplicated to honour
 * Nominatim's usage policy and keep the UI responsive.</p>
 */
@Service
public class VenueSearchService {

    private static final Logger logger = LoggerFactory.getLogger(VenueSearchService.class);
    private static final String SEARCH_URL = "https://nominatim.openstreetmap.org/search";
    // Overpass is OSM's POI/feature query API: it is the right tool for discovering sports
    // facilities near a coordinate by tag (leisure/sport/amenity), which Nominatim free-text
    // search does poorly. A public mirror with generous limits is used.
    private static final String OVERPASS_URL = "https://overpass-api.de/api/interpreter";
    private static final Duration CACHE_TTL = Duration.ofMinutes(15);
    // Maximum number of live Nominatim query variants attempted per search (rate-limit friendly).
    private static final int MAX_SEARCH_ATTEMPTS = 4;
    // Default and expanded discovery radius (metres) for Overpass venue searches around an area.
    private static final int DEFAULT_RADIUS_M = 20000;   // ~20 km covers a city + near suburbs
    private static final int EXPANDED_RADIUS_M = 45000;   // fallback when nothing found nearby
    // Nominatim's usage policy requires a valid, identifiable User-Agent with contact details;
    // a generic UA can be throttled/blocked (returning empty results). Keep this descriptive.
    private static final String USER_AGENT =
            "GameOn/1.0 (Sports venue finder; South Africa; contact: support@gameon.co.za)";
    private static final String REFERER = "https://gameon.co.za";

    // South Africa bounding box (lon/lat): west, south, east, north.
    // Used both to bias/bound Nominatim and to reject any stray out-of-country result.
    private static final double ZA_WEST = 16.0;
    private static final double ZA_SOUTH = -35.5;
    private static final double ZA_EAST = 33.5;
    private static final double ZA_NORTH = -22.0;

    private final RestTemplate restTemplate;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    // Per-cacheKey locks so concurrent identical requests share a single external lookup
    // (H3), mirroring WeatherService's de-dup pattern.
    private final Map<String, Object> inFlightLocks = new ConcurrentHashMap<>();

    public VenueSearchService(RestTemplate externalApiRestTemplate) {
        this.restTemplate = externalApiRestTemplate;
    }

    /** Signals that an upstream OSM provider failed (vs. genuinely returning no venues). */
    private static class ProviderException extends RuntimeException {
        final VenueSearchOutcome.Status status;
        ProviderException(VenueSearchOutcome.Status status, String message) {
            super(message);
            this.status = status;
        }
    }

    /**
     * Classify a caught exception into a {@link VenueSearchOutcome.Status} so callers can tell
     * a rate-limit / timeout / outage apart from a genuinely empty area.
     */
    private VenueSearchOutcome.Status classifyProviderError(Exception e) {
        // Rate limited (HTTP 429).
        if (e instanceof HttpClientErrorException hce && hce.getStatusCode().value() == 429) {
            return VenueSearchOutcome.Status.RATE_LIMITED;
        }
        // Timeouts surface as ResourceAccessException wrapping a SocketTimeoutException.
        if (e instanceof ResourceAccessException) {
            Throwable cause = e.getCause();
            String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase(Locale.ROOT);
            if (cause instanceof java.net.SocketTimeoutException
                    || msg.contains("timed out") || msg.contains("timeout")) {
                return VenueSearchOutcome.Status.TIMEOUT;
            }
            // Connection refused / unknown host / other transport failure -> unavailable.
            return VenueSearchOutcome.Status.UNAVAILABLE;
        }
        // Any 5xx server error from the provider.
        if (e instanceof HttpServerErrorException) {
            return VenueSearchOutcome.Status.UNAVAILABLE;
        }
        return VenueSearchOutcome.Status.UNAVAILABLE;
    }

    /**
     * Search for South African sports venues suitable for the given sport, prioritised by
     * distance from the supplied bias location (the user's city/suburb/event location).
     *
     * @param query    free-text search (city / suburb / venue name); may include the sport
     * @param sport    the sport being searched for; drives which venue types are recommended
     * @param biasLat  latitude to prioritise nearby venues (selected location or user); nullable
     * @param biasLng  longitude to prioritise nearby venues; nullable
     */
    public List<VenueResult> search(String query, String sport, String area, Double biasLat, Double biasLng) {
        SportProfile profile = SportProfile.forSport(sport);
        // Allow an empty free-text query as long as we have a sport and/or a location bias:
        // that powers "recommended venues for <sport> near me".
        boolean hasQuery = query != null && !query.isBlank();
        boolean hasBias = biasLat != null && biasLng != null;
        if (!hasQuery && !hasBias) {
            return List.of();
        }

        String expanded = buildQuery(query, area, profile);
        logger.info("[VenueSearch] Area: {}", (area == null || area.isBlank()) ? "(none)" : area);
        logger.info("[VenueSearch] Sport: {}", profile.displayName() == null ? "(any)" : profile.displayName());
        logger.info("[VenueSearch] Search centre latitude: {}", hasBias ? biasLat : "(none)");
        logger.info("[VenueSearch] Search centre longitude: {}", hasBias ? biasLng : "(none)");

        String cacheKey = ((hasQuery ? "q:" + query.trim() : "reco") + "|" + profile.name() + "|"
                + (hasBias ? round2(biasLat) + "," + round2(biasLng) : "za")
                + "|" + (area == null ? "" : area.trim())).toLowerCase(Locale.ROOT);

        CacheEntry cached = cache.get(cacheKey);
        List<VenueResult> results;
        if (cached != null && !cached.isExpired()) {
            results = cached.results;
            logger.info("[VenueSearch] Provider: cache | Results: {}", results.size());
        } else {
            // De-duplicate concurrent identical lookups (same cacheKey) so they don't each hit
            // the external APIs; the first thread discovers, the rest read the freshly cached result.
            Object lock = inFlightLocks.computeIfAbsent(cacheKey, k -> new Object());
            synchronized (lock) {
                try {
                    CacheEntry recheck = cache.get(cacheKey);
                    if (recheck != null && !recheck.isExpired()) {
                        results = recheck.results;
                        logger.info("[VenueSearch] Provider: cache | Results: {}", results.size());
                    } else {
                        results = discoverVenues(query, area, profile, biasLat, biasLng);
                        if (!results.isEmpty()) {
                            cache.put(cacheKey, new CacheEntry(results));
                        }
                    }
                } finally {
                    inFlightLocks.remove(cacheKey);
                }
            }
        }
        logger.info("[VenueSearch] Query: '{}' -> {} venue(s) found", expanded, results.size());

        // Compute distance + score + sort (per-request since the bias location varies).
        List<VenueResult> out = new ArrayList<>(results.size());
        for (VenueResult v : results) {
            VenueResult copy = copyOf(v);
            if (hasBias) {
                copy.setDistanceKm(round1(haversineKm(biasLat, biasLng, copy.getLatitude(), copy.getLongitude())));
            }
            copy.setScore(scoreOf(copy, profile));
            out.add(copy);
        }
        // Sort: best sport match first, then nearest, then higher score.
        out.sort(Comparator
                .comparing((VenueResult v) -> v.isMatchesSport() ? 0 : 1)
                .thenComparing(v -> v.getDistanceKm() == null ? Double.MAX_VALUE : v.getDistanceKm())
                .thenComparing(v -> -v.getScore()));
        return out;
    }

    /**
     * Like {@link #search}, but returns a {@link VenueSearchOutcome} that distinguishes a
     * genuinely empty area from an upstream provider failure (rate-limit / timeout / outage).
     * The client uses the status to show an honest message and to retry (H2).
     */
    public VenueSearchOutcome searchDetailed(String query, String sport, String area, Double biasLat, Double biasLng) {
        try {
            List<VenueResult> results = search(query, sport, area, biasLat, biasLng);
            return VenueSearchOutcome.ok(results);
        } catch (ProviderException pe) {
            logger.warn("[VenueSearch] provider failure ({}): {}", pe.status, pe.getMessage());
            return VenueSearchOutcome.failure(pe.status);
        }
    }

    /**
     * Resolve a free-text place (city / suburb / address) to a coordinate inside South Africa.
     * Returns null when nothing suitable is found within the country.
     */
    public VenueResult geocodeSouthAfrica(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        String cacheKey = "geo|" + query.trim().toLowerCase(Locale.ROOT);
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired() && !cached.results.isEmpty()) {
            return cached.results.get(0);
        }
        try {
            String url = UriComponentsBuilder.fromHttpUrl(SEARCH_URL)
                    .queryParam("q", query.trim())
                    .queryParam("format", "jsonv2")
                    .queryParam("addressdetails", 1)
                    .queryParam("countrycodes", "za")
                    .queryParam("viewbox", ZA_WEST + "," + ZA_NORTH + "," + ZA_EAST + "," + ZA_SOUTH)
                    .queryParam("bounded", 1)
                    .queryParam("limit", 1)
                    .build().encode().toUriString();

            ResponseEntity<JsonNode> resp = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(osmHeaders()), JsonNode.class);
            JsonNode arr = resp.getBody();
            if (arr == null || !arr.isArray() || arr.isEmpty()) {
                return null;
            }
            JsonNode node = arr.get(0);
            double lat = node.path("lat").asDouble();
            double lng = node.path("lon").asDouble();
            if (!inSouthAfrica(lat, lng)) {
                return null;
            }
            VenueResult place = new VenueResult();
            place.setLatitude(lat);
            place.setLongitude(lng);
            place.setName(node.path("display_name").asText(query.trim()));
            place.setAddress(node.path("display_name").asText(""));
            cache.put(cacheKey, new CacheEntry(List.of(place)));
            return place;
        } catch (Exception e) {
            logger.warn("ZA geocode failed for '{}': {}", query, e.getMessage());
            return null;
        }
    }

    /**
     * Reverse-geocode a coordinate to a readable venue name + full South African address.
     * <p>
     * Overpass sports-pitch polygons frequently lack name/address tags (which is why a raw
     * venue often only had "South Africa"); Nominatim's reverse endpoint fills in the real
     * street/suburb/city so the location field shows a meaningful place. A preferred name
     * (e.g. the venue's own OSM name, when present) can be supplied and is kept if non-blank.
     *
     * @return a {@link VenueResult} carrying resolved name + address, or null on failure.
     */
    public VenueResult reverseGeocode(double lat, double lng, String preferredName) {
        if (!inSouthAfrica(lat, lng)) {
            return null;
        }
        String cacheKey = "rev|" + round2(lat) + "," + round2(lng);
        CacheEntry cached = cache.get(cacheKey);
        VenueResult base = (cached != null && !cached.isExpired() && !cached.results.isEmpty())
                ? cached.results.get(0) : null;
        if (base == null) {
            try {
                String url = UriComponentsBuilder
                        .fromHttpUrl("https://nominatim.openstreetmap.org/reverse")
                        .queryParam("lat", lat)
                        .queryParam("lon", lng)
                        .queryParam("format", "jsonv2")
                        .queryParam("addressdetails", 1)
                        .queryParam("zoom", 18)
                        .build().encode().toUriString();

                ResponseEntity<JsonNode> resp = restTemplate.exchange(
                        url, HttpMethod.GET, new HttpEntity<>(osmHeaders()), JsonNode.class);
                JsonNode node = resp.getBody();
                if (node != null && !node.has("error")) {
                    base = new VenueResult();
                    base.setLatitude(lat);
                    base.setLongitude(lng);
                    String display = node.path("display_name").asText("");
                    base.setAddress(display.isBlank() ? "South Africa" : display);

                    // Derive a name: OSM feature name > address "name" > first address segment.
                    JsonNode addr = node.path("address");
                    String osmName = node.path("name").asText("");
                    String derived = firstNonBlank(
                            osmName,
                            addr.path("leisure").asText(""),
                            addr.path("sports_centre").asText(""),
                            addr.path("amenity").asText(""),
                            addr.path("road").asText(""),
                            addr.path("suburb").asText(""),
                            addr.path("neighbourhood").asText(""),
                            addr.path("city").asText(""),
                            addr.path("town").asText(""));
                    base.setName(derived != null ? derived
                            : (display.contains(",") ? display.substring(0, display.indexOf(',')) : display));
                    cache.put(cacheKey, new CacheEntry(List.of(copyOf(base))));
                }
            } catch (Exception e) {
                logger.warn("ZA reverse-geocode failed for {},{}: {}", lat, lng, e.getMessage());
            }
        }

        if (base == null) {
            base = new VenueResult();
            base.setLatitude(lat);
            base.setLongitude(lng);
            base.setAddress("South Africa");
        }
        VenueResult out = copyOf(base);
        // Prefer a meaningful supplied name (e.g. the OSM venue name) over a derived one.
        if (preferredName != null && !preferredName.isBlank()
                && !preferredName.equalsIgnoreCase("Tennis venue")
                && !preferredName.toLowerCase(Locale.ROOT).endsWith(" venue")) {
            out.setName(preferredName.trim());
        } else if (out.getName() == null || out.getName().isBlank()) {
            out.setName(preferredName != null && !preferredName.isBlank() ? preferredName.trim() : "Selected venue");
        }
        return out;
    }

    /**
     * Driving route summary (distance + duration) between two points, via the public OSRM demo
     * server. Returns null when routing is unavailable. Coordinates are lon,lat per OSRM.
     */
    public RouteSummary route(double fromLat, double fromLng, double toLat, double toLng) {
        try {
            String coords = fromLng + "," + fromLat + ";" + toLng + "," + toLat;
            String url = "https://router.project-osrm.org/route/v1/driving/" + coords
                    + "?overview=false&alternatives=false&steps=false";
            ResponseEntity<JsonNode> resp = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(osmHeaders()), JsonNode.class);
            JsonNode body = resp.getBody();
            if (body == null || !"Ok".equals(body.path("code").asText(""))) {
                return null;
            }
            JsonNode route = body.path("routes").path(0);
            if (route.isMissingNode()) {
                return null;
            }
            double meters = route.path("distance").asDouble();
            double seconds = route.path("duration").asDouble();
            return new RouteSummary(round1(meters / 1000.0), (int) Math.round(seconds / 60.0));
        } catch (Exception e) {
            logger.warn("Route lookup failed: {}", e.getMessage());
            return null;
        }
    }

    /** Distance (km) and duration (minutes) for a route. */
    public record RouteSummary(double distanceKm, int durationMinutes) {
    }

    /**
     * Discover venues using the best provider for the request:
     *
     * <ol>
     *   <li><b>Named venue search</b> (user typed a venue name): use Nominatim free-text search,
     *       biased to the selected area, since Overpass cannot match arbitrary commercial names.</li>
     *   <li><b>Area/sport discovery</b> (no venue name, but an area is selected): use the
     *       <b>Overpass API</b> around the area coordinates - this is the reliable way to find
     *       sports facilities by tag. Expand the radius once if nothing is found nearby, then
     *       fall back to a Nominatim category search only if Overpass yields nothing.</li>
     *   <li><b>No location at all</b>: fall back to a country-wide Nominatim category search.</li>
     * </ol>
     *
     * Results are always distance-filtered/sorted by the caller. No venues are ever hardcoded.
     */
    private List<VenueResult> discoverVenues(String query, String area, SportProfile profile,
                                             Double biasLat, Double biasLng) {
        boolean hasQuery = query != null && !query.isBlank();
        boolean hasBias = biasLat != null && biasLng != null;

        // 1) Named-venue search -> Nominatim (Overpass can't match arbitrary names).
        if (hasQuery) {
            List<VenueResult> named = searchWithFallbacks(query, area, profile, biasLat, biasLng);
            logger.info("[VenueSearch] Provider: Nominatim (named venue) | Results: {}", named.size());
            return named;
        }

        // 2) Area/sport discovery -> Overpass first.
        if (hasBias) {
            List<VenueResult> venues = overpassSearch(profile, biasLat, biasLng, DEFAULT_RADIUS_M);
            if (venues.isEmpty()) {
                // Expand once before giving up on the local area.
                venues = overpassSearch(profile, biasLat, biasLng, EXPANDED_RADIUS_M);
            }
            if (!venues.isEmpty()) {
                // Keep only venues within a sensible distance of the selected area so results
                // from a broad match never belong to another city.
                double maxKm = EXPANDED_RADIUS_M / 1000.0;
                List<VenueResult> near = new ArrayList<>();
                for (VenueResult v : venues) {
                    if (haversineKm(biasLat, biasLng, v.getLatitude(), v.getLongitude()) <= maxKm) {
                        near.add(v);
                    }
                }
                if (!near.isEmpty()) {
                    logger.info("[VenueSearch] Provider: Overpass | Results: {}", near.size());
                    logVenueNames(near);
                    return near;
                }
            }
            // 3) Overpass found nothing usable -> Nominatim category fallback (still area-biased).
            List<VenueResult> fallback = searchWithFallbacks(null, area, profile, biasLat, biasLng);
            logger.info("[VenueSearch] Provider: Nominatim (fallback) | Results: {}", fallback.size());
            return fallback;
        }

        // 4) No location -> country-wide Nominatim category search.
        List<VenueResult> country = searchWithFallbacks(null, area, profile, null, null);
        logger.info("[VenueSearch] Provider: Nominatim (country-wide) | Results: {}", country.size());
        return country;
    }

    private void logVenueNames(List<VenueResult> venues) {
        if (!logger.isInfoEnabled()) { return; }
        StringBuilder sb = new StringBuilder();
        int n = Math.min(venues.size(), 10);
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(", ");
            sb.append(venues.get(i).getName());
        }
        logger.info("[VenueSearch] Venue names returned: {}", sb);
    }

    /**
     * Discover sports facilities near a coordinate via the Overpass API.
     *
     * <p>Queries OSM nodes, ways and relations (<code>nwr</code>) around the point for the
     * sport's tags (e.g. <code>sport=padel</code>) plus generic sports-facility feature classes
     * (<code>leisure=pitch/sports_centre/stadium/track/fitness_centre</code>). This finds real
     * venues by their OSM metadata rather than by name, so it works for arbitrary SA areas.</p>
     *
     * @param radiusMeters search radius around the coordinate
     */
    private List<VenueResult> overpassSearch(SportProfile profile, double lat, double lng, int radiusMeters) {
        String ql = buildOverpassQuery(profile, lat, lng, radiusMeters);
        logger.info("[VenueSearch] Radius: {}m", radiusMeters);
        logger.info("[VenueSearch] Provider: Overpass | Query: {}",
                ql.replaceAll("\\s+", " ").trim());

        List<VenueResult> out = new ArrayList<>();
        try {
            HttpHeaders headers = osmHeaders();
            headers.set(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");
            HttpEntity<String> req = new HttpEntity<>("data=" + java.net.URLEncoder.encode(
                    ql, java.nio.charset.StandardCharsets.UTF_8), headers);

            ResponseEntity<JsonNode> resp = restTemplate.exchange(
                    OVERPASS_URL, HttpMethod.POST, req, JsonNode.class);
            JsonNode elements = resp.getBody() == null ? null : resp.getBody().path("elements");
            if (elements == null || !elements.isArray()) {
                return out;
            }
            Set<String> seen = new LinkedHashSet<>();
            for (JsonNode el : elements) {
                VenueResult v = toVenueFromOverpass(el, profile);
                if (v == null || !inSouthAfrica(v.getLatitude(), v.getLongitude())) {
                    continue;
                }
                String key = round2(v.getLatitude()) + "," + round2(v.getLongitude());
                if (seen.add(key)) {
                    out.add(v);
                }
            }
        } catch (Exception e) {
            logger.warn("[VenueSearch] Overpass search failed ({}m around {},{}): {}",
                    radiusMeters, lat, lng, e.getMessage());
            // Surface the failure so callers can distinguish it from a genuinely empty area (H2).
            throw new ProviderException(classifyProviderError(e), e.getMessage());
        }
        return out;
    }

    /**
     * Build the Overpass QL for a sport around a point. Uses the sport's OSM sport tags plus
     * generic sports-facility feature classes so venues are found even when they lack an explicit
     * sport tag. Searches nodes, ways and relations, returning a centre point for ways/relations.
     */
    private String buildOverpassQuery(SportProfile profile, double lat, double lng, int radiusMeters) {
        String around = "(around:" + radiusMeters + "," + lat + "," + lng + ")";
        StringBuilder sb = new StringBuilder();
        sb.append("[out:json][timeout:25];(");

        // Sport-specific tag matches (sport=<tag>) across nwr, e.g. sport~"padel".
        String[] tags = profile.overpassSportTags();
        if (tags.length > 0) {
            String regex = String.join("|", tags);
            sb.append("nwr[\"sport\"~\"").append(regex).append("\",i]").append(around).append(";");
        }

        if (profile == SportProfile.SWIMMING) {
            // Pools are usually leisure=swimming_pool rather than sport=swimming.
            sb.append("nwr[\"leisure\"=\"swimming_pool\"]").append(around).append(";");
            sb.append("nwr[\"leisure\"=\"water_park\"]").append(around).append(";");
        }

        // Generic sports facilities (help when the specific sport tag is missing). For a specific
        // sport we still include these so sports centres/pitches that host the sport surface;
        // the caller marks whether each result actually matches the sport.
        sb.append("nwr[\"leisure\"=\"sports_centre\"]").append(around).append(";");
        sb.append("nwr[\"leisure\"=\"fitness_centre\"]").append(around).append(";");
        if (tags.length > 0) {
            // Pitches/stadia/tracks tagged with this sport.
            String regex = String.join("|", tags);
            sb.append("nwr[\"leisure\"~\"pitch|stadium|track\"][\"sport\"~\"")
              .append(regex).append("\",i]").append(around).append(";");
        } else {
            // Generic profile: any pitch/stadium/track/sports facility.
            sb.append("nwr[\"leisure\"~\"pitch|stadium|track|sports_centre\"]").append(around).append(";");
        }

        sb.append(");out center tags 60;");
        return sb.toString();
    }

    /** Convert an Overpass element (node/way/relation with center) into a {@link VenueResult}. */
    private VenueResult toVenueFromOverpass(JsonNode el, SportProfile profile) {
        try {
            double lat, lng;
            if (el.has("lat") && el.has("lon")) {
                lat = el.path("lat").asDouble();
                lng = el.path("lon").asDouble();
            } else if (el.has("center")) {
                lat = el.path("center").path("lat").asDouble();
                lng = el.path("center").path("lon").asDouble();
            } else {
                return null;
            }
            if (lat == 0.0 && lng == 0.0) {
                return null;
            }

            JsonNode tags = el.path("tags");
            String osmSport = tags.path("sport").asText("").toLowerCase(Locale.ROOT);
            String leisure = tags.path("leisure").asText("");
            String name = firstNonBlank(
                    tags.path("name").asText(""),
                    tags.path("official_name").asText(""),
                    tags.path("operator").asText(""));
            if (name == null || name.isBlank()) {
                // Derive a readable label from the feature type / sport so the pin is meaningful.
                String label = profile.displayName() != null ? profile.displayName()
                        : (leisure.isBlank() ? "Sports" : capitalizeWords(leisure.replace("_", " ")));
                name = label + " venue";
            }

            VenueResult v = new VenueResult();
            v.setLatitude(lat);
            v.setLongitude(lng);
            v.setName(name);
            v.setAddress(buildOsmAddress(tags));
            v.setVenueType(inferVenueType(tags, leisure, name));
            v.setSport(inferSport(tags, name, profile));
            v.setFacilities(extractFacilities(tags, leisure));
            v.setWebsite(firstNonBlank(
                    tags.path("website").asText(""),
                    tags.path("contact:website").asText(""),
                    tags.path("url").asText("")));
            v.setBookingUrl(firstNonBlank(
                    tags.path("reservation").asText(""),
                    tags.path("contact:booking").asText("")));
            v.setPhone(firstNonBlank(
                    tags.path("phone").asText(""),
                    tags.path("contact:phone").asText("")));

            boolean matches = profile.matches(osmSport, leisure, name);
            v.setMatchesSport(matches);
            v.setSuitability(profile.suitabilityText(matches, v.getVenueType()));
            return v;
        } catch (Exception e) {
            return null;
        }
    }

    /** Compose a readable address from OSM address tags; falls back to "South Africa". */
    private String buildOsmAddress(JsonNode tags) {
        StringBuilder sb = new StringBuilder();
        appendPart(sb, tags.path("addr:housenumber").asText(""));
        appendPart(sb, tags.path("addr:street").asText(""));
        appendPart(sb, tags.path("addr:suburb").asText(""));
        appendPart(sb, tags.path("addr:city").asText(""));
        appendPart(sb, tags.path("addr:province").asText(""));
        if (sb.length() == 0) {
            return "South Africa";
        }
        sb.append(", South Africa");
        return sb.toString();
    }

    private void appendPart(StringBuilder sb, String part) {
        if (part != null && !part.isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(part.trim());
        }
    }

    /**
     * Run the venue search with a series of progressively looser query variants, returning the
     * results of the first variant that yields any venues.
     *
     * <p>Real-world OSM data rarely contains venues under their exact commercial name (e.g.
     * "Boardwalk Padel" returns nothing, but "Boardwalk Summerstrand" resolves correctly), so a
     * single strict query is unreliable. Each pass is a genuine live Nominatim search - no
     * hardcoded venues are ever substituted; the fallbacks only reshape the free-text query.</p>
     */
    private List<VenueResult> searchWithFallbacks(String query, String area, SportProfile profile,
                                                  Double biasLat, Double biasLng) {
        List<String> candidates = candidateQueries(query, area, profile);
        // Cap the number of live calls to stay well within Nominatim's usage policy (max ~1
        // request/second). Since bounded=1 is not used, a single biased call already returns
        // country-wide matches as a soft preference, so no separate country-wide retry is needed.
        int attempts = 0;
        for (String candidate : candidates) {
            if (attempts >= MAX_SEARCH_ATTEMPTS) {
                break;
            }
            if (attempts > 0) {
                pause();   // be polite: space out consecutive Nominatim requests
            }
            attempts++;
            List<VenueResult> results = fetch(candidate, profile, biasLat, biasLng);
            if (!results.isEmpty()) {
                logger.debug("[VenueSearch] variant '{}' matched {} venue(s)", candidate, results.size());
                return results;
            }
        }
        return List.of();
    }

    /** Standard headers for OSM/Nominatim requests (policy-compliant identification). */
    private static HttpHeaders osmHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, USER_AGENT);
        headers.set(HttpHeaders.ACCEPT_LANGUAGE, "en");
        headers.set(HttpHeaders.REFERER, REFERER);
        return headers;
    }

    /** Space out consecutive external calls to respect Nominatim's ~1 req/sec policy. */
    private void pause() {
        try {
            Thread.sleep(1100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Build the ordered list of query variants to try, from most specific to broadest.
     * De-duplicated while preserving order.
     */
    private List<String> candidateQueries(String query, String area, SportProfile profile) {
        String q = query == null ? "" : query.trim();
        String a = area == null ? "" : area.trim();
        boolean hasQuery = !q.isBlank();
        boolean hasArea = !a.isBlank();
        String sportKw = profile.recommendationKeyword();

        // The "core" venue name has sport/type words (padel, tennis, courts, ...) stripped, so a
        // commercial name like "Boardwalk Padel" degrades to "Boardwalk" - which OSM often knows
        // even when the full branded name is absent.
        String core = stripSportWords(q);

        List<String> variants = new ArrayList<>();
        if (hasQuery) {
            // Ordered by real-world hit-rate against OSM (the attempt count is capped, so the
            // most likely matches come first). Commercial names ("Boardwalk Padel") are rarely
            // in OSM verbatim, whereas loose place-based queries ("Boardwalk Summerstrand")
            // usually resolve.
            if (hasArea) {
                // 1) Loose "venue area" (skips the area if the venue text already contains it,
                //    so we never build e.g. "Cape Town Cape Town").
                variants.add(joinPlace(q, a));
                // 2) Core venue name + area, sport words dropped ("Boardwalk Summerstrand").
                if (!core.equalsIgnoreCase(q)) {
                    variants.add(joinPlace(core, a));
                }
            }
            // 3) Venue name alone within South Africa.
            variants.add(q + " South Africa");
            // 4) Core venue name within South Africa.
            if (!core.equalsIgnoreCase(q)) {
                variants.add(core + " South Africa");
            }
            // 5) Sport category in the area, so the user still gets relevant venues even when
            //    the exact named venue is absent from OSM.
            if (hasArea) {
                variants.add(sportKw + " near " + a + " South Africa");
            }
            // 6) Venue name alone (unqualified) - last resort.
            variants.add(q);
        } else {
            // Recommendation searches: category near the area, then broaden.
            if (hasArea) {
                variants.add(sportKw + " near " + a + " South Africa");
                variants.add(sportKw + " " + a);
                variants.add(sportKw + " " + a + " South Africa");
            }
            variants.add(sportKw + " South Africa");
        }

        // De-duplicate (case-insensitive) while keeping order.
        List<String> ordered = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String v : variants) {
            String key = v.trim().toLowerCase(Locale.ROOT);
            if (!key.isBlank() && seen.add(key)) {
                ordered.add(v.trim());
            }
        }
        return ordered;
    }

    /**
     * Find real South African sports venues via the OpenStreetMap Nominatim free-text search.
     *
     * <p>This is a live location search: the user's typed venue/area query (already expanded
     * with the sport keyword and the selected suburb by {@link #buildQuery}) is sent to
     * Nominatim, restricted to South Africa. When a bias location is supplied, a viewbox
     * around it is used so the selected suburb/city is prioritised before the rest of the
     * country. No hardcoded venues or coordinates are involved.</p>
     */
    private List<VenueResult> fetch(String query, SportProfile profile, Double biasLat, Double biasLng) {
        List<VenueResult> out = new ArrayList<>();
        try {
            UriComponentsBuilder ub = UriComponentsBuilder.fromHttpUrl(SEARCH_URL)
                    .queryParam("q", query)
                    .queryParam("format", "jsonv2")
                    .queryParam("addressdetails", 1)
                    .queryParam("extratags", 1)
                    .queryParam("namedetails", 1)
                    .queryParam("countrycodes", "za")
                    .queryParam("limit", 25);

            if (biasLat != null && biasLng != null) {
                // Prioritise the selected area with a ~0.35 deg (~35 km) preferred box centred
                // on the supplied bias coordinates. This box is generated dynamically from the
                // *current* bias (the selected area), so different cities get different bounds;
                // there is no fixed city viewbox. bounded=0 keeps it a soft preference so a named
                // venue just outside the box still returns; countrycodes=za keeps it in-country.
                double d = 0.35;
                double west = clampLng(biasLng - d), east = clampLng(biasLng + d);
                double north = clampLat(biasLat + d), south = clampLat(biasLat - d);
                ub.queryParam("viewbox", west + "," + north + "," + east + "," + south);
                logger.info("[VenueSearch] Search centre: {}, {} | bounds(W,N,E,S): {}, {}, {}, {}",
                        biasLat, biasLng, west, north, east, south);
            } else {
                logger.info("[VenueSearch] Search centre: (none - country-wide South Africa)");
            }
            // No hard bounding: countrycodes=za already restricts results to South Africa, and a
            // strict viewbox (bounded=1) was found to suppress valid named-venue matches.

            String url = ub.build().encode().toUriString();
            logger.info("[VenueSearch] Nominatim URL: {}", url);

            ResponseEntity<JsonNode> resp = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(osmHeaders()), JsonNode.class);
            JsonNode arr = resp.getBody();
            if (arr == null || !arr.isArray()) {
                return out;
            }
            Set<String> seen = new LinkedHashSet<>();
            for (JsonNode node : arr) {
                VenueResult v = toVenueFromNominatim(node, profile);
                if (v == null || !inSouthAfrica(v.getLatitude(), v.getLongitude())) {
                    continue;
                }
                // De-duplicate venues that resolve to the same rounded coordinate.
                String key = round2(v.getLatitude()) + "," + round2(v.getLongitude());
                if (seen.add(key)) {
                    out.add(v);
                }
            }
            logger.debug("[VenueSearch] '{}' (Nominatim) -> raw={} kept={}", query, arr.size(), out.size());
        } catch (Exception e) {
            logger.warn("[VenueSearch] ZA venue search failed for '{}': {}", query, e.getMessage());
            // Surface the failure so callers can distinguish it from a genuinely empty area (H2/M3).
            throw new ProviderException(classifyProviderError(e), e.getMessage());
        }
        return out;
    }

    /** Convert a Nominatim jsonv2 result into a {@link VenueResult}. */
    private VenueResult toVenueFromNominatim(JsonNode node, SportProfile profile) {
        try {
            double lat = node.path("lat").asDouble();
            double lng = node.path("lon").asDouble();
            if (lat == 0.0 && lng == 0.0) {
                return null;
            }

            JsonNode address = node.path("address");
            JsonNode nameDetails = node.path("namedetails");
            JsonNode extra = node.path("extratags");
            String display = node.path("display_name").asText("");

            // Prefer an explicit place name; otherwise fall back to the first segment of the
            // display address so the venue always has a readable label.
            String name = firstNonBlank(
                    nameDetails.path("name").asText(""),
                    node.path("name").asText(""),
                    address.path("leisure").asText(""),
                    address.path("sports_centre").asText(""),
                    address.path("amenity").asText(""),
                    display.contains(",") ? display.substring(0, display.indexOf(',')) : display);
            if (name == null || name.isBlank()) {
                String label = profile.displayName() != null ? profile.displayName() : "Sports";
                name = label + " venue";
            }

            VenueResult v = new VenueResult();
            v.setLatitude(lat);
            v.setLongitude(lng);
            v.setName(name);
            v.setAddress(display.isBlank() ? "South Africa" : display);

            String osmSport = firstNonBlankLower(
                    extra.path("sport").asText(""),
                    node.path("type").asText(""));
            String leisure = firstNonBlank(
                    extra.path("leisure").asText(""),
                    node.path("category").asText(""),
                    node.path("type").asText(""));
            leisure = leisure == null ? "" : leisure;

            v.setVenueType(inferVenueType(extra, leisure, name));
            v.setSport(inferSport(extra, name, profile));
            v.setFacilities(extractFacilities(extra, leisure));
            v.setWebsite(firstNonBlank(
                    extra.path("website").asText(""),
                    extra.path("contact:website").asText(""),
                    extra.path("url").asText("")));
            v.setBookingUrl(firstNonBlank(
                    extra.path("reservation").asText(""),
                    extra.path("contact:booking").asText(""),
                    extra.path("booking").asText("")));
            v.setPhone(firstNonBlank(
                    extra.path("phone").asText(""),
                    extra.path("contact:phone").asText("")));

            boolean matches = profile.matches(osmSport, leisure, name);
            v.setMatchesSport(matches);
            v.setSuitability(profile.suitabilityText(matches, v.getVenueType()));
            return v;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Build the free-text search query, prioritising the selected suburb/area.
     *
     * <p>Examples produced (matching the product requirements):</p>
     * <ul>
     *   <li>venue "Boardwalk Padel" + area "Summerstrand" -&gt; "Boardwalk Padel, Summerstrand, South Africa"</li>
     *   <li>no venue + Padel + area "Summerstrand" -&gt; "Padel courts near Summerstrand South Africa"</li>
     *   <li>no venue + Tennis, no area -&gt; "Tennis courts South Africa"</li>
     * </ul>
     */
    private String buildQuery(String query, String area, SportProfile profile) {
        String q = query == null ? "" : query.trim();
        String a = area == null ? "" : area.trim();
        boolean hasQuery = !q.isBlank();
        boolean hasArea = !a.isBlank();
        String lower = q.toLowerCase(Locale.ROOT);

        StringBuilder sb = new StringBuilder();
        if (hasQuery) {
            // Named venue / free text search: keep the user's text first so Nominatim matches
            // the venue by name, then the selected suburb, then anchor to the country. e.g.
            // "Boardwalk Padel, Summerstrand, South Africa".
            sb.append(q);
            if (hasArea && !lower.contains(a.toLowerCase(Locale.ROOT))) {
                sb.append(", ").append(a);
            }
            if (!lower.contains("south africa") && !a.toLowerCase(Locale.ROOT).contains("south africa")) {
                sb.append(", South Africa");
            }
        } else {
            // Recommendation search: "<Sport> courts near <area> South Africa". e.g.
            // "Padel courts near Summerstrand South Africa".
            sb.append(profile.recommendationKeyword());
            if (hasArea) {
                sb.append(" near ").append(a);
            }
            sb.append(" South Africa");
        }
        return sb.toString().trim();
    }

    // Sport / facility words removed from a venue name to obtain its "core" (place) name.
    private static final Set<String> SPORT_WORDS = Set.of(
            "padel", "tennis", "squash", "basketball", "netball", "hockey", "cricket",
            "rugby", "soccer", "football", "swimming", "pool", "pools", "golf",
            "court", "courts", "field", "fields", "ground", "grounds", "centre", "center",
            "club", "sports", "sport");

    /**
     * Strip sport/facility words from a venue name to derive its core place name, e.g.
     * "Boardwalk Padel" -&gt; "Boardwalk", "Crusaders Sports Club" -&gt; "Crusaders".
     * Falls back to the original text if stripping would leave nothing.
     */
    private static String stripSportWords(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String word : name.trim().split("\\s+")) {
            String bare = word.replaceAll("[^\\p{L}\\p{Nd}]", "").toLowerCase(Locale.ROOT);
            if (bare.isEmpty() || SPORT_WORDS.contains(bare)) {
                continue;
            }
            if (sb.length() > 0) sb.append(' ');
            sb.append(word);
        }
        String core = sb.toString().trim();
        return core.isEmpty() ? name.trim() : core;
    }

    /**
     * Join a venue/place term with an area, avoiding duplication: if the term already contains
     * the area (case-insensitive), the term is returned unchanged. Prevents malformed queries
     * such as "Cape Town Cape Town".
     */
    private static String joinPlace(String term, String area) {
        String t = term == null ? "" : term.trim();
        String a = area == null ? "" : area.trim();
        if (a.isBlank()) {
            return t;
        }
        if (t.toLowerCase(Locale.ROOT).contains(a.toLowerCase(Locale.ROOT))) {
            return t;
        }
        return (t.isBlank() ? a : t + " " + a);
    }

    private static String firstNonBlankLower(String... vals) {
        String v = firstNonBlank(vals);
        return v == null ? "" : v.toLowerCase(Locale.ROOT);
    }

    private static double clampLat(double lat) {
        return Math.max(-90.0, Math.min(90.0, lat));
    }

    private static double clampLng(double lng) {
        return Math.max(-180.0, Math.min(180.0, lng));
    }

    private String inferVenueType(JsonNode tags, String leisure, String name) {
        String building = tags.path("building").asText("");
        String indoorTag = tags.path("indoor").asText("");
        String sport = tags.path("sport").asText("").toLowerCase(Locale.ROOT);
        String n = name == null ? "" : name.toLowerCase(Locale.ROOT);
        boolean indoorSport = sport.contains("squash") || sport.contains("swimming")
                || sport.contains("gym") || sport.contains("fitness")
                || n.contains("squash") || n.contains("gym") || n.contains("virgin active");
        if ("sports_centre".equals(leisure) || "sports_hall".equals(leisure)
                || "yes".equalsIgnoreCase(indoorTag) || !building.isBlank() || indoorSport) {
            return "INDOOR";
        }
        if ("pitch".equals(leisure) || "stadium".equals(leisure) || "track".equals(leisure)) {
            return "OUTDOOR";
        }
        return "OUTDOOR";
    }

    private String inferSport(JsonNode extra, String name, SportProfile profile) {
        String sport = extra.path("sport").asText("");
        if (!sport.isBlank()) {
            return capitalizeWords(sport.replace("_", " ").replace(";", ", "));
        }
        String n = name == null ? "" : name.toLowerCase(Locale.ROOT);
        for (String s : new String[]{"padel", "tennis", "squash", "basketball", "rugby", "soccer",
                "football", "cricket", "swimming", "pool", "hockey", "netball", "golf"}) {
            if (n.contains(s)) {
                return capitalizeWords(s);
            }
        }
        // Fall back to the sport we were searching for.
        return profile.displayName() != null ? profile.displayName() : "Multi-sport";
    }

    /** Pull human-friendly facilities out of common OSM tags. */
    private List<String> extractFacilities(JsonNode tags, String leisure) {
        Set<String> f = new LinkedHashSet<>();
        if (yes(tags, "lit")) f.add("Floodlights");
        if (yes(tags, "parking") || !tags.path("parking").asText("").isBlank()) f.add("Parking");
        if (yes(tags, "shower") || yes(tags, "changing_room") || yes(tags, "changing_rooms")) f.add("Changerooms");
        if (yes(tags, "toilets")) f.add("Toilets");
        if (yes(tags, "wheelchair")) f.add("Wheelchair access");
        if (yes(tags, "covered")) f.add("Covered");
        String surface = tags.path("surface").asText("");
        if (!surface.isBlank()) f.add("Surface: " + capitalizeWords(surface.replace("_", " ")));
        String access = tags.path("access").asText("");
        if ("public".equalsIgnoreCase(access)) f.add("Public access");
        if ("sports_centre".equals(leisure)) {
            f.add("Sports centre");
        }
        return new ArrayList<>(f);
    }

    /** Score a venue: sport match dominates, then facilities richness. */
    private int scoreOf(VenueResult v, SportProfile profile) {
        int s = 0;
        if (v.isMatchesSport()) s += 100;
        if (v.getFacilities() != null) s += Math.min(v.getFacilities().size() * 3, 15);
        if (v.getWebsite() != null && !v.getWebsite().isBlank()) s += 5;
        if (v.getPhone() != null && !v.getPhone().isBlank()) s += 3;
        return s;
    }

    // ===== helpers =====

    private static boolean inSouthAfrica(double lat, double lng) {
        return lat <= ZA_NORTH && lat >= ZA_SOUTH && lng >= ZA_WEST && lng <= ZA_EAST;
    }

    private static boolean yes(JsonNode extra, String key) {
        String v = extra.path(key).asText("");
        return "yes".equalsIgnoreCase(v) || "true".equalsIgnoreCase(v);
    }

    private static String firstNonBlank(String... vals) {
        for (String v : vals) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

    private static String capitalizeWords(String s) {
        if (s == null || s.isBlank()) return s;
        StringBuilder sb = new StringBuilder(s.length());
        boolean cap = true;
        for (char c : s.toCharArray()) {
            if (Character.isWhitespace(c) || c == ',') {
                cap = true;
                sb.append(c);
            } else if (cap) {
                sb.append(Character.toUpperCase(c));
                cap = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static VenueResult copyOf(VenueResult v) {
        VenueResult c = new VenueResult();
        c.setName(v.getName());
        c.setAddress(v.getAddress());
        c.setLatitude(v.getLatitude());
        c.setLongitude(v.getLongitude());
        c.setVenueType(v.getVenueType());
        c.setSport(v.getSport());
        c.setFacilities(v.getFacilities());
        c.setWebsite(v.getWebsite());
        c.setBookingUrl(v.getBookingUrl());
        c.setPhone(v.getPhone());
        c.setSuitability(v.getSuitability());
        c.setMatchesSport(v.isMatchesSport());
        return c;
    }

    static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private static Double round1(double d) {
        return Math.round(d * 10.0) / 10.0;
    }

    private static double round2(double d) {
        return Math.round(d * 100.0) / 100.0;
    }

    /**
     * Maps a GameOn sport to the OSM feature keywords / sport tags that identify a suitable
     * venue, plus suitability copy. Recognises the common GameOn sports and degrades to a
     * generic "sports venue" profile for anything else.
     */
    private enum SportProfile {
        FOOTBALL("Soccer", "soccer field", new String[]{"soccer", "football"},
                new String[]{"soccer", "football", "multi"}),
        RUGBY("Rugby", "rugby field", new String[]{"rugby", "rugby_union", "rugby_league"},
                new String[]{"rugby"}),
        TENNIS("Tennis", "tennis court", new String[]{"tennis"}, new String[]{"tennis"}),
        PADEL("Padel", "padel court", new String[]{"padel"}, new String[]{"padel", "tennis"}),
        SQUASH("Squash", "squash court", new String[]{"squash"}, new String[]{"squash"}),
        BASKETBALL("Basketball", "basketball court", new String[]{"basketball"}, new String[]{"basketball", "multi"}),
        CRICKET("Cricket", "cricket ground", new String[]{"cricket"}, new String[]{"cricket"}),
        SWIMMING("Swimming", "swimming pool", new String[]{"swimming", "swimming_pool"},
                new String[]{"swimming", "water"}),
        HOCKEY("Hockey", "hockey field", new String[]{"hockey", "field_hockey"}, new String[]{"hockey"}),
        NETBALL("Netball", "netball court", new String[]{"netball"}, new String[]{"netball", "multi"}),
        GENERIC(null, "sports venue", new String[]{}, new String[]{});

        private final String displayName;
        private final String primaryKeyword;
        private final String[] tags;      // OSM sport tags that clearly match
        private final String[] softTags;  // additional acceptable tags

        SportProfile(String displayName, String primaryKeyword, String[] tags, String[] softTags) {
            this.displayName = displayName;
            this.primaryKeyword = primaryKeyword;
            this.tags = tags;
            this.softTags = softTags;
        }

        static SportProfile forSport(String sport) {
            if (sport == null) return GENERIC;
            String s = sport.trim().toLowerCase(Locale.ROOT);
            if (s.contains("soccer") || s.contains("football")) return FOOTBALL;
            if (s.contains("rugby")) return RUGBY;
            if (s.contains("padel")) return PADEL;
            if (s.contains("tennis")) return TENNIS;
            if (s.contains("squash")) return SQUASH;
            if (s.contains("basketball")) return BASKETBALL;
            if (s.contains("cricket")) return CRICKET;
            if (s.contains("swim")) return SWIMMING;
            if (s.contains("hockey")) return HOCKEY;
            if (s.contains("netball")) return NETBALL;
            return GENERIC;
        }

        String displayName() { return displayName; }
        String primaryKeyword() { return primaryKeyword; }

        /**
         * OSM {@code sport=*} tag values used to discover this sport via Overpass, including
         * common synonyms/spellings (e.g. padel is sometimes tagged "paddle"/"padel_tennis").
         * The values form a case-insensitive regex alternation.
         */
        String[] overpassSportTags() {
            return switch (this) {
                case FOOTBALL -> new String[]{"soccer", "football"};
                case RUGBY -> new String[]{"rugby", "rugby_union", "rugby_league"};
                case TENNIS -> new String[]{"tennis"};
                case PADEL -> new String[]{"padel", "paddle", "padel_tennis"};
                case SQUASH -> new String[]{"squash"};
                case BASKETBALL -> new String[]{"basketball"};
                case CRICKET -> new String[]{"cricket"};
                case SWIMMING -> new String[]{"swimming"};
                case HOCKEY -> new String[]{"hockey", "field_hockey"};
                case NETBALL -> new String[]{"netball"};
                case GENERIC -> new String[]{};
            };
        }

        /**
         * Free-text keyword used for the "recommended venues" search when the user has not typed
         * a specific venue name, e.g. "Padel courts", "Tennis courts", "Swimming pools".
         * The facility noun follows how the venue type is commonly named/tagged in OSM.
         */
        String recommendationKeyword() {
            return switch (this) {
                case GENERIC -> "sports venues";
                case SWIMMING -> "swimming pools";
                case FOOTBALL, RUGBY, CRICKET, HOCKEY -> displayName + " fields";
                default -> displayName + " courts"; // TENNIS, PADEL, SQUASH, BASKETBALL, NETBALL
            };
        }

        boolean matches(String osmSport, String type, String name) {
            String n = name == null ? "" : name.toLowerCase(Locale.ROOT);
            for (String t : tags) {
                if ((osmSport != null && osmSport.contains(t)) || n.contains(t)) return true;
            }
            for (String t : softTags) {
                if (osmSport != null && osmSport.contains(t)) return true;
            }
            return false;
        }

        String suitabilityText(boolean matches, String venueType) {
            String sportLabel = displayName == null ? "your sport" : displayName;
            if (matches) {
                return "Well suited for " + sportLabel;
            }
            return "General sports venue - confirm " + sportLabel + " facilities before booking";
        }
    }

    private static final class CacheEntry {
        final List<VenueResult> results;
        final Instant expiresAt;

        CacheEntry(List<VenueResult> results) {
            this.results = results;
            this.expiresAt = Instant.now().plus(CACHE_TTL);
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
