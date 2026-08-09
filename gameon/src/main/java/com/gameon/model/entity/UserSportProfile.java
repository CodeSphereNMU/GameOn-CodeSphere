package com.gameon.model.entity;

import com.gameon.model.enums.SkillLevel;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * UserSportProfile entity - Tracks which sports a user plays + per-sport stats.
 * Maps to 'user_sport_profiles' table in GameOnDb.
 * Composite PK: (userId, sportId)
 */
@Entity
@Table(name = "user_sport_profiles")
public class UserSportProfile {

    @EmbeddedId
    private UserSportProfileId id;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("sportId")
    @JoinColumn(name = "sport_id")
    private Sport sport;

    @NotNull(message = "Skill level is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "skill_level", nullable = false, length = 20)
    private SkillLevel skillLevel;

    @Min(value = 0, message = "Wins cannot be negative")
    @Column(name = "wins", nullable = false)
    private Integer wins = 0;

    @Min(value = 0, message = "Losses cannot be negative")
    @Column(name = "losses", nullable = false)
    private Integer losses = 0;

    @Column(name = "win_percentage", nullable = false)
    private Double winPercentage = 0.0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // ===== Constructors =====

    public UserSportProfile() {
    }

    public UserSportProfile(User user, Sport sport, SkillLevel skillLevel) {
        this.id = new UserSportProfileId(user.getUserId(), sport.getSportId());
        this.user = user;
        this.sport = sport;
        this.skillLevel = skillLevel;
    }

    // ===== Business Methods =====

    /**
     * Recalculates win percentage based on current wins and losses.
     */
    public void calculateWinPercentage() {
        int totalGames = wins + losses;
        if (totalGames > 0) {
            this.winPercentage = (double) wins / totalGames * 100.0;
        } else {
            this.winPercentage = 0.0;
        }
        this.updatedAt = LocalDateTime.now();
    }

    // ===== Getters and Setters =====

    public UserSportProfileId getId() {
        return id;
    }

    public void setId(UserSportProfileId id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Sport getSport() {
        return sport;
    }

    public void setSport(Sport sport) {
        this.sport = sport;
    }

    public SkillLevel getSkillLevel() {
        return skillLevel;
    }

    public void setSkillLevel(SkillLevel skillLevel) {
        this.skillLevel = skillLevel;
    }

    public Integer getWins() {
        return wins;
    }

    public void setWins(Integer wins) {
        this.wins = wins;
    }

    public Integer getLosses() {
        return losses;
    }

    public void setLosses(Integer losses) {
        this.losses = losses;
    }

    public Double getWinPercentage() {
        return winPercentage;
    }

    public void setWinPercentage(Double winPercentage) {
        this.winPercentage = winPercentage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
