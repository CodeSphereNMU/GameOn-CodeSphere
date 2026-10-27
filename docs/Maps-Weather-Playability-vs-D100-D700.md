# Game On — How Maps, Weather & Playability Integrate with D100–D700

> Everything below was verified against the actual source in `gameon/src/main/java/com/gameon`, the Thymeleaf templates in `gameon/src/main/resources/templates`, the Flyway migration `V15__Add_Listing_Venue_Details.sql`, and cross-checked against `ARCHITECTURE.md` and the Final Panel Review slides. Where a connection does **not** exist, that is stated plainly rather than invented.

## The one-sentence truth you must lead with

**Maps, venue search, weather and playability are a self-contained "venue intelligence" feature that lives entirely inside the Game On *game-listing* workflow. None of D100–D700 (register, profile, add sport, follow, notifications, report user, report post) use maps or weather at all.** The evidence: a repository-wide search for `Weather|Venue|Playability|latitude|longitude|geocode` matches only two controllers — `MapWeatherController` and `ListingsController`. Every account/profile/social/moderation controller is clean.

So the honest panel answer is: *"Maps and weather integrate with game listings — specifically the create-listing wizard and the listings map view. The D-series use cases are deliberately independent of them."*

---

# A. The map and weather architecture (actual implementation)

## A.1 Big picture — keyless, server-proxied, fail-soft

The browser **never** calls OpenStreetMap or Open-Meteo directly. A single REST controller, `MapWeatherController` (`@RequestMapping("/api")`), proxies all external APIs server-side. `ARCHITECTURE.md` §1.11 states the reason: so results can be cached, so rate-limits are handled in one place, and so the Content-Security-Policy can stay strict (the browser only talks to our own `/api/**`). All external APIs used are **keyless public APIs**, called through **one shared `RestTemplate`** that fails softly.

```
Browser (Leaflet + inline JS)
        │  fetch('/api/...')
        ▼
MapWeatherController  (/api/**)   ← the only HTTP surface for this feature
        │
        ├── VenueSearchService  → Nominatim (geocode/reverse), Overpass (venue discovery), OSRM (routes)
        ├── WeatherService      → Open-Meteo (hourly forecast)
        └── PlayabilityService  → pure in-memory rules (NO external call)
        │
        └── GameListingService / GameJoinerService  (for the map-view listing data)
```

## A.2 `HttpClientConfig.java` — the shared HTTP client

- **File:** `config/HttpClientConfig.java`
- Defines one `@Bean externalApiRestTemplate(RestTemplateBuilder)` with **5-second connect timeout** and **20-second read timeout** (the longer read timeout accommodates Overpass, which is slow).
- Both `WeatherService` and `VenueSearchService` constructor-inject this same bean. Consequence: if a third party is slow, it degrades gracefully — weather comes back "unavailable", venue search returns an empty/`UNAVAILABLE` status — instead of hanging the request thread forever.

## A.3 `VenueSearchService.java` — venues, geocoding, routing

**File:** `service/VenueSearchService.java`. Everything is restricted to a **South Africa bounding box** (`ZA_WEST 16.0, ZA_SOUTH -35.5, ZA_EAST 33.5, ZA_NORTH -22.0`) and stray out-of-country results are rejected by `inSouthAfrica()`.

| Method | External API | Endpoint in code | Purpose |
|---|---|---|---|
| `search(...)` / `searchDetailed(...)` | **Overpass** + **Nominatim** | `https://overpass-api.de/api/interpreter`, `https://nominatim.openstreetmap.org/search` | Discover SA sports venues near a point, ranked by sport-match then distance |
| `geocodeSouthAfrica(query)` | **Nominatim** | `https://nominatim.openstreetmap.org/search` (`format=jsonv2, countrycodes=za, viewbox=<ZA bbox>, bounded=1, limit=1`) | Turn a typed place ("Gqeberha") into a lat/lng |
| `reverseGeocode(lat, lng, name)` | **Nominatim** | `https://nominatim.openstreetmap.org/reverse` (`format=jsonv2, addressdetails=1, zoom=18`) | Turn a coordinate into a readable name + street address |
| `route(fromLat, fromLng, toLat, toLng)` | **OSRM** | `https://router.project-osrm.org/route/v1/driving/{lng,lat;lng,lat}?overview=false` | Driving distance (km) + duration (min) → `RouteSummary` |

**Roles of each OSM service, in plain language:**

