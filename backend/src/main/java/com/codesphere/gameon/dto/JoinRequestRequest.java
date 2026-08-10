package com.codesphere.gameon.dto;

/**
 * Request DTO for POST /api/game-listings/{id}/join-requests.
 */
public class JoinRequestRequest {

    private String team;
    private boolean anyPosition;
    private Long positionId;
    private Long alternatePositionId;

    public JoinRequestRequest() {
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public boolean isAnyPosition() {
        return anyPosition;
    }

    public void setAnyPosition(boolean anyPosition) {
        this.anyPosition = anyPosition;
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
}
