package com.codesphere.gameon.model;

import java.time.LocalDateTime;

/**
 * Domain model for the dbo.invitation table.
 */
public class Invitation {

    private long invitationId;
    private long gameListingId;
    private long inviteeId;
    private String status;
    private LocalDateTime createdAt;

    public Invitation() {
    }

    public Invitation(long invitationId, long gameListingId, long inviteeId,
                      String status, LocalDateTime createdAt) {
        this.invitationId = invitationId;
        this.gameListingId = gameListingId;
        this.inviteeId = inviteeId;
        this.status = status;
        this.createdAt = createdAt;
    }

    public long getInvitationId() {
        return invitationId;
    }

    public void setInvitationId(long invitationId) {
        this.invitationId = invitationId;
    }

    public long getGameListingId() {
        return gameListingId;
    }

    public void setGameListingId(long gameListingId) {
        this.gameListingId = gameListingId;
    }

    public long getInviteeId() {
        return inviteeId;
    }

    public void setInviteeId(long inviteeId) {
        this.inviteeId = inviteeId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