- **OpenStreetMap (OSM):** the free, crowd-sourced world map. It supplies both the *map tiles* (the images you see) and the underlying *data* the other services query.
- **Nominatim:** OSM's **search/geocoding** engine. "Place name ↔ coordinate." Used for the *area* box ("Gqeberha" → coordinates) and for *reverse geocoding* a picked pin back to a real address. Code comment notes Nominatim's policy requires a descriptive `User-Agent` (`GameOn/1.0 … contact: support@gameon.co.za`), which the service sets, or requests get throttled.
- **Overpass:** OSM's **feature/POI query** engine. It finds things *by tag* (`leisure=pitch`, `sport=tennis`, `sports_centre`…) within a radius of a coordinate. The code comment explains *why* it's used: Nominatim free-text search is poor at discovering sports facilities, so Overpass is "the right tool." Default discovery radius is `DEFAULT_RADIUS_M = 20 km`, expanding to `EXPANDED_RADIUS_M = 45 km` if nothing is found.
- **OSRM (project-osrm.org):** a routing engine, used only by `route()` to compute driving distance and travel time for the "Get Directions" feature.

**Reliability engineering worth naming to the panel:**

- 15-minute in-memory cache (`ConcurrentHashMap`, `CACHE_TTL`) keyed by query+sport+rounded-bias+area.
- Per-key in-flight locks so concurrent identical searches share one external lookup.
- `classifyProviderError(...)` maps exceptions to a `VenueSearchOutcome.Status`: HTTP 429 → `RATE_LIMITED`, socket timeout → `TIMEOUT`, 5xx/transport → `UNAVAILABLE`. This lets the UI say *"provider busy, try again"* instead of lying that there are *"no venues."*

## A.4 `WeatherService.java` — the forecast

**File:** `service/WeatherService.java`. External API: **Open-Meteo**, `https://api.open-meteo.com/v1/forecast`.

- `fetchFromApi(lat, lng)` requests `hourly = temperature_2m, relative_humidity_2m, precipitation_probability, wind_speed_10m, weather_code`, with `wind_speed_unit=kmh`, `timezone=auto`, `forecast_days=8`.
- It parses the parallel hourly arrays into a `List<WeatherForecast>` (fields: `temperatureC`, `humidityPercent`, `rainChancePercent`, `windSpeedKmh`, `weatherCode`, `condition`, `icon`, `available`).
- `conditionFor(code)` / `iconFor(code)` translate WMO weather codes into human text ("Partly Cloudy") and a Bootstrap icon.
- `getForecast(lat, lng, matchTime)` returns the hourly sample **nearest** the match time; `nearest()` rejects any sample more than **12 hours** from the target as `WeatherForecast.unavailable()` (so weather for a game 3 weeks out — beyond the 8-day horizon — correctly reads "unavailable").
- Caching: 30-minute TTL, keyed by lat/lng rounded to ~2 decimals (~1 km grid) so nearby venues share a cache entry; per-key locks de-dupe concurrent fetches.

## A.5 `PlayabilityService.java` — combining weather + venue into a verdict

**File:** `service/PlayabilityService.java`. This is a **pure in-memory rules engine — no external calls.** This is exactly where weather and venue type combine.

`evaluate(sportName, VenueType venueType, WeatherForecast forecast)` returns a `PlayabilityResult` (`Rating` = `GOOD` / `FAIR` / `POOR` / `UNKNOWN`, plus a message):

1. **Indoor short-circuit:** `isIndoor()` is true if `venueType == INDOOR` **or** the sport is in `ALWAYS_INDOOR_SPORTS` (`SQUASH, INDOOR PADEL, INDOOR TENNIS, INDOOR BASKETBALL`). → `GOOD`, "Weather conditions do not impact playability."
2. **No forecast:** forecast null or unavailable → `UNKNOWN`, "Check conditions closer to match time."
3. **Outdoor grading** (thresholds are named constants):
   - **POOR** if rain > 50%, wind > 35 km/h, temp > 38°C, or temp < 5°C.
   - **FAIR** if rain ≥ 30%, wind ≥ 25 km/h, temp ≥ 33°C, or temp ≤ 8°C.
   - Otherwise **GOOD**.

Note what drives it: the forecast's `rainChancePercent` / `windSpeedKmh` / `temperatureC`, and the venue's `INDOOR`/`OUTDOOR` flag. Sport only matters via the "always indoor" list.

## A.6 `MapWeatherController.java` — the endpoints (the whole HTTP surface)

