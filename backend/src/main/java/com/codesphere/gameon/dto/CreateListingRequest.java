package com.codesphere.gameon.dto;

import java.util.List;

/**
 * Request DTO for POST /api/game-listings.
 */
public class CreateListingRequest {

    private Long sportId;
    private Long formatId;
    private String skillLevel;
    private String date;
    private String time;
    private String location;
    private Boolean isPrivate;
    private Boolean anyPosition;
    private Long positionId;
    private Long alternatePositionId;
    private List<Long> invitedFriendIds;

    public CreateListingRequest() {
    }

    public Long getSportId() {
        return sportId;
    }

    public void setSportId(Long sportId) {
        this.sportId = sportId;
    }

    public Long getFormatId() {
        return formatId;
    }

    public void setFormatId(Long formatId) {
        this.formatId = formatId;
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

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Boolean getIsPrivate() {
        return isPrivate;
    }

    public void setIsPrivate(Boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    public Boolean getAnyPosition() {
        return anyPosition;
    }

    public void setAnyPosition(Boolean anyPosition) {
        this.anyPosition = anyPosition;
    }

    public Long getPositionId() {
        return positionId;
    }

    public void setPositionId(Long positionId) {
        this.positionId = positionId;
    }

    public Long getAlternatePositionId() {
        return alternatePositionId;
    }

    public void setAlternatePositionId(Long alternatePositionId) {
        this.alternatePositionId = alternatePositionId;
    }

    public List<Long> getInvitedFriendIds() {
        return invitedFriendIds;
    }

    public void setInvitedFriendIds(List<Long> invitedFriendIds) {
        this.invitedFriendIds = invitedFriendIds;
    }
}
