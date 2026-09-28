# Create Listing Map - Full Bug Audit

*GameOn - CodeSphere*

## Scope Reviewed

create.html (form + all map/venue/weather JS), VenueSearchService, WeatherService (the real weather client - there is no WeatherApiClient/WeatherController; those are WeatherService + MapWeatherController), MapWeatherController, the GameOnMap helper in fragments/layout.html, the Nominatim/Overpass/OSRM integrations, the Open-Meteo integration, and the weather + playability UI.

## Overall Assessment

The recent area-bias refactor is solid - the userLoc / areaLoc / selectedVenueLoc separation is correct and the search priority works. No hard-coded venue lists remain. The remaining issues are mostly medium/low: race conditions from un-cancelled async requests, missing loading/error states, distance-filter gaps, accessibility, and a few null/exception risks on the backend. Nothing is data-corrupting, but several bugs cause confusing UX and stale displays.

---

## Critical

None found. Listing creation, weather coordinate handoff, and area authority are functionally correct.

## High

### H1 - Race condition: out-of-order async responses can show stale venues/area
- **File:** create.html - setArea(), runSearch(), refreshRecommendations()
- **Root cause:** No request sequencing/cancellation. Setting "Cape Town" then quickly "Durban" (or rapid sport changes) lets an earlier, slower response resolve after the later one and overwrite areaLoc/markers/list with stale data. setArea also sets areaLoc only inside .then, so two in-flight geocodes race.
- **Recommended fix:** Add a monotonically increasing request token captured per call; in each .then, bail if the token is no longer current. Apply to geocode, venue search, and recommendations.
- **Affects:** Map display, Venue selection, Performance

### H2 - Sole-provider dependency on Overpass with no partial fallback / failure signal
- **File:** VenueSearchService - discoverVenues(...)
- **Root cause:** A 429 or timeout on the shared Overpass endpoint makes the whole area return empty, because the Nominatim fallback rarely yields sports venues. There is no distinction between "Overpass failed" and "area genuinely empty", and no user-visible signal that discovery degraded.
- **Recommended fix:** Distinguish API failure from empty results; surface distinct client messages; add a secondary Overpass mirror (e.g. overpass.kumi.systems) as failover before the Nominatim fallback.
- **Affects:** Venue selection, Performance, UX

### H3 - No throttle / in-flight guard on Set Area / Search; rate-limit risk
- **File:** create.html (areaBtn, searchBtn) and VenueSearchService
- **Root cause:** Rapid repeated clicks each fire geocode plus a venue search that may internally issue up to 4 paced Nominatim variants or 2 Overpass calls. On shared public endpoints this trips 429 Too Many Requests, which then makes legitimate searches return empty.
- **Recommended fix:** Disable Set-area/Search buttons while a request is in flight (with a spinner); short-circuit repeat queries client-side; add per-key in-flight de-dup on the backend venue search (WeatherService already does this for weather).
- **Affects:** Performance, Venue selection, UX

---

## Medium

### M1 - Venue text search has no loading indicator and no in-flight guard
- **File:** create.html - runSearch() / renderDropdown()
- **Root cause:** No spinner between debounce firing and response; a slow response can replace a newer query dropdown (same race as H1, lower impact).
- **Recommended fix:** Show a spinner in the dropdown and apply the H1 sequence token.
- **Affects:** Venue selection, UX

### M2 - loadWeather() has no request token; rapid date/venue changes can render an older forecast
- **File:** create.html - loadWeather(), loadAlternatives()
- **Root cause:** Out-of-order responses: changing the date twice quickly can let the first /api/weather response land last and display the wrong forecast.
- **Recommended fix:** Sequence token guarding renderWeather / renderPlayability.
- **Affects:** Weather

### M3 - No user-facing error when APIs fail; failures are swallowed
- **File:** create.html - the empty .catch(function(){}) blocks (venue search, reverse-geocode, route, weather, alternatives)
- **Root cause:** Empty catch blocks hide failures; on an outage the UI shows nothing, reading as "no venues/weather" rather than "temporary error".
- **Recommended fix:** Distinguish error vs. empty and show a retry message.
- **Affects:** Weather, Venue selection, UX