**File:** `controller/MapWeatherController.java`, `@RestController @RequestMapping("/api")`. All endpoints require authentication (global rule in `SecurityConfig`).

| Method + path | Params | Returns |
|---|---|---|
| `GET /api/venues/search` | `q?`, `sport?`, `area?`, `lat?`, `lng?` | `{status, venues[]}`; status→HTTP: OK/EMPTY=200, RATE_LIMITED=429, TIMEOUT/UNAVAILABLE=503 |
| `GET /api/geocode` | `q` | `{found, name, address, lat, lng}` |
| `GET /api/reverse-geocode` | `lat, lng, name?` | `{found, name, address, lat, lng}` |
| `GET /api/route` | `fromLat, fromLng, toLat, toLng` | `{available, distanceKm, durationMinutes}` |
| `GET /api/weather` | `lat, lng, time`(ISO), `sport?`, `venueType?` | `{weather, playability}` |
| `GET /api/playability` | `lat, lng, time, sport?, venueType?` | `PlayabilityResult` only |
| `GET /api/weather/alternatives` | `lat, lng, time, sport?, venueType?` | up to 3 future days (next 7) rated GOOD/FAIR |
| `GET /api/listings/map` | (authenticated user; no params) | all coordinate-bearing public listings + per-listing weather + playability |

## A.7 Frontend: Leaflet + OpenStreetMap tiles (not Google Maps)

- **Shared helper `window.GameOnMap`** is defined inline in `templates/fragments/layout.html`, which loads **Leaflet 1.9.4** (CSS + JS from unpkg). Tiles come from OSM directly: `https://tile.openstreetmap.org/{z}/{x}/{y}.png`. Default map centre is Gqeberha/Port Elizabeth `[-33.9608, 25.6022]`. There are **no `.js` files under `static/`** — all map JS is inline in templates.
- **Creation page** `templates/listings/create.html` is the heart of the feature (map on the right, form on the left).
- **List/map view** `templates/listings/index.html` has a List View / Map View toggle (`#listViewBtn` / `#mapViewBtn`, container `#listingsMap`).
- **Edit page** `templates/listings/edit.html` has only a plain free-text location field — no map intelligence.

## A.8 Complete data flow (the 7 steps you asked for)

1. **User selects/searches a location.** In `create.html`: type a city/suburb in the *area* box → JS calls `GET /api/geocode?q=…` → `VenueSearchService.geocodeSouthAfrica` (Nominatim) → map recentres; OR type a venue name → debounced `GET /api/venues/search?q=…&sport=…&area=…&lat=…&lng=…`.
2. **Obtaining venue + coordinates.** `/api/venues/search` → `VenueSearchService.searchDetailed` → Overpass (area discovery) or Nominatim (named search) → list of `VenueResult`s with `name, address, latitude, longitude, venueType, distanceKm, matchesSport`. If a picked pin has a generic name/address, JS calls `GET /api/reverse-geocode` to fill a real address.
3. **Coordinates → weather.** On venue pick (`applyVenue`) and on date change, JS calls `GET /api/weather?lat=…&lng=…&time=…&sport=…&venueType=…` → `WeatherService.getForecast` (Open-Meteo).
4. **Weather + venue → playability.** The same `/api/weather` call also runs `PlayabilityService.evaluate(sport, venueType, forecast)` server-side and returns `{weather, playability}` together.
5. **Display.** `renderWeather()` fills the weather card (temp, condition, humidity, rain %, wind, time); `renderPlayability()` colours the playability card GOOD/FAIR/POOR. If `POOR`, JS calls `GET /api/weather/alternatives` to suggest better dates in the next 7 days ("Use This Date" button).
6. **Store + retrieve.** The chosen venue is written into **hidden form fields** `venueName`, `latitude`, `longitude`, `venueType` (plus visible `location`). These post through the create wizard and are persisted to `game_listings`. Retrieval: `GET /api/listings/map` reads them back (`l.getLatitude()`, etc.) to draw markers.
7. **Directions.** The "Get Directions" button (`#getDirectionsBtn`) calls `startDirections()`: it asks the browser for geolocation, calls `GET /api/route` (OSRM) to show driving distance + ETA, and hands off to the device's maps app via an external maps link (on mobile, immediately for turn-by-turn).

---

# B. Connecting maps & weather to each use case (D100–D700)

For each: **classification · evidence · code · user view · where it's documented.**

## D100 — Register New User

