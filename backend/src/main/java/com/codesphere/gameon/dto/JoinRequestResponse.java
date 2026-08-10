package com.codesphere.gameon.dto;

/**
 * Response DTO for POST /api/game-listings/{id}/join-requests.
 */
public class JoinRequestResponse {

    private long joinRequestId;
    private long gameListingId;
    private String team;
    private Long positionId;
    private Long alternatePositionId;
    private String status;
    private boolean invitationLinked;

    public JoinRequestResponse() {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isInvitationLinked() {
        return invitationLinked;
    }

    public void setInvitationLinked(boolean invitationLinked) {
        this.invitationLinked = invitationLinked;
    }
}
