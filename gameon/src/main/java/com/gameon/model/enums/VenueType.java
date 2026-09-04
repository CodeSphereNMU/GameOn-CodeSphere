package com.gameon.model.enums;

/**
 * Indicates whether a venue is indoor or outdoor.
 * <p>
 * Drives the playability engine: OUTDOOR venues are affected by weather
 * (rain, wind, temperature), while INDOOR venues are always considered
 * playable regardless of forecast.
 */
public enum VenueType {
    INDOOR,
    OUTDOOR
}