- **Classification: Not implemented / independent.**
- **Evidence:** `AuthController.processRegister` / `processRegisterSports` → `AuthService.registerStep1/Step2`. Step 1 collects **username + password only**; Step 2 collects **at least one sport + skill level** (creates `UserSportProfile` rows). No location field, no lat/lng, no call to any `/api/*` map endpoint. The grep for location/weather terms never touches `AuthController` or `AuthService`.
- **User view:** Two-step wizard; after step 2 the user is auto-logged-in and dropped on `/listings`. Location only becomes relevant *later*, when they create or browse a listing.
- **Selected sports relationship:** The sports chosen at registration *do* flow into listings indirectly — browse/create are gated on sports-on-profile, and `sport` is later passed to `/api/venues/search` and `/api/weather` as a parameter. But that is a listing-time connection, **not** a registration-time one.
- **Documented in:** `ARCHITECTURE.md` D100 (confirms two-step, sports, auto-login — no location). Slides: generic ("how does this screen connect to the database?" applies — it writes `users` + `user_sport_profiles`, nothing geo).

## D200 — Manage User Profile

- **Classification: Not implemented / independent.**
- **Evidence:** `ProfileController` + `ProfileService.updateProfilePicture` + `UserService.updateUsername` + `AvatarStorageService`. The profile stores **username and an avatar URL** only. The `User` entity has no latitude/longitude/location column. No profile code path calls a map/weather endpoint.
- **User view:** Edit username, upload a profile picture. Nothing geographic is stored or shown.
- **Not connected:** There is **no "home location" on the profile**, so maps cannot and do not personalise anything from the profile. (The listings *browse* page does a best-effort *browser* geolocation for a "km away" label, but that comes from the device at view time, not from stored profile data.)
- **Documented in:** `ARCHITECTURE.md` D200.

## D300 — Add New Sport

- **Classification: Indirect (parameter only), map/weather otherwise independent.**
- **Evidence:** `ProfileService.addSportToProfile(userId, sportId, skillLevel)` just inserts a `UserSportProfile` row (rejects duplicates; blocks removing your last sport). It never touches venue/weather code. **However**, the sport you own later becomes the `sport` query parameter on `/api/venues/search` (drives the Overpass tag filter / sport-match ranking via `SportProfile.forSport`) and on `/api/weather` (fed to `PlayabilityService` for the `ALWAYS_INDOOR_SPORTS` check).
- **Important honesty point:** There are **no sport-specific weather rules** beyond the fixed indoor-sport list. The POOR/FAIR temperature/rain/wind thresholds are the same for every outdoor sport. So "adding a sport" does **not** change weather thresholds or create sport-specific playability rules.
- **User view:** Pick a sport + skill; it appears on your profile and unlocks that sport in create/browse. Only *then* does the sport name influence which venues/playability show.
- **Documented in:** `ARCHITECTURE.md` D300. The sport→venue coupling is implicit in the create flow docs, not in D300 itself.

## D400 — View User Profile and Follow/Unfollow

- **Classification: Not implemented / independent.**
- **Evidence:** `ProfileController.viewProfile` → `BlockService.isBlockedEitherWay`, `FollowService.isFollowing`. Shows sports, follower counts, follow state, friends leaderboard. No map/weather calls. Following writes a `follows` row and may send a `FOLLOW_NEW` notification — no location data.
- **Following vs game discovery:** Following **does** affect listings elsewhere, but not via maps: in the create wizard the friends step offers `followService.getFriendIds` to invite, and private listings use follow/friend relationships. None of that involves coordinates or weather.
- **User view:** You see someone's profile and press Follow/Unfollow. You never see their location or any map here.
- **Future-improvement vs actual:** "Discover games near people I follow on a map" would be a *potential* improvement; it is **not implemented**.
- **Documented in:** `ARCHITECTURE.md` D400.

## D500 — View Notifications

- **Classification: Not implemented (no weather/map content in notifications).**
- **Evidence:** `NotificationService.getNotificationsForUser` + `resolveDestination` (a `switch` on `NotificationType`). Notifications carry a *type* and a *linked entity id* (listing/request/post/result), and resolve to an **internal route** (e.g. `JOIN_REQUEST_RECEIVED → /lobby/requests/{id}`). There is **no weather snippet, no map link, no playability text, no coordinates** in a notification payload.
- **Subtle correct nuance:** A notification can *link to a listing* that itself has coordinates; opening it lands you on the listing page, from where the map/weather features live. But the notification itself contains none of that — the connection is at most "a hop away," not embedded.
- **User view:** A list of text notifications, newest first; clicking an eligible one marks it read and navigates to the relevant page.
- **Documented in:** `ARCHITECTURE.md` D500.

