package com.gameon.model.dto;

import java.util.List;

/**
 * Result wrapper for a venue discovery/search that distinguishes a genuinely empty result
 * from an upstream provider failure (H2 in the Create Listing Map audit).
 *
 * <p>The client uses {@link #getStatus()} to show an honest message: "no venues found in this
 * area" vs. "the venue service is temporarily busy / unavailable, please retry".</p>
 */
public class VenueSearchOutcome {

    /** How the discovery attempt resolved. */
    public enum Status {
        /** Discovery completed; {@code venues} holds the (possibly empty) matches. */
        OK,
        /** No venues were found, but discovery itself succeeded. */
        EMPTY,
        /** An upstream provider (Overpass/Nominatim) rate-limited us (HTTP 429). */
        RATE_LIMITED,
        /** An upstream provider timed out. */
        TIMEOUT,
        /** An upstream provider was unreachable or returned a server error. */
        UNAVAILABLE
    }

    private final Status status;
    private final List<VenueResult> venues;

    public VenueSearchOutcome(Status status, List<VenueResult> venues) {
        this.status = status;
        this.venues = venues == null ? List.of() : venues;
    }

    public static VenueSearchOutcome ok(List<VenueResult> venues) {
        if (venues == null || venues.isEmpty()) {
            return new VenueSearchOutcome(Status.EMPTY, List.of());
        }
        return new VenueSearchOutcome(Status.OK, venues);
    }

    public static VenueSearchOutcome failure(Status status) {
        return new VenueSearchOutcome(status, List.of());
    }

    public Status getStatus() { return status; }
    public List<VenueResult> getVenues() { return venues; }
    public boolean isFailure() {
        return status == Status.RATE_LIMITED || status == Status.TIMEOUT || status == Status.UNAVAILABLE;
    }
}
