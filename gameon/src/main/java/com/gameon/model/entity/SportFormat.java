package com.gameon.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SportFormat entity - Format variations per sport (5v5, 3v3, Doubles, etc.).
 * Maps to 'sport_formats' table in GameOnDb.
 */
@Entity
@Table(name = "sport_formats")
public class SportFormat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "format_id")
    private Long formatId;

    @NotBlank(message = "Format name is required")
    @Column(name = "format_name", nullable = false, length = 50)
    private String formatName;

    @Positive(message = "Number of players must be positive")
    @Column(name = "no_players", nullable = false)
    private Integer noPlayers;

    @Column(name = "has_positions", nullable = false)
    private Boolean hasPositions = false;

    @Positive(message = "Duration must be positive")
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // ===== Relationships =====

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sport_id", nullable = false)
    private Sport sport;

    @OneToMany(mappedBy = "format", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<FormatPosition> positions = new ArrayList<>();

    @OneToMany(mappedBy = "format", fetch = FetchType.LAZY)
    private List<GameListing> listings = new ArrayList<>();

    // ===== Constructors =====

    public SportFormat() {
    }

    public SportFormat(String formatName, Integer noPlayers, Boolean hasPositions, Sport sport) {
        this.formatName = formatName;
        this.noPlayers = noPlayers;
        this.hasPositions = hasPositions;
        this.sport = sport;
    }

    // ===== Getters and Setters =====

    public Long getFormatId() {
        return formatId;
    }

    public void setFormatId(Long formatId) {
        this.formatId = formatId;
    }

    public String getFormatName() {
        return formatName;
    }

    public void setFormatName(String formatName) {
        this.formatName = formatName;
    }

    public Integer getNoPlayers() {
        return noPlayers;
    }

    public void setNoPlayers(Integer noPlayers) {
        this.noPlayers = noPlayers;
    }

    public Boolean getHasPositions() {
        return hasPositions;
    }

    public void setHasPositions(Boolean hasPositions) {
        this.hasPositions = hasPositions;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
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

    public Sport getSport() {
        return sport;
    }

    public void setSport(Sport sport) {
        this.sport = sport;
    }

    public List<FormatPosition> getPositions() {
        return positions;
    }

    public void setPositions(List<FormatPosition> positions) {
        this.positions = positions;
    }

    public List<GameListing> getListings() {
        return listings;
    }

    public void setListings(List<GameListing> listings) {
        this.listings = listings;
    }
}
