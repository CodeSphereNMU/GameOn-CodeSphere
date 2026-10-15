# GameOn — Use Case Diagram

This diagram shows the GameOn actors and the use cases attached to each.

## Actors

- **Registered User (`ROLE_USER`)** — the main player; access to all core gameplay and social features. Ownership (edit own listing, delete own post, etc.) is enforced in code rather than by role.
- **Moderator / Admin (`ROLE_ADMIN`)** — everything a User can do *plus* moderation. (Internally the role is named `ADMIN`, but the UI/URLs treat it as "Moderator".)

### Secondary system actors
Server-side external APIs and the internal scheduler. No payment, email, SMS, or OAuth integrations exist in the codebase.

- **Open-Meteo** — weather forecasts for playability.
- **OpenStreetMap (Nominatim / Overpass / OSRM)** — geocoding, venue discovery, routing.
- **Scheduler / Time** — an internal `@Scheduled` job (runs every 60s) that drives the listing lifecycle (confirmation reminders, deadlines, finalisation).

## Diagram

```mermaid
graph LR
    %% ===== ACTORS =====
    User(("👤 Registered User"))
    Mod(("👤 Moderator / Admin"))

    %% ===== SECONDARY SYSTEM ACTORS =====
    Weather(["☁️ Open-Meteo<br/>(Weather)"])
    Maps(["🗺️ OpenStreetMap<br/>Nominatim / Overpass / OSRM"])
    Scheduler(["⏱️ Scheduler / Time"])

    %% ===== ACTOR GENERALIZATION =====
    Mod -. is a .-> User

    %% ===== ACCOUNT & ACCESS =====
    User --> UC1["Register (2-step: account + sports)"]
    User --> UC2["Log In"]
    User --> UC3["Reset Password"]

    %% ===== GAME LISTINGS =====
    User --> UC4["Browse Game Listings"]
    User --> UC5["View Listing Detail"]
    User --> UC6["Create Game Listing"]
    User --> UC7["Edit / Cancel Own Listing"]

    %% ===== JOINING & LOBBY =====
    User --> UC8["Send Join Request"]
    User --> UC9["Leave / Withdraw Request"]
    User --> UC10["Confirm Attendance"]
    User --> UC11["Claim Last-Call Place"]
    User --> UC12["View Lobby (created / joined / history)"]
    User --> UC13["Manage Join Requests (accept / reject / last-call)"]

    %% ===== MATCH RESULTS =====
    User --> UC14["Submit / Update Match Result"]
    User --> UC15["View Leaderboard"]

    %% ===== SOCIAL =====
    User --> UC16["View Social Feed"]
    User --> UC17["Create / Edit / Delete Post"]
    User --> UC18["Like & Comment on Posts"]

    %% ===== PROFILE & SOCIAL GRAPH =====
    User --> UC19["Manage Own Profile & Sports"]
    User --> UC20["View Other Profiles"]
    User --> UC21["Follow / Unfollow"]
    User --> UC22["Block / Unblock"]

    %% ===== REPORTING, NOTIFICATIONS, MAP =====
    User --> UC23["Report User / Post"]
    User --> UC24["View & Manage Notifications"]
    User --> UC25["View Venue Map / Weather / Playability"]

    %% ===== MODERATOR USE CASES =====
    Mod --> UC26["View Moderation Dashboard"]
    Mod --> UC27["Review Report Queue"]
    Mod --> UC28["Dismiss Report"]
    Mod --> UC29["Issue Warning / Strike"]
    Mod --> UC30["Remove Content + Warn"]
    Mod --> UC31["Suspend User"]
    Mod --> UC32["Ban User"]
    Mod --> UC33["Reinstate / Unban / Reset Warnings"]
    Mod --> UC34["Restore Removed Post"]
    Mod --> UC35["View Audit / Moderation History"]

    %% ===== SECONDARY ACTOR LINKS =====
    UC25 --> Weather
    UC25 --> Maps
    UC6 --> Maps
    Scheduler --> UC36["Listing Lifecycle<br/>(reminders, deadlines, finalisation)"]

    classDef uc fill:#eef6ff,stroke:#4a90d9,color:#123;
    classDef sys fill:#f3f0ff,stroke:#8a6fd4,color:#123;
    class UC1,UC2,UC3,UC4,UC5,UC6,UC7,UC8,UC9,UC10,UC11,UC12,UC13,UC14,UC15,UC16,UC17,UC18,UC19,UC20,UC21,UC22,UC23,UC24,UC25,UC26,UC27,UC28,UC29,UC30,UC31,UC32,UC33,UC34,UC35,UC36 uc;
    class Weather,Maps,Scheduler sys;
```

## Authorization notes (from the code)

- Only `/moderator/**` and `/admin/**` are role-gated (`ROLE_ADMIN`). All other authenticated use cases are open to any logged-in user, with ownership (e.g. *edit own listing*, *delete own post*) enforced in code.
- The Moderator/Admin "is a" Registered User (generalization arrow), so it inherits every User use case plus the moderation set.
- Suspended/banned accounts cannot log in at all — `AccountStatus` gates authentication.
