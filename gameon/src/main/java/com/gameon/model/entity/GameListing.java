package com.gameon.model.entity;

import com.gameon.model.enums.PrivacySetting;
import com.gameon.model.enums.SkillLevel;
import com.gameon.model.enums.ListingStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * GameListing entity - A game session created by a user seeking players.
 * Maps to 'game_listings' table in GameOnDb.
 */
@Entity
@Table(name = "game_listings")
public class GameListing extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "game_listing_id")
    private Long gameListingId;

    @NotNull(message = "Skill level is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "skill_level", nullable = false, length = 20)
    private SkillLevel skillLevel;

    @NotNull(message = "Scheduled date is required")
    @Column(name = "scheduled_date", nullable = false)
    private LocalDateTime scheduledDate;

    @NotNull(message = "Listing status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "listing_status", nullable = false, length = 40)
    private ListingStatus listingStatus = ListingStatus.OPEN;

    @NotBlank(message = "Location is required")
    @Size(max = 200, message = "Location must be at most 200 characters")
    @Column(name = "location", nullable = false, length = 200)
    private String location;

    // ===== Venue intelligence (Map + Weather + Playability upgrade) =====
    // All nullable so legacy listings created before this feature remain valid.

    @Size(max = 200, message = "Venue name must be at most 200 characters")
    @Column(name = "venue_name", length = 200)
    private String venueName;

    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
    @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
    @Column(name = "latitude", precision = 9, scale = 6)
    private java.math.BigDecimal latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
    @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
    @Column(name = "longitude", precision = 9, scale = 6)
    private java.math.BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "venue_type", length = 10)
    private com.gameon.model.enums.VenueType venueType;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    @Max(value = 480, message = "Duration must not exceed 480 minutes")
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes = 60;

    @NotNull(message = "Privacy setting is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "privacy_setting", nullable = false, length = 10)
    private PrivacySetting privacySetting = PrivacySetting.PUBLIC;

    // ===== Relationships =====

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "format_id", nullable = false)
    private SportFormat format;

    @OneToMany(mappedBy = "gameListing", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GameJoiner> joiners = new ArrayList<>();

    @OneToOne(mappedBy = "gameListing", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private MatchResult matchResult;

    // ===== Constructors =====

    public GameListing() {
    }

    public GameListing(User creator, SportFormat format, SkillLevel skillLevel,
                       LocalDateTime scheduledDate, String location, PrivacySetting privacySetting,
                       Integer durationMinutes) {
        this.creator = creator;
        this.format = format;
        this.skillLevel = skillLevel;
        this.scheduledDate = scheduledDate;
        this.location = location;
        this.privacySetting = privacySetting;
        this.durationMinutes = durationMinutes;
    }

    // ===== Getters and Setters =====

    public Long getGameListingId() {
        return gameListingId;
    }

    public void setGameListingId(Long gameListingId) {
        this.gameListingId = gameListingId;
    }

    public SkillLevel getSkillLevel() {
        return skillLevel;
    }

    public void setSkillLevel(SkillLevel skillLevel) {
        this.skillLevel = skillLevel;
    }

    public LocalDateTime getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(LocalDateTime scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public ListingStatus getListingStatus() {
        return listingStatus;
    }

    public void setListingStatus(ListingStatus listingStatus) {
        this.listingStatus = listingStatus;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getVenueName() {
        return venueName;
    }

    public void setVenueName(String venueName) {
        this.venueName = venueName;
    }

    public java.math.BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(java.math.BigDecimal latitude) {
        this.latitude = latitude;
    }

    public java.math.BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(java.math.BigDecimal longitude) {
        this.longitude = longitude;
    }

    public com.gameon.model.enums.VenueType getVenueType() {
        return venueType;
    }

    public void setVenueType(com.gameon.model.enums.VenueType venueType) {
        this.venueType = venueType;
    }

    /** True when this listing has usable map coordinates. */
    @Transient
    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }

    public PrivacySetting getPrivacySetting() {
        return privacySetting;
    }

    public void setPrivacySetting(PrivacySetting privacySetting) {
        this.privacySetting = privacySetting;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    /**
     * Returns the end time of this listing's session (start + duration).
     */
    public LocalDateTime getSessionEndTime() {
        return scheduledDate.plusMinutes(durationMinutes != null ? durationMinutes : 60);
    }

    /**
     * Returns the blocked-until time (session end + 60 min travel buffer).
     */
    public LocalDateTime getBlockedUntilTime() {
        return getSessionEndTime().plusMinutes(60);
    }

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }

    public SportFormat getFormat() {
        return format;
    }

    public void setFormat(SportFormat format) {
        this.format = format;
    }

    public List<GameJoiner> getJoiners() {
        return joiners;
    }

    public void setJoiners(List<GameJoiner> joiners) {
        this.joiners = joiners;
    }

    public MatchResult getMatchResult() {
        return matchResult;
    }

    public void setMatchResult(MatchResult matchResult) {
        this.matchResult = matchResult;
    }

    /** Time-based phase shown in the UI and readable database view. */
    @Transient
    public String getEffectiveStatus() {
        if (listingStatus != ListingStatus.CONFIRMED || scheduledDate == null) {
            return listingStatus.name();
        }
        LocalDateTime now = LocalDateTime.now();
        if (!now.isBefore(getSessionEndTime())) {
            return "AWAITING_RESULT";
        }
        if (!now.isBefore(scheduledDate)) {
            return "IN_PROGRESS";
        }
        return ListingStatus.CONFIRMED.name();
    }
}