## D600 — Report User

- **Classification: Not implemented / independent.**
- **Evidence:** `ReportService.reportAndBlockUser(...)` creates a `USER` report **and** a block in one transaction. A `Report` references reporter + reported user (+ reason); it does **not** store location, coordinates, or a game listing.
- **Moderation/safety connection:** Purely social/moderation — no geographic or weather dimension.
- **User view:** You report a user with a reason; they're also blocked immediately.
- **Documented in:** `ARCHITECTURE.md` D600/D700.

## D700 — Report Post

- **Classification: Not implemented / independent.**
- **Evidence:** `ReportService.createReport(...)` stores a `POST` report against an active post. Posts are **social-feed content** (text + optional images), not game listings — a post has no venue/coordinate/weather fields. So reporting a post cannot carry map or playability data.
- **Reported content containing a listing/location?** No. The report targets a `Post` entity; there is no path that attaches a game location or map marker to a report.
- **User view:** You report a post with a reason; it enters the moderator queue.
- **Documented in:** `ARCHITECTURE.md` D700 (and `ModeratorController` B400 handles the queue).

**Summary for Part B:** Every D-series use case is **independent of maps and weather.** D300 is the only one with even an *indirect* link, and only because the sport you add later becomes a query parameter to the listing-side venue/playability features — not because adding a sport does anything geographic itself.

---

# C. Game listings ↔ maps ↔ weather (the realistic end-to-end example)

*Scenario: Thandi creates a 5-a-side football listing in Gqeberha, picks a venue, saves it; Sipho later finds it and joins.*

| Stage | Frontend component | Endpoint | Controller | Service | DB interaction |
|---|---|---|---|---|---|
| 1. Start create wizard | `listings/create.html` (`#createMap` Leaflet map) | `GET /listings/create` | `ListingsController.showCreate` | `GameListingService` (form data) | reads user's sports/formats |
| 2a. Set area | area box | `GET /api/geocode?q=Gqeberha` | `MapWeatherController.geocode` | `VenueSearchService.geocodeSouthAfrica` → Nominatim | none (external) |
| 2b. Find venue | venue search dropdown | `GET /api/venues/search?q=…&sport=Football&area=Gqeberha&lat=…&lng=…` | `MapWeatherController.searchVenues` | `VenueSearchService.searchDetailed` → Overpass/Nominatim | none (external, cached) |
| 2c. Resolve address | pin select (`applyVenue`) | `GET /api/reverse-geocode?lat=…&lng=…` | `MapWeatherController.reverseGeocode` | `VenueSearchService.reverseGeocode` → Nominatim | none |
| 3. Store selection | hidden fields `venueName/latitude/longitude/venueType` + visible `location` | `POST /listings/create` → … → `POST /listings/confirm` | `ListingsController.processCreate` → `confirmCreate` | `GameListingService.createListing(..., venueName, latitude, longitude, venueType)` | **INSERT into `game_listings`** (cols `location, venue_name, latitude DECIMAL(9,6), longitude DECIMAL(9,6), venue_type`) — also inserts creator as `GameJoiner` Team A |
| 4a. List view | `listings/index.html` List View | `GET /listings` | `ListingsController.index` | `GameListingService.browseAvailableListings` | SELECT public/open/future listings |
| 4b. Map view | `#listingsMap` Leaflet | `GET /api/listings/map` | `MapWeatherController.listingsForMap` | `GameListingService.browseAvailableListings` + `GameJoinerService.countCurrentParticipants` | SELECT listings w/ coords; count participants |
| 5. Weather for coords | per-card / popup | `GET /api/weather?lat=…&lng=…&time=…` | `MapWeatherController.weather` | `WeatherService.getForecast` → Open-Meteo | none (cached) |
| 6. Playability | same `/api/weather` response | — | same | `PlayabilityService.evaluate(sport, venueType, forecast)` | none (in-memory rules) |
| 7. View + directions | `listings/{id}` + "Get Directions" | `GET /api/route?fromLat…&toLat…` | `MapWeatherController.route` | `VenueSearchService.route` → OSRM | none |
| 8. Join | join button | `POST /game-joiner/join/{id}` | `GameJoinerController.submitJoinRequest` | `GameJoinerService.sendJoinRequest` | INSERT `JoinRequest` (PENDING) + notify creator |

