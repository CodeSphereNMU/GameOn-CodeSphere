package com.codesphere.gameon.model;

/**
 * Domain model for the dbo.sport_format table.
 */
public class SportFormat {

    private long formatId;
    private String formatName;
    private boolean hasPositions;
    private int noPlayers;
    private long sportId;

    public SportFormat() {
    }

    public SportFormat(long formatId, String formatName, boolean hasPositions, int noPlayers, long sportId) {
        this.formatId = formatId;
        this.formatName = formatName;
        this.hasPositions = hasPositions;
        this.noPlayers = noPlayers;
        this.sportId = sportId;
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

    public long getSportId() {
        return sportId;
    }

    public void setSportId(long sportId) {
        this.sportId = sportId;
    }
}
