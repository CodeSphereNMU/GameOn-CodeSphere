package com.gameon.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.gameon.model.dto.VenueResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
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
    // Overpass is OSM's query API and is designed for automated feature lookups by tag;
    // it is used to find actual sports facilities (pitches, courts, pools) near a location.
    private static final String OVERPASS_URL = "https://overpass-api.de/api/interpreter";
    private static final Duration CACHE_TTL = Duration.ofMinutes(15);

    // South Africa bounding box (lon/lat): west, south, east, north.
    // Used both to bias/bound Nominatim and to reject any stray out-of-country result.
    private static final double ZA_WEST = 16.0;
    private static final double ZA_SOUTH = -35.5;
    private static final double ZA_EAST = 33.5;
    private static final double ZA_NORTH = -22.0;

    private final RestTemplate restTemplate;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public VenueSearchService(RestTemplate externalApiRestTemplate) {
        this.restTemplate = externalApiRestTemplate;
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
    public List<VenueResult> search(String query, String sport, Double biasLat, Double biasLng) {
        SportProfile profile = SportProfile.forSport(sport);
        // Allow an empty free-text query as long as we have a sport and/or a location bias:
        // that powers "recommended venues for <sport> near me".
        boolean hasQuery = query != null && !query.isBlank();
        boolean hasBias = biasLat != null && biasLng != null;
        if (!hasQuery && !hasBias) {
            return List.of();
        }

        String expanded = buildQuery(query, profile);
        String cacheKey = (expanded + "|" + (hasBias ? round2(biasLat) + "," + round2(biasLng) : "za"))
                .toLowerCase(Locale.ROOT);

        CacheEntry cached = cache.get(cacheKey);
        List<VenueResult> results;
        if (cached != null && !cached.isExpired()) {
            results = cached.results;
        } else {
            results = fetch(expanded, profile, biasLat, biasLng);
            // Fallback: if a location-biased search returns nothing (sparse OSM area or a
            // transient empty response), retry once as a country-wide South Africa search so
            // the user still gets relevant recommendations.
            if (results.isEmpty() && hasBias) {
                results = fetch(expanded, profile, null, null);
            }
            if (!results.isEmpty()) {
                cache.put(cacheKey, new CacheEntry(results));
            }
        }

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

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT, "GameOn/1.0 (South Africa sports venue finder)");
            headers.set(HttpHeaders.ACCEPT_LANGUAGE, "en");

            ResponseEntity<JsonNode> resp = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
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

                HttpHeaders headers = new HttpHeaders();
                headers.set(HttpHeaders.USER_AGENT, "GameOn/1.0 (South Africa sports venue finder)");
                headers.set(HttpHeaders.ACCEPT_LANGUAGE, "en");

                ResponseEntity<JsonNode> resp = restTemplate.exchange(
                        url, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
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
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT, "GameOn/1.0 (South Africa sports venue finder)");
            ResponseEntity<JsonNode> resp = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
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
     * Find actual South African sports facilities via the Overpass API. Overpass is the OSM
     * query service intended for automated, tag-based feature lookups (unlike Nominatim, whose
     * usage policy discourages automated free-text search from servers), so it reliably returns
     * pitches/courts/pools/grounds tagged for the requested sport near the bias location.
     */
    private List<VenueResult> fetch(String query, SportProfile profile, Double biasLat, Double biasLng) {
        List<VenueResult> out = new ArrayList<>();
        try {
            String area = overpassArea(biasLat, biasLng);
            String filter = profile.overpassFilter();
            // Search nodes, ways and relations that are sports facilities for this sport.
            String ql = "[out:json][timeout:20];(" + filter.replace("{{A}}", area) + ");out center tags 40;";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);
            headers.set(HttpHeaders.USER_AGENT, "GameOn/1.0 (South Africa sports venue finder)");
            HttpEntity<String> entity = new HttpEntity<>("data=" + java.net.URLEncoder.encode(ql, java.nio.charset.StandardCharsets.UTF_8), headers);

            ResponseEntity<JsonNode> resp =
                    restTemplate.exchange(OVERPASS_URL, HttpMethod.POST, entity, JsonNode.class);
            JsonNode root = resp.getBody();
            JsonNode elements = root == null ? null : root.path("elements");
            if (elements == null || !elements.isArray()) {
                return out;
            }
            for (JsonNode el : elements) {
                VenueResult v = toVenue(el, profile);
                if (v != null && inSouthAfrica(v.getLatitude(), v.getLongitude())) {
                    out.add(v);
                }
            }
            logger.debug("ZA venue search '{}' (Overpass) -> raw={} kept={}", profile.displayName(),
                    elements.size(), out.size());
        } catch (Exception e) {
            logger.warn("ZA venue search failed for '{}': {}", query, e.getMessage());
        }
        return out;
    }

    /**
     * Build the Overpass area clause. When a bias location is supplied, search a radius around
     * it; otherwise search a bounding box covering all of South Africa.
     */
    private String overpassArea(Double biasLat, Double biasLng) {
        if (biasLat != null && biasLng != null) {
            // 30 km radius around the selected location.
            return "around:30000," + biasLat + "," + biasLng;
        }
        // South Africa bounding box: (south, west, north, east).
        return ZA_SOUTH + "," + ZA_WEST + "," + ZA_NORTH + "," + ZA_EAST;
    }

    private VenueResult toVenue(JsonNode el, SportProfile profile) {
        try {
            JsonNode tags = el.path("tags");
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

            VenueResult v = new VenueResult();
            v.setLatitude(lat);
            v.setLongitude(lng);

            String osmSport = tags.path("sport").asText("").toLowerCase(Locale.ROOT);
            String leisure = tags.path("leisure").asText("");
            String name = firstNonBlank(tags.path("name").asText(""), tags.path("official_name").asText(""));
            if (name == null || name.isBlank()) {
                // Derive a readable name from the sport + feature type.
                String label = profile.displayName() != null ? profile.displayName() : capitalizeWords(osmSport);
                name = (label == null || label.isBlank() ? "Sports" : label) + " venue";
            }
            v.setName(name);
            v.setAddress(buildAddress(tags));

            v.setVenueType(inferVenueType(tags, leisure));
            v.setSport(inferSport(tags, name, profile));
            v.setFacilities(extractFacilities(tags, leisure));
            v.setWebsite(firstNonBlank(
                    tags.path("website").asText(""),
                    tags.path("contact:website").asText(""),
                    tags.path("url").asText("")));
            v.setBookingUrl(firstNonBlank(
                    tags.path("reservation").asText(""),
                    tags.path("contact:booking").asText(""),
                    tags.path("booking").asText("")));
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

    /** Compose a readable address from OSM address tags. */
    private String buildAddress(JsonNode tags) {
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

    /** Bias the free-text query toward the correct venue type for the sport, within South Africa. */
    private String buildQuery(String query, SportProfile profile) {
        String q = query == null ? "" : query.trim();
        String kw = profile.primaryKeyword();
        String lower = q.toLowerCase(Locale.ROOT);

        StringBuilder sb = new StringBuilder();
        if (!kw.isBlank() && !lower.contains(kw.toLowerCase(Locale.ROOT))) {
            sb.append(kw).append(' ');
        }
        sb.append(q);
        // Anchor to the country so free-text-only searches stay in South Africa.
        if (!lower.contains("south africa")) {
            sb.append(" South Africa");
        }
        return sb.toString().trim();
    }

    private String inferVenueType(JsonNode tags, String leisure) {
        String building = tags.path("building").asText("");
        String indoorTag = tags.path("indoor").asText("");
        String sport = tags.path("sport").asText("").toLowerCase(Locale.ROOT);
        boolean indoorSport = sport.contains("squash") || sport.contains("swimming")
                || sport.contains("gym") || sport.contains("fitness");
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
         * Overpass QL fragment (node/way/relation clauses) selecting sports facilities for this
         * sport. {@code {{A}}} is replaced with the area filter (around:radius,lat,lng or a bbox).
         */
        String overpassFilter() {
            StringBuilder sb = new StringBuilder();
            if (this == SWIMMING) {
                // Swimming pools are tagged leisure=swimming_pool / sport=swimming.
                for (String s : tags) {
                    sb.append("nwr[\"sport\"=\"").append(s).append("\"]({{A}});");
                }
                sb.append("nwr[\"leisure\"=\"swimming_pool\"]({{A}});");
                sb.append("nwr[\"leisure\"=\"sports_centre\"][\"sport\"~\"swim\"]({{A}});");
                return sb.toString();
            }
            if (this == GENERIC) {
                // Any recognised sports facility.
                sb.append("nwr[\"leisure\"=\"pitch\"][\"sport\"]({{A}});");
                sb.append("nwr[\"leisure\"=\"sports_centre\"]({{A}});");
                sb.append("nwr[\"leisure\"=\"stadium\"]({{A}});");
                return sb.toString();
            }
            // Sport-specific: match the sport tag on any facility, plus sports centres for it.
            for (String s : tags) {
                sb.append("nwr[\"sport\"~\"").append(s).append("\"]({{A}});");
            }
            return sb.toString();
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
