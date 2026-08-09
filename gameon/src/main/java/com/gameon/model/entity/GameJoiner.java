package com.gameon.model.entity;

import com.gameon.model.enums.JoinerStatus;
import com.gameon.model.enums.Team;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * GameJoiner entity - Tracks join requests and accepted members for each listing.
 * Maps to 'game_joiners' table in GameOnDb.
 * Composite PK: (userId, gameListingId)
 */
@Entity
@Table(name = "game_joiners")
public class GameJoiner {

    @EmbeddedId
    private GameJoinerId id;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("gameListingId")
    @JoinColumn(name = "game_listing_id")
    private GameListing gameListing;

    @NotNull(message = "Team is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "team", nullable = false, length = 1)
    private Team team;

    @Column(name = "format_position_id")
    private Long formatPositionId;

    @Column(name = "alt_format_position_id")
    private Long altFormatPositionId;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private JoinerStatus status = JoinerStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // ===== Constructors =====

    public GameJoiner() {
    }

    public GameJoiner(User user, GameListing gameListing, Team team) {
        this.id = new GameJoinerId(user.getUserId(), gameListing.getGameListingId());
        this.user = user;
        this.gameListing = gameListing;
        this.team = team;
        this.status = JoinerStatus.PENDING;
    }

    // ===== Getters and Setters =====

    public GameJoinerId getId() {
        return id;
    }

    public void setId(GameJoinerId id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public GameListing getGameListing() {
        return gameListing;
    }

    public void setGameListing(GameListing gameListing) {
        this.gameListing = gameListing;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public Long getFormatPositionId() {
        return formatPositionId;
    }

    public void setFormatPositionId(Long formatPositionId) {
        this.formatPositionId = formatPositionId;
    }

    public Long getAltFormatPositionId() {
        return altFormatPositionId;
    }

    public void setAltFormatPositionId(Long altFormatPositionId) {
        this.altFormatPositionId = altFormatPositionId;
    }

    public JoinerStatus getStatus() {
        return status;
    }

    public void setStatus(JoinerStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
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
