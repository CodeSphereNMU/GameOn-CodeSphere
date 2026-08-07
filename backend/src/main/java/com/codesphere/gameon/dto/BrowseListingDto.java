package com.codesphere.gameon.dto;

/**
 * Response DTO representing a single listing card in browse results.
 */
public class BrowseListingDto {

    private long gameListingId;
    private String sportName;
    private String formatName;
    private String skillLevel;
    private String date;
    private String sessionWindow;
    private String location;
    private int spotsFilled;
    private int totalSpots;
    private String creatorUsername;

    public BrowseListingDto() {
    }

    public long getGameListingId() {
        return gameListingId;
    }

    public void setGameListingId(long gameListingId) {
        this.gameListingId = gameListingId;
    }

    public String getSportName() {
        return sportName;
    }

    public void setSportName(String sportName) {
        this.sportName = sportName;
    }

    public String getFormatName() {
        return formatName;
    }

    public void setFormatName(String formatName) {
        this.formatName = formatName;
    }

    public String getSkillLevel() {
        return skillLevel;
    }

    public void setSkillLevel(String skillLevel) {
        this.skillLevel = skillLevel;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getSessionWindow() {
        return sessionWindow;
    }

    public void setSessionWindow(String sessionWindow) {
        this.sessionWindow = sessionWindow;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getSpotsFilled() {
        return spotsFilled;
    }

    public void setSpotsFilled(int spotsFilled) {
        this.spotsFilled = spotsFilled;
    }

    public int getTotalSpots() {
        return totalSpots;
    }

    public void setTotalSpots(int totalSpots) {
        this.totalSpots = totalSpots;
    }

    public String getCreatorUsername() {
        return creatorUsername;
    }

    public void setCreatorUsername(String creatorUsername) {
        this.creatorUsername = creatorUsername;
    }
}