**Key persistence facts (verified):**

- The entity `GameListing` has `location` (NOT NULL free text), plus nullable `venueName`, `latitude`/`longitude` (`BigDecimal`, precision 9 scale 6 ≈ 0.11 m), and `venueType` (enum `INDOOR`/`OUTDOOR`). `@Transient hasCoordinates()` gates whether a listing shows on the map.
- Flyway `V15__Add_Listing_Venue_Details.sql` added these four columns as **nullable** (so legacy listings stay valid under `ddl-auto=validate`) with CHECK constraints (`venue_type IN ('INDOOR','OUTDOOR')`, lat −90..90, lng −180..180).
- Step 8 (join) has **nothing to do with maps/weather** — it's the standard A300 join-request flow. The map simply helped Sipho *discover* the game; joining itself is geo-agnostic.

---

# D. Use-case integration matrix

| Use case | Map connection | Weather connection | Playability connection | Evidence in code | Explanation |
|---|---|---|---|---|---|
| **D100 Register User** | Not implemented | Not implemented | Not implemented | `AuthController`, `AuthService.registerStep1/2` — no geo | Collects username/password + sports only; auto-login to `/listings`. |
| **D200 Manage Profile** | Not implemented | Not implemented | Not implemented | `ProfileController`, `UserService.updateUsername`, `AvatarStorageService` | Username + avatar URL only; `User` has no location column. |
| **D300 Add Sport** | Indirect | Indirect | Indirect | `ProfileService.addSportToProfile`; sport later a param to `/api/venues/search` & `/api/weather` | Adding a sport does nothing geo itself; the sport *name* later filters venues and feeds `ALWAYS_INDOOR_SPORTS`. No sport-specific weather thresholds exist. |
| **D400 View Profile / Follow** | Not implemented | Not implemented | Not implemented | `ProfileController.viewProfile`, `FollowService`, `BlockService` | Shows social data; follow affects invites/privacy, never coordinates. |
| **D500 View Notifications** | Not implemented | Not implemented | Not implemented | `NotificationService.resolveDestination` (type→internal route) | Notifications carry type + entity id → internal URL; no weather/map/coords embedded. |
| **D600 Report User** | Not implemented | Not implemented | Not implemented | `ReportService.reportAndBlockUser` | Report + block in one transaction; no location fields. |
| **D700 Report Post** | Not implemented | Not implemented | Not implemented | `ReportService.createReport` | Targets a social `Post` (text/images), which has no venue/weather data. |
| **A100 Create Game Listing** *(listing UC)* | **Direct** | **Direct** | **Direct** | `listings/create.html` + `MapWeatherController` + all three services + `GameListingService.createListing` | The feature's home: geocode area, Overpass/Nominatim venue search, reverse-geocode, store lat/lng/venueType, live weather + playability + alternative dates + directions. |
| **A200 Browse Listings (List + Map View)** *(listing UC)* | **Direct** | **Direct** | **Direct** | `listings/index.html` (`#listingsMap`), `GET /api/listings/map`, per-card `GET /api/weather` | Map View plots coordinate-bearing listings; popups/cards show forecast + playability badge. |
| **C300 Manage/Edit Listing** *(listing UC)* | Not implemented | Not implemented | Not implemented | `listings/edit.html` — plain free-text location only | The edit page deliberately omits the map widget; venue intelligence is capture-at-create only. |

Labels used: **Direct** = the use case's own UI/controller calls the map/weather code. **Indirect** = no direct call, but a value it produces is later consumed by the feature. **Not implemented** = no connection in code.

---

# E. Significance for the project

## Why these features add value

Game On's core job is helping people find and join pickup sports games. A listing's most decision-critical attributes for a stranger are **where** and **whether it's worth going**. Maps answer "where" precisely (a real pin + address instead of a vague text string); weather + playability answer "is it worth going" for outdoor sports. They complement the core use cases by making **A100 (create)** richer and **A200 (browse)** more actionable — without altering the account/social/moderation flows (D100–D700), which have no geographic dimension.

## Realistic example (what's real vs assumed)

A student new to Gqeberha opens the **Map View** on `/listings`. **Actual behaviour:** coordinate-bearing public games appear as coloured pins (green = open, red = full via `GameOnMap.statusColor`); each popup shows the venue, date, spots left, a live forecast chip, and a playability badge (all from `GET /api/listings/map` + Open-Meteo + `PlayabilityService`). She opens a listing, taps **Get Directions** — **actual:** `/api/route` (OSRM) shows driving distance + ETA and hands off to her phone's maps app. **Assumption to avoid:** the app does *not* store her home address or auto-personalise by profile location; the "km away" figure comes from a best-effort *browser* geolocation prompt at view time, not stored data.

