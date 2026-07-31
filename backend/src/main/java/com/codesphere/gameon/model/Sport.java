package com.codesphere.gameon.model;

/**
 * Domain model for the dbo.sport table.
 */
public class Sport {

    private long sportId;
    private String sportName;

    public Sport() {
    }

    public Sport(long sportId, String sportName) {
        this.sportId = sportId;
        this.sportName = sportName;
    }

    public long getSportId() {
        return sportId;
    }

    public void setSportId(long sportId) {
        this.sportId = sportId;
    }

    public String getSportName() {
        return sportName;
    }

    public void setSportName(String sportName) {
        this.sportName = sportName;
    }
}