### M4 - Weather horizon (~7-8 days) silently returns "unavailable" for far-future dates
- **File:** WeatherService.fetchFromApi (forecast_days=8) and nearest() (12h window)
- **Root cause:** A listing scheduled more than 8 days out yields an unavailable forecast; the UI shows "Forecast unavailable" with no explanation that it is a range limit, not an error.
- **Recommended fix:** Detect targets beyond the horizon and show "Forecast available closer to the date (within ~7 days)".
- **Affects:** Weather, UX

### M5 - Selecting a recommended venue leaves its reco pin on the map (overlapping markers)
- **File:** create.html - selectVenue/applyVenue adds a marker via placeMarker but does not clear recoMarkers
- **Root cause:** No coordination between the selected marker and the recommendation pins, so a green reco circle and the new pin overlap at the same coordinate.
- **Recommended fix:** On selection, clear or de-emphasise reco markers; avoid a duplicate marker at the identical point.
- **Affects:** Map display

### M6 - applyVenue truncates location to 200 chars by substring (can cut mid-word)
- **File:** create.html - applyVenue: location = addr.substring(0,200)
- **Root cause:** Hard substring to satisfy the DB maxlength=200 can persist an ugly truncated address.
- **Recommended fix:** Truncate at the last comma/space before 200, or store venueName + suburb + city.
- **Affects:** Listing creation (data quality)

### M7 - Reverse-geocode enrichment gap for named venues with weak addresses
- **File:** create.html selectVenue (genericAddr || genericName) and VenueSearchService.buildOsmAddress
- **Root cause:** A named venue whose address is a vague suburb-only string is not enriched, leaving a weak address in location.
- **Recommended fix:** Also enrich when the address lacks a street/suburb token.
- **Affects:** Listing creation (data quality)

### M8 - /api/listings/map calls weather + playability per listing (N+1 external calls)
- **File:** MapWeatherController.listingsForMap
- **Root cause:** For up to 200 listings it calls getForecast and evaluate each; distinct venues each trigger an Open-Meteo fetch - a burst of external calls on one page load. (Listings map view; shares the services.)
- **Recommended fix:** Lazy-load popup weather on marker click, or cap/async-enrich.
- **Affects:** Performance

### M9 - POOR playability auto-fires alternative-date lookup on every weather load
- **File:** create.html loadWeather -> loadAlternatives; MapWeatherController.alternatives
- **Root cause:** Each POOR verdict triggers /api/weather/alternatives (7-day evaluation). Combined with M2, rapid date changes stack alternative lookups.
- **Recommended fix:** Sequence-guard and debounce; fetch alternatives only after the primary forecast settles.
- **Affects:** Weather, Performance

### M10 - Map hidden-container sizing relies on a fixed 300ms timeout
- **File:** fragments/layout.html createMap - setTimeout(invalidateSize, 300)
- **Root cause:** On slower devices 300ms may fire before layout completes, leaving grey tiles until a resize (low risk here as the column is always visible).
- **Recommended fix:** Also call invalidateSize on window.resize and after the first tile load event.
- **Affects:** Map display (mobile especially)

---

## Low

### L1 - Venue dropdown and reco list are not keyboard-navigable / lack ARIA roles
- **File:** create.html - renderDropdown, buildRecoRow
- **Recommended fix:** Add ARIA roles, arrow-key handling, and focus management.
- **Affects:** UX (accessibility)

### L2 - Mobile: absolutely-positioned venue dropdown can overlap the map card
- **File:** create.html layout + .venue-search-dropdown CSS
- **Recommended fix:** Confirm dropdown z-index above .gameon-map and adjust for small viewports.
- **Affects:** UX (mobile)