## User-experience improvements

- One pin + resolved address removes ambiguity about where to show up.
- A POOR verdict triggers `/api/weather/alternatives`, proactively suggesting better dates — a genuinely helpful nudge.
- Indoor sports/venues short-circuit to GOOD, so players aren't shown irrelevant rain warnings.

## Technical challenges of integrating external APIs

- **Rate limits / etiquette:** Nominatim requires a descriptive User-Agent (set in `VenueSearchService`) or it throttles. Caching (15 min venues, 30 min weather) + per-key in-flight locks reduce load.
- **Right tool per job:** Overpass for POI discovery, Nominatim for geocoding, OSRM for routing, Open-Meteo for forecast — four different APIs, each with its own response shape to parse.
- **Correctness bounds:** the SA bounding box rejects stray foreign results; the 12-hour nearest-sample rule and 8-day horizon prevent fabricating a forecast that doesn't exist.

## Missing / delayed API responses and the interface

- All calls go through the shared 5s-connect/20s-read `RestTemplate` and **fail soft**: weather returns `unavailable()` (card shows "Forecast unavailable"), venue search returns a typed status (`RATE_LIMITED`→429, `TIMEOUT`/`UNAVAILABLE`→503) and the UI shows an honest retry message rather than "no venues." Playability with no forecast → `UNKNOWN`. The map tiles themselves log `tileerror` to the console if blocked.
- Client-side, each async flow uses a monotonic **token** (`weatherToken`, `searchToken`, `recoToken`, `altToken`) so a slow/stale response can never overwrite a newer one in the DOM.

## Coordinating changes to shared components

- `window.GameOnMap` (in `fragments/layout.html`) and `MapWeatherController` are shared by both create and browse pages, so a change to the JSON shape or helper signature affects both templates — they must change together.
- `game_listings` venue columns are owned by Flyway `V15`; schema changes must be a new migration (never hand-edited) because `ddl-auto=validate` will fail the app on any mismatch.

## Current limitations / gaps (state these honestly)

- **Hard-coded to South Africa** (bounding box + `countrycodes=za`); unusable elsewhere.
- **No stored user home location** → no true "near me" without a live browser geolocation prompt.
- **Edit page has no map** (`edit.html`) — you can't re-pick a venue after creation, only retype free text.
- **Playability is a simple fixed-threshold rule set** with one indoor-sport allowlist; no per-sport thresholds, no humidity/UV/air-quality factors.
- Depends on **public demo endpoints** (overpass-api.de, router.project-osrm.org) with no SLA.

---

# F. Ten likely lecturer questions + model answers

**Q1. Which of D100–D700 actually use maps or weather?**
None. A codebase search for weather/venue/latitude/geocode matches only `MapWeatherController` and `ListingsController`. D100 (register), D200/D400 (profile/follow), D300 (add sport), D500 (notifications), D600/D700 (reports) are all independent. The feature lives in the *game-listing* use cases (create A100, browse A200), not the D-series. *(Evidence: `ARCHITECTURE.md` §1.5 controller table; grep across `controller/`.)*

