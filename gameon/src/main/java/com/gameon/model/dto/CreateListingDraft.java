package com.gameon.model.dto;

import com.gameon.model.enums.PrivacySetting;
import com.gameon.model.enums.SkillLevel;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CreateListingDraft implements Serializable {
    private Long formatId;
    private SkillLevel skillLevel;
    private LocalDateTime scheduledDate;
    private String location;
    private String venueName;
    private java.math.BigDecimal latitude;
    private java.math.BigDecimal longitude;
    private com.gameon.model.enums.VenueType venueType;
    private PrivacySetting privacySetting;
    private Integer durationMinutes;
    private List<Long> positionIds = new ArrayList<>();
    private List<Long> invitedFriendIds = new ArrayList<>();

    public Long getFormatId() { return formatId; }
    public void setFormatId(Long formatId) { this.formatId = formatId; }
    public SkillLevel getSkillLevel() { return skillLevel; }
    public void setSkillLevel(SkillLevel skillLevel) { this.skillLevel = skillLevel; }
    public LocalDateTime getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(LocalDateTime scheduledDate) { this.scheduledDate = scheduledDate; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getVenueName() { return venueName; }
    public void setVenueName(String venueName) { this.venueName = venueName; }
    public java.math.BigDecimal getLatitude() { return latitude; }
    public void setLatitude(java.math.BigDecimal latitude) { this.latitude = latitude; }
    public java.math.BigDecimal getLongitude() { return longitude; }
    public void setLongitude(java.math.BigDecimal longitude) { this.longitude = longitude; }
    public com.gameon.model.enums.VenueType getVenueType() { return venueType; }
    public void setVenueType(com.gameon.model.enums.VenueType venueType) { this.venueType = venueType; }
    public PrivacySetting getPrivacySetting() { return privacySetting; }
    public void setPrivacySetting(PrivacySetting privacySetting) { this.privacySetting = privacySetting; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public List<Long> getPositionIds() { return positionIds; }
    public void setPositionIds(List<Long> positionIds) {
        this.positionIds = positionIds == null ? null : new ArrayList<>(positionIds);
    }
    public List<Long> getInvitedFriendIds() { return invitedFriendIds; }
    public void setInvitedFriendIds(List<Long> invitedFriendIds) {
        this.invitedFriendIds = invitedFriendIds == null ? new ArrayList<>() : new ArrayList<>(invitedFriendIds);
    }
}