### L3 - escapeAttr equals escapeHtml; link href schemes are not validated
- **File:** create.html - buildRecoRow href from v.bookingUrl / v.website / v.phone
- **Root cause:** A malicious/malformed OSM website tag could contain a javascript: scheme; HTML-escaping does not neutralise a javascript: href on click.
- **Recommended fix:** Whitelist http/https (and tel: for phone) schemes before rendering the link.
- **Affects:** Security (low; OSM data on an authenticated page)

### L4 - Duplicate-venue de-dup rounded to ~1km can merge distinct venues
- **File:** VenueSearchService overpassSearch/fetch (round2 key)
- **Recommended fix:** De-dup by name + rounded coord and use finer rounding (3dp ~110m).
- **Affects:** Venue selection (missing/duplicate venues)

### L5 - Sport is sent as display text; unknown sports silently degrade to GENERIC
- **File:** create.html sportName() and VenueSearchService.SportProfile.forSport
- **Recommended fix:** Pass a stable sport id/key; log when a sport falls through to GENERIC.
- **Affects:** Venue selection

### L6 - Malformed numeric params: NaN guarded client-side; backend 400 hidden
- **File:** create.html restore block parseFloat; backend @RequestParam Double
- **Recommended fix:** Validate/no-op on NaN; surface backend errors (see M3).
- **Affects:** Weather, Listing creation (minor)

### L7 - Verbose INFO logging on every venue search
- **File:** VenueSearchService.search / discoverVenues
- **Recommended fix:** Demote routine lines to DEBUG; keep a single INFO summary.
- **Affects:** Performance (log volume), operability

### L8 - OSRM route call lacks distinct timeout/rate-limit handling; can hang silently
- **File:** create.html showRoute; VenueSearchService.route
- **Recommended fix:** Add a timeout and a "route estimate unavailable" message.
- **Affects:** UX (directions)

### L9 - Bounded geocode may fail for small/ambiguous suburbs with no fallback
- **File:** VenueSearchService.geocodeSouthAfrica
- **Recommended fix:** On empty bounded result, retry once without bounded (still countrycodes=za) before returning not-found.
- **Affects:** Venue selection, Map display

---

## Cross-cutting Observations

- **Shared public API rate limits (Overpass/Nominatim/OSRM):** the biggest practical reliability risk; H2, H3, M8, M9 converge here. In testing, 429s were the main cause of "zero venues", not logic errors. Production should use dedicated/self-hosted Overpass + Nominatim, or at least a real contact User-Agent and strict client-side throttling.
- **No automated tests** cover this workflow; all verification is manual.
- **Weather correctness confirmed:** applyVenue writes venue lat/lng to the hidden inputs and loadWeather reads those - weather uses venue coordinates, not area coordinates.
- **Area authority confirmed:** searchBias() priority (area > venue > user) and setArea clearing selectedVenueLoc correctly prevent the old Gqeberha-bias bug.

---

## Ranked Fix Order (highest risk first)

1. **H3** - throttle/disable in-flight buttons + backend in-flight de-dup. Biggest real-world failure cause (rate-limit to empty results). Cheap, high impact.
2. **H1** - request-sequence tokens for geocode/venue/reco. Eliminates stale-area/stale-venue displays on rapid interaction.
3. **H2** - distinguish Overpass failure vs. empty; add mirror failover + honest messaging.
4. **M3** - surface API errors (stop swallowing catches). Prerequisite for diagnosing everything else; small change.
5. **M2 / M9** - sequence-guard weather + alternatives. Prevents wrong-date forecasts.
6. **M5** - resolve duplicate/overlapping markers on venue select.
7. **M4** - forecast-horizon messaging.
8. **M8** - lazy-load listings-map weather.
9. **L4 / L9** - de-dup granularity + geocode fallback.
10. **L1 / L2** - accessibility + mobile dropdown.
11. **L3 / L5 / L6 / L7 / L8** - link-scheme whitelist, sport-key decoupling, NaN guards, log levels, route timeout.

**Recommendation:** Fix H1-H3 and M2/M3 before adding any new venue/map features - they address stale state, silent failures, and rate-limit fragility that would otherwise undermine anything built on top. The remaining medium/low items can be batched afterward.
