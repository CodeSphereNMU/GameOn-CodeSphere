package com.codesphere.gameon.dto;

/**
 * Response DTO for a sport format.
 */
public class FormatDto {

    private long formatId;
    private String formatName;
    private boolean hasPositions;
    private int noPlayers;

    public FormatDto() {
    }

    public FormatDto(long formatId, String formatName, boolean hasPositions, int noPlayers) {
        this.formatId = formatId;
        this.formatName = formatName;
        this.hasPositions = hasPositions;
        this.noPlayers = noPlayers;
    }

    public long getFormatId() {
        return formatId;
    }

    public void setFormatId(long formatId) {
        this.formatId = formatId;
    }

    public String getFormatName() {
        return formatName;
    }

    public void setFormatName(String formatName) {
        this.formatName = formatName;
    }

    public boolean isHasPositions() {
        return hasPositions;
    }

    public void setHasPositions(boolean hasPositions) {
        this.hasPositions = hasPositions;
    }

    public int getNoPlayers() {
        return noPlayers;
    }

    public void setNoPlayers(int noPlayers) {
        this.noPlayers = noPlayers;
    }
}
