package com.gameon.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * ListingInvitation entity - Tracks invitations sent for a game listing.
 * Maps to 'listing_invitations' table in GameOnDb.
 * Maintains invitation history and prevents duplicate invitations.
 */
@Entity
@Table(name = "listing_invitations")
public class ListingInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invitation_id")
    private Long invitationId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_listing_id", nullable = false)
    private GameListing gameListing;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "invited_user_id", nullable = false)
    private User invitedUser;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by_user_id", nullable = false)
    private User invitedBy;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // ===== Constructors =====

    public ListingInvitation() {
    }

    public ListingInvitation(GameListing gameListing, User invitedUser, User invitedBy) {
        this.gameListing = gameListing;
        this.invitedUser = invitedUser;
        this.invitedBy = invitedBy;
        this.status = "PENDING";
    }

    // ===== Getters and Setters =====

    public Long getInvitationId() {
        return invitationId;
    }

    public void setInvitationId(Long invitationId) {
        this.invitationId = invitationId;
    }

    public GameListing getGameListing() {
        return gameListing;
    }

    public void setGameListing(GameListing gameListing) {
        this.gameListing = gameListing;
    }

    public User getInvitedUser() {
        return invitedUser;
    }

    public void setInvitedUser(User invitedUser) {
        this.invitedUser = invitedUser;
    }

    public User getInvitedBy() {
        return invitedBy;
    }

    public void setInvitedBy(User invitedBy) {
        this.invitedBy = invitedBy;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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
