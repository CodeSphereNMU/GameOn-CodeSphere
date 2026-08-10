package com.codesphere.gameon.dto;

import java.util.List;

/**
 * Response DTO for the full listing detail view, including roster information.
 */
public class ListingDetailDto {

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
    private boolean hasPositions;
    private boolean isPrivate;
    private long formatId;
    private boolean isCreator;
    private boolean isAcceptedParticipant;
    private boolean hasPendingRequest;
    private List<RosterEntryDto> teamA;
    private List<RosterEntryDto> teamB;

    public ListingDetailDto() {
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

    public boolean isHasPositions() {
        return hasPositions;
    }

    public void setHasPositions(boolean hasPositions) {
        this.hasPositions = hasPositions;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    public long getFormatId() {
        return formatId;
    }

    public void setFormatId(long formatId) {
        this.formatId = formatId;
    }

    public boolean isCreator() {
        return isCreator;
    }

    public void setCreator(boolean isCreator) {
        this.isCreator = isCreator;
    }

    public boolean isAcceptedParticipant() {
        return isAcceptedParticipant;
    }

    public void setAcceptedParticipant(boolean isAcceptedParticipant) {
        this.isAcceptedParticipant = isAcceptedParticipant;
    }

    public boolean isHasPendingRequest() {
        return hasPendingRequest;
    }

    public void setHasPendingRequest(boolean hasPendingRequest) {
        this.hasPendingRequest = hasPendingRequest;
    }

    public List<RosterEntryDto> getTeamA() {
        return teamA;
    }

    public void setTeamA(List<RosterEntryDto> teamA) {
        this.teamA = teamA;
    }

    public List<RosterEntryDto> getTeamB() {
        return teamB;
    }

    public void setTeamB(List<RosterEntryDto> teamB) {
        this.teamB = teamB;
    }
}
