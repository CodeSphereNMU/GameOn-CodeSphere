package com.gameon.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Session entity - Confirmed game session (created 2hrs before start when listing full).
 * Maps to 'sessions' table in GameOnDb.
 * One-to-one relationship with GameListing (unique FK constraint).
 */
@Entity
@Table(name = "sessions")
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long sessionId;

    @NotNull(message = "Session date is required")
    @Column(name = "session_date", nullable = false)
    private LocalDateTime sessionDate;

    @NotBlank(message = "Location is required")
    @Column(name = "location", nullable = false, length = 200)
    private String location;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // ===== Relationships =====

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "game_listing_id", nullable = false, unique = true)
    private GameListing gameListing;

    // ===== Constructors =====

    public Session() {
    }

    public Session(GameListing gameListing) {
        this.gameListing = gameListing;
        this.sessionDate = gameListing.getScheduledDate();
        this.location = gameListing.getLocation();
    }

    // ===== Getters and Setters =====

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public LocalDateTime getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(LocalDateTime sessionDate) {
        this.sessionDate = sessionDate;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
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

    public GameListing getGameListing() {
        return gameListing;
    }

    public void setGameListing(GameListing gameListing) {
        this.gameListing = gameListing;
    }
}
