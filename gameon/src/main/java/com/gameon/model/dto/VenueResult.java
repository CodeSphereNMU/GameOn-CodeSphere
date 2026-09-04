package com.gameon.model.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A sports venue returned by the venue-search service (South Africa only, backed by
 * OpenStreetMap Nominatim).
 */
public class VenueResult implements Serializable {

    private String name;              // venue name, e.g. "Boardwalk Padel"
    private String address;           // full display address
    private double latitude;
    private double longitude;
    private String venueType;         // INDOOR / OUTDOOR (best-effort inferred)
    private String sport;             // sport(s) available at the venue (best-effort)
    private Double distanceKm;        // distance from the user/selected location, when supplied

    private List<String> facilities = new ArrayList<>(); // e.g. Parking, Lighting, Changerooms
    private String website;           // official site / booking link, when known
    private String bookingUrl;        // explicit booking/reservation link, when known
    private String phone;             // contact number, when known
    private String suitability;       // human-readable suitability for the selected sport
    private boolean matchesSport;     // true when the venue is a good fit for the selected sport
    private int score;                // internal ranking score (higher = more suitable)

    public VenueResult() {
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public String getVenueType() { return venueType; }
    public void setVenueType(String venueType) { this.venueType = venueType; }
    public String getSport() { return sport; }
    public void setSport(String sport) { this.sport = sport; }
    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }
    public List<String> getFacilities() { return facilities; }
    public void setFacilities(List<String> facilities) {
        this.facilities = facilities == null ? new ArrayList<>() : new ArrayList<>(facilities);
    }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    public String getBookingUrl() { return bookingUrl; }
    public void setBookingUrl(String bookingUrl) { this.bookingUrl = bookingUrl; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getSuitability() { return suitability; }
    public void setSuitability(String suitability) { this.suitability = suitability; }
    public boolean isMatchesSport() { return matchesSport; }
    public void setMatchesSport(boolean matchesSport) { this.matchesSport = matchesSport; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
}