**Q2. So why does the sport I pick at registration or in D300 seem connected to venues?**
Because the sport *name* later becomes a query parameter — `/api/venues/search?sport=…` (drives Overpass tags and sport-match ranking via `SportProfile.forSport`) and `/api/weather?...&sport=…` (feeds `PlayabilityService`'s `ALWAYS_INDOOR_SPORTS`). But D300 itself only inserts a `UserSportProfile` row (`ProfileService.addSportToProfile`). The connection is indirect and happens at listing time, not when you add the sport.

**Q3. How does a location get from the screen into the database?**
In `create.html`, picking a venue calls `applyVenue(...)` which writes hidden inputs `venueName`, `latitude`, `longitude`, `venueType` (+ visible `location`). These POST to `ListingsController.processCreate`, are stashed in a `CreateListingDraft` in the HTTP session, and on `confirmCreate` are passed to `GameListingService.createListing(..., venueName, latitude, longitude, venueType)`, which saves a `game_listings` row. Columns came from Flyway `V15` as nullable `DECIMAL(9,6)` lat/lng etc.

**Q4. How does the app get venue data, and why three different OSM APIs?**
`VenueSearchService` uses **Nominatim** to turn a typed place into a coordinate (geocode) and a coordinate back into an address (reverse geocode), **Overpass** to discover sports facilities by OSM tag within a radius (Nominatim is poor at that), and **OSRM** for driving routes. All keyless, SA-bounded, cached 15 min, proxied through `MapWeatherController` so the browser never calls them directly.

**Q5. How does the app get weather, and how is "match-time" weather chosen?**
`WeatherService.fetchFromApi` calls **Open-Meteo** `/v1/forecast` for 8 days of hourly data (temperature, humidity, precipitation probability, wind, weather code). `getForecast(lat,lng,time)` returns the hourly sample nearest the match time; if the closest sample is >12h away (e.g. game beyond the 8-day horizon) it returns `WeatherForecast.unavailable()`. Cached 30 min on a ~1 km grid.

**Q6. How do the map and weather services interact to produce playability?**
They don't call each other. `MapWeatherController.weather` orchestrates: it calls `WeatherService.getForecast` then passes that forecast, plus the sport and venue type, into `PlayabilityService.evaluate`. Playability is a pure rules engine — indoor → GOOD; no forecast → UNKNOWN; otherwise GOOD/FAIR/POOR from fixed rain/wind/temperature thresholds. Venue type comes from the map selection; weather from Open-Meteo; the verdict combines them.

**Q7. Why don't registration, notifications, or reports need maps or weather?**
Because their jobs are non-geographic: registration creates an account and sports; notifications route a user to an internal page based on `NotificationType`; reports file moderation records against a user or a social post. A `Post`/`Report`/`Notification` has no coordinate fields. Forcing a map in would be scope creep with no data to back it.

**Q8. Could a notification carry weather or a map link? Is that just not built?**
Correct — not built. `NotificationService.resolveDestination` is a `switch` mapping type → internal route (e.g. `MATCH_RESULT_POSTED → /match-result/view/{id}`). It links to a *listing* page where the map/weather live, but embeds none of that itself. "Weather in the notification" would be a future improvement, not current behaviour.

**Q9. What happens if an external API is slow, down, or rate-limited?**
Shared `RestTemplate` has 5s connect/20s read timeouts, so threads don't hang. Weather → `unavailable()` and the card reads "Forecast unavailable." Venue search → typed `VenueSearchOutcome` status: `RATE_LIMITED`→HTTP 429, `TIMEOUT`/`UNAVAILABLE`→503, and the UI shows a retry message instead of pretending there are no venues. Client-side request tokens prevent stale responses from clobbering fresh UI. The rest of the app is unaffected because the feature is isolated.

**Q10. What's the difference between the *intended architecture* and the *implemented behaviour* here?**
Intended (per `ARCHITECTURE.md` §1.11 and slides): a clean layered design where a few listing features call cached, keyless public APIs through one proxy controller. Implemented and matching: that is exactly what exists — `MapWeatherController` + three services + Leaflet. Gaps to be honest about: hard-coded to South Africa, no stored user home location, the edit page has no map widget, and playability uses fixed thresholds with a small indoor-sport allowlist (no per-sport or humidity/UV rules). None of D100–D700 were ever intended to use these features, and none do.

**Bonus Q11. Where would the panel look to verify all this quickly?**
`ARCHITECTURE.md` §1.5 (controller table shows only `MapWeatherController` is `/api`), §1.11 (external integrations table: Open-Meteo, Nominatim, Overpass, OSRM); source `controller/MapWeatherController.java`, `service/{VenueSearch,Weather,Playability}Service.java`, `model/entity/GameListing.java`, migration `V15`; templates `listings/create.html`, `listings/index.html`, `fragments/layout.html`. Slides: the Final Panel Review guide's §5 ("How does this screen/function connect to the database?", "What would happen if this data were invalid or missing?") map directly onto Q3 and Q9 above.

---

## The honest closing statement for the panel

"Maps, weather and playability are a cohesive add-on to the **game-listing** experience — create and browse. They turn a vague text location into a real pin with an address, a forecast, a GO/CAUTION/NO-GO verdict, alternative dates, and directions. Everything is keyless, cached and proxied through one controller so it fails softly. Crucially, **D100 through D700 are independent of these features** — accounts, profiles, follows, notifications and reports have no geographic dimension in the data model, so we deliberately didn't bolt maps onto them. The only indirect link is that the sport a user owns later parameterises venue search and playability."
