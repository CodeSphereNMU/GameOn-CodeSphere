package com.gameon.model.entity;

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

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Invitation() {}

    public Invitation(GameListing gameListing, User invitee) {
        this.gameListing = gameListing;
        this.invitee = invitee;
    }

    public Long getInvitationId() { return invitationId; }
    public GameListing getGameListing() { return gameListing; }
    public User getInvitee() { return invitee; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setStatus(String status) { this.status = status; }
}
