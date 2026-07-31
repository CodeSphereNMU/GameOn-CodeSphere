package com.codesphere.gameon.dto;

/**
 * Response DTO for a position.
 */
public class PositionDto {

    private long positionId;
    private String positionName;

    public PositionDto() {
    }

    public PositionDto(long positionId, String positionName) {
        this.positionId = positionId;
        this.positionName = positionName;
    }

    public long getPositionId() {
        return positionId;
    }

    public void setPositionId(long positionId) {
        this.positionId = positionId;
    }

    public String getPositionName() {
        return positionName;
    }

    public void setPositionName(String positionName) {
        this.positionName = positionName;
    }
}
