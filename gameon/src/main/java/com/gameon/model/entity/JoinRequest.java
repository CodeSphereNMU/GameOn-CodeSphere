package com.gameon.model.entity;

import com.gameon.model.enums.JoinRequestStatus;
import com.gameon.model.enums.Team;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/** A durable request to join a game listing. Processed requests are retained as history. */
@Entity
@Table(name = "join_requests")
public class JoinRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "join_request_id")
    private Long joinRequestId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "game_listing_id", nullable = false)
    private GameListing gameListing;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "invitation_id")
    private Invitation invitation;

    @NotNull(message = "Request status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private JoinRequestStatus status = JoinRequestStatus.PENDING;

    /** Whether the creator has pre-approved this requester for a last-call place (T-2h → T-1h). */
    @Column(name = "is_last_call_approved", nullable = false)
    private boolean lastCallApproved = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public JoinRequest() {
    }

    public JoinRequest(User user, GameListing gameListing, Team team) {
        this.user = user;
        this.gameListing = gameListing;
        this.format = gameListing.getFormat();
        this.team = team;
    }

    public Long getJoinRequestId() { return joinRequestId; }
    public void setJoinRequestId(Long joinRequestId) { this.joinRequestId = joinRequestId; }
    public GameListing getGameListing() { return gameListing; }
    public void setGameListing(GameListing gameListing) { this.gameListing = gameListing; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public SportFormat getFormat() { return format; }
    public void setFormat(SportFormat format) { this.format = format; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public Long getPrimaryPositionId() { return primaryPositionId; }
    public void setPrimaryPositionId(Long primaryPositionId) { this.primaryPositionId = primaryPositionId; }
    public Long getAlternatePositionId() { return alternatePositionId; }
    public void setAlternatePositionId(Long alternatePositionId) { this.alternatePositionId = alternatePositionId; }
    public Invitation getInvitation() { return invitation; }
    public void setInvitation(Invitation invitation) { this.invitation = invitation; }
    public JoinRequestStatus getStatus() { return status; }
    public void setStatus(JoinRequestStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }
    public boolean isLastCallApproved() { return lastCallApproved; }
    public void setLastCallApproved(boolean lastCallApproved) { this.lastCallApproved = lastCallApproved; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    /** Compatibility accessors used by the existing request template. */
    @Transient
    public Long getFormatPositionId() { return primaryPositionId; }
    @Transient
    public Long getAltFormatPositionId() { return alternatePositionId; }
    @Transient
    public boolean isInvited() { return invitation != null; }
}
