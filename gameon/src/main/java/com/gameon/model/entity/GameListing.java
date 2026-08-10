package com.gameon.model.entity;

import com.gameon.model.enums.PrivacySetting;
import com.gameon.model.enums.SkillLevel;
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
    @Future(message = "Scheduled date must be in the future")
    @Column(name = "scheduled_date", nullable = false)
    private LocalDateTime scheduledDate;

    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted = false;

    @NotBlank(message = "Location is required")
    @Size(max = 200, message = "Location must be at most 200 characters")
    @Column(name = "location", nullable = false, length = 200)
    private String location;

    @NotNull(message = "Session duration is required")
    @Min(value = 1, message = "Session duration must be at least 1 hour")
    @Max(value = 8, message = "Session duration must not exceed 8 hours")
    @Column(name = "session_duration", nullable = false)
    private Integer sessionDuration = 1;

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
    private Session session;

    @OneToOne(mappedBy = "gameListing", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private MatchResult matchResult;

    // ===== Constructors =====

    public GameListing() {
    }

    public GameListing(User creator, SportFormat format, SkillLevel skillLevel,
                       LocalDateTime scheduledDate, String location, PrivacySetting privacySetting,
                       Integer sessionDuration) {
        this.creator = creator;
        this.format = format;
        this.skillLevel = skillLevel;
        this.scheduledDate = scheduledDate;
        this.location = location;
        this.privacySetting = privacySetting;
        this.sessionDuration = sessionDuration;
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

    public Boolean getIsCompleted() {
        return isCompleted;
    }

    public void setIsCompleted(Boolean isCompleted) {
        this.isCompleted = isCompleted;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public PrivacySetting getPrivacySetting() {
        return privacySetting;
    }

    public void setPrivacySetting(PrivacySetting privacySetting) {
        this.privacySetting = privacySetting;
    }

    public Integer getSessionDuration() {
        return sessionDuration;
    }

    public void setSessionDuration(Integer sessionDuration) {
        this.sessionDuration = sessionDuration;
    }

    /**
     * Returns the end time of this listing's session (start + duration).
     */
    public LocalDateTime getSessionEndTime() {
        return scheduledDate.plusHours(sessionDuration != null ? sessionDuration : 1);
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

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public MatchResult getMatchResult() {
        return matchResult;
    }

    public void setMatchResult(MatchResult matchResult) {
        this.matchResult = matchResult;
    }
}
