package com.codesphere.gameon.dto;

/**
 * Represents one participant in a listing's roster.
 */
public class RosterEntryDto {

    private String username;
    private String positionName;
    private String alternatePositionName;

    public RosterEntryDto() {
    }

    public RosterEntryDto(String username, String positionName) {
        this.username = username;
        this.positionName = positionName;
        this.alternatePositionName = null;
    }

    public RosterEntryDto(String username, String positionName, String alternatePositionName) {
        this.username = username;
        this.positionName = positionName;
        this.alternatePositionName = alternatePositionName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPositionName() {
        return positionName;
    }

    public void setPositionName(String positionName) {
        this.positionName = positionName;
    }

    public String getAlternatePositionName() {
        return alternatePositionName;
    }

    public void setAlternatePositionName(String alternatePositionName) {
        this.alternatePositionName = alternatePositionName;
    }
}
