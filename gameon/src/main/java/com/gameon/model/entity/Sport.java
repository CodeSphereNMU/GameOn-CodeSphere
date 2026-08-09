package com.gameon.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Sport entity - Reference table of available sports.
 * Maps to 'sports' table in GameOnDb.
 */
@Entity
@Table(name = "sports")
public class Sport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sport_id")
    private Long sportId;

    @NotBlank(message = "Sport name is required")
    @Column(name = "sport_name", nullable = false, unique = true, length = 50)
    private String sportName;

    @Positive(message = "Number of players must be positive")
    @Column(name = "no_players", nullable = false)
    private Integer noPlayers;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // ===== Relationships =====

    @OneToMany(mappedBy = "sport", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<SportFormat> formats = new ArrayList<>();

    @OneToMany(mappedBy = "sport", fetch = FetchType.LAZY)
    private List<UserSportProfile> playerProfiles = new ArrayList<>();

    // ===== Constructors =====

    public Sport() {
    }

    public Sport(String sportName, Integer noPlayers) {
        this.sportName = sportName;
        this.noPlayers = noPlayers;
    }

    // ===== Getters and Setters =====

    public Long getSportId() {
        return sportId;
    }

    public void setSportId(Long sportId) {
        this.sportId = sportId;
    }

    public String getSportName() {
        return sportName;
    }

    public void setSportName(String sportName) {
        this.sportName = sportName;
    }

    public Integer getNoPlayers() {
        return noPlayers;
    }

    public void setNoPlayers(Integer noPlayers) {
        this.noPlayers = noPlayers;
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

    public List<SportFormat> getFormats() {
        return formats;
    }

    public void setFormats(List<SportFormat> formats) {
        this.formats = formats;
    }

    public List<UserSportProfile> getPlayerProfiles() {
        return playerProfiles;
    }

    public void setPlayerProfiles(List<UserSportProfile> playerProfiles) {
        this.playerProfiles = playerProfiles;
    }
}
