package com.gameon.model.entity;

import com.gameon.model.enums.InvitationStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Courtesy invitation. An invitee must still submit a join request. */
@Entity
@Table(name = "invitation", uniqueConstraints =
        @UniqueConstraint(name = "UQ_invitation_listing_user",
                columnNames = {"game_listing_id", "invitee_id"}))
public class Invitation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invitation_id")
    private Long invitationId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "game_listing_id", nullable = false)
    private GameListing gameListing;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "invitee_id", nullable = false)
    private User invitee;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InvitationStatus status = InvitationStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Invitation() {}

    public Invitation(GameListing gameListing, User invitee) {
        this.gameListing = gameListing;
        this.invitee = invitee;
    }

    public Long getInvitationId() { return invitationId; }
    public void setInvitationId(Long invitationId) { this.invitationId = invitationId; }
    public GameListing getGameListing() { return gameListing; }
    public void setGameListing(GameListing gameListing) { this.gameListing = gameListing; }
    public User getInvitee() { return invitee; }
    public void setInvitee(User invitee) { this.invitee = invitee; }
    public InvitationStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setStatus(InvitationStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }
}
