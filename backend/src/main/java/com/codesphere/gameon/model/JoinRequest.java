package com.codesphere.gameon.model;

import java.time.LocalDateTime;

/**
 * Domain model for the dbo.join_request table.
 */
public class JoinRequest {

    private long joinRequestId;
    private long gameListingId;
    private long userId;
    private long formatId;
    private String team;                  // "A" or "B"
    private Long positionId;              // nullable
    private Long alternatePositionId;     // nullable
    private Long invitationId;            // nullable — set when player was invited
    private String status;                // PENDING, ACCEPTED, REJECTED, WITHDRAWN, EXPIRED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public JoinRequest() {
    }

    public JoinRequest(long joinRequestId, long gameListingId, long userId, long formatId,
                       String team, Long positionId, Long alternatePositionId, Long invitationId,
                       String status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.joinRequestId = joinRequestId;
        this.gameListingId = gameListingId;
        this.userId = userId;
        this.formatId = formatId;
        this.team = team;
        this.positionId = positionId;
        this.alternatePositionId = alternatePositionId;
        this.invitationId = invitationId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getJoinRequestId() {
        return joinRequestId;
    }

    public void setJoinRequestId(long joinRequestId) {
        this.joinRequestId = joinRequestId;
    }

    public long getGameListingId() {
        return gameListingId;
    }

    public void setGameListingId(long gameListingId) {
        this.gameListingId = gameListingId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getFormatId() {
        return formatId;
    }

    public void setFormatId(long formatId) {
        this.formatId = formatId;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public Long getPositionId() {
        return positionId;
    }

    public void setPositionId(Long positionId) {
        this.positionId = positionId;
    }

    public Long getAlternatePositionId() {
        return alternatePositionId;
    }

    public void setAlternatePositionId(Long alternatePositionId) {
        this.alternatePositionId = alternatePositionId;
    }

    public Long getInvitationId() {
        return invitationId;
    }

    public void setInvitationId(Long invitationId) {
        this.invitationId = invitationId;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
