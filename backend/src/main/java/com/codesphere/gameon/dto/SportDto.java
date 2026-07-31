package com.codesphere.gameon.dto;

/**
 * Response DTO for a sport on the user's profile.
 */
public class SportDto {

    private long sportId;
    private String sportName;
    private String skillLevel;

    public SportDto() {
    }

    public SportDto(long sportId, String sportName, String skillLevel) {
        this.sportId = sportId;
        this.sportName = sportName;
        this.skillLevel = skillLevel;
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

    public String getSkillLevel() {
        return skillLevel;
    }

    public void setSkillLevel(String skillLevel) {
        this.skillLevel = skillLevel;
    }
}
