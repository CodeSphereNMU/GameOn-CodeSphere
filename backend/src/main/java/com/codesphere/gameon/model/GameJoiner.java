package com.codesphere.gameon.model;

/**
 * Domain model for the dbo.game_joiner table.
 */
public class GameJoiner {

    private long gameListingId;
    private long userId;
    private String team;
    private String status;
    private Long positionId;       // nullable
    private Long formatId;         // nullable
    private String alternateFormatPosition; // nullable — second preferred position

    public GameJoiner() {
    }

    public GameJoiner(long gameListingId, long userId, String team, String status,
                      Long positionId, Long formatId, String alternateFormatPosition) {
        this.gameListingId = gameListingId;
        this.userId = userId;
        this.team = team;
        this.status = status;
        this.positionId = positionId;
        this.formatId = formatId;
        this.alternateFormatPosition = alternateFormatPosition;
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

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getPositionId() {
        return positionId;
    }

    public void setPositionId(Long positionId) {
        this.positionId = positionId;
    }

    public Long getFormatId() {
        return formatId;
    }

    public void setFormatId(Long formatId) {
        this.formatId = formatId;
    }

    public String getAlternateFormatPosition() {
        return alternateFormatPosition;
    }

    public void setAlternateFormatPosition(String alternateFormatPosition) {
        this.alternateFormatPosition = alternateFormatPosition;
    }
}
