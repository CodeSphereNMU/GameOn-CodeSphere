package com.codesphere.gameon.model;

import java.time.LocalDateTime;

/**
 * Domain model for the dbo.game_listing table.
 */
public class GameListing {

    private long gameListingId;
    private LocalDateTime date;
    private boolean isCompleted;
    private boolean isPrivate;
    private String location;
    private String skillLevel;
    private long creatorId;
    private long formatId;

    public GameListing() {
    }

    public GameListing(long gameListingId, LocalDateTime date, boolean isCompleted, boolean isPrivate,
                       String location, String skillLevel, long creatorId, long formatId) {
        this.gameListingId = gameListingId;
        this.date = date;
        this.isCompleted = isCompleted;
        this.isPrivate = isPrivate;
        this.location = location;
        this.skillLevel = skillLevel;
        this.creatorId = creatorId;
        this.formatId = formatId;
    }

    public long getGameListingId() {
        return gameListingId;
    }

    public void setGameListingId(long gameListingId) {
        this.gameListingId = gameListingId;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean aPrivate) {
        isPrivate = aPrivate;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getSkillLevel() {
        return skillLevel;
    }

    public void setSkillLevel(String skillLevel) {
        this.skillLevel = skillLevel;
    }

    public long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(long creatorId) {
        this.creatorId = creatorId;
    }

    public long getFormatId() {
        return formatId;
    }

    public void setFormatId(long formatId) {
        this.formatId = formatId;
    }
}
