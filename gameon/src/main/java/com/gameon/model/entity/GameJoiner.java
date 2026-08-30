package com.gameon.model.entity;

import com.gameon.model.enums.JoinerStatus;
import com.gameon.model.enums.Team;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * GameJoiner entity - the accepted participant roster for a listing.
 * Join-request history is stored separately in {@link JoinRequest}.
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "join_request_id")
    private JoinRequest joinRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "format_id", nullable = false)
    private SportFormat format;

    @NotNull(message = "Team is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "team", nullable = false, length = 1)
    private Team team;

    @Column(name = "primary_position_id")
    private Long primaryPositionId;

    @Column(name = "alternate_position_id")
    private Long alternatePositionId;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private JoinerStatus status = JoinerStatus.ACCEPTED;

    /** When the player explicitly confirmed attendance. Null if not yet confirmed. */
    @Column(name = "attendance_confirmed_at")
    private LocalDateTime attendanceConfirmedAt;

    /** Whether this player's departure was a late withdrawal (T-2h → T-1h). */
    @Column(name = "is_late_withdrawal", nullable = false)
    private boolean lateWithdrawal = false;

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
        this.format = gameListing.getFormat();
        this.team = team;
        this.status = JoinerStatus.ACCEPTED;
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

    public JoinRequest getJoinRequest() {
        return joinRequest;
    }

    public void setJoinRequest(JoinRequest joinRequest) {
        this.joinRequest = joinRequest;
    }

    public SportFormat getFormat() {
        return format;
    }

    public void setFormat(SportFormat format) {
        this.format = format;
    }

    public Long getPrimaryPositionId() {
        return primaryPositionId;
    }

    public void setPrimaryPositionId(Long primaryPositionId) {
        this.primaryPositionId = primaryPositionId;
    }

    public Long getAlternatePositionId() {
        return alternatePositionId;
    }

    public void setAlternatePositionId(Long alternatePositionId) {
        this.alternatePositionId = alternatePositionId;
    }

    public Long getFormatPositionId() {
        return primaryPositionId;
    }

    public void setFormatPositionId(Long formatPositionId) {
        this.primaryPositionId = formatPositionId;
    }

    public Long getAltFormatPositionId() {
        return alternatePositionId;
    }

    public void setAltFormatPositionId(Long altFormatPositionId) {
        this.alternatePositionId = altFormatPositionId;
    }

    public JoinerStatus getStatus() {
        return status;
    }

    public void setStatus(JoinerStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getAttendanceConfirmedAt() {
        return attendanceConfirmedAt;
    }

    public void setAttendanceConfirmedAt(LocalDateTime attendanceConfirmedAt) {
        this.attendanceConfirmedAt = attendanceConfirmedAt;
    }

    public boolean isLateWithdrawal() {
        return lateWithdrawal;
    }

    public void setLateWithdrawal(boolean lateWithdrawal) {
        this.lateWithdrawal = lateWithdrawal;
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
