package com.gameon.model.dto;

import java.io.Serializable;

/**
 * The outcome of the playability engine for a given sport, venue type and forecast.
 */
public class PlayabilityResult implements Serializable {

    /** Traffic-light rating for playing conditions. */
    public enum Rating {
        GOOD("GREEN", "bi-check-circle-fill", "success", "Good Playing Conditions"),
        FAIR("ORANGE", "bi-exclamation-triangle-fill", "warning", "Fair Playing Conditions"),
        POOR("RED", "bi-x-circle-fill", "danger", "Poor Playing Conditions"),
        UNKNOWN("GREY", "bi-question-circle-fill", "secondary", "Conditions Unknown");

        private final String colour;
        private final String icon;
        private final String bootstrapClass;
        private final String heading;

        Rating(String colour, String icon, String bootstrapClass, String heading) {
            this.colour = colour;
            this.icon = icon;
            this.bootstrapClass = bootstrapClass;
            this.heading = heading;
        }

        public String getColour() { return colour; }
        public String getIcon() { return icon; }
        public String getBootstrapClass() { return bootstrapClass; }
        public String getHeading() { return heading; }
    }

    private Rating rating;
    private String message;
    private boolean indoor;

    public PlayabilityResult() {
    }

    public PlayabilityResult(Rating rating, String message, boolean indoor) {
        this.rating = rating;
        this.message = message;
        this.indoor = indoor;
    }

    public Rating getRating() { return rating; }
    public void setRating(Rating rating) { this.rating = rating; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public boolean isIndoor() { return indoor; }
    public void setIndoor(boolean indoor) { this.indoor = indoor; }

    // Convenience getters for template binding
    public String getColour() { return rating == null ? null : rating.getColour(); }
    public String getIcon() { return rating == null ? null : rating.getIcon(); }
    public String getBootstrapClass() { return rating == null ? null : rating.getBootstrapClass(); }
    public String getHeading() { return rating == null ? null : rating.getHeading(); }
}
