package com.gameon.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Position entity - Reference table of playing positions.
 * Maps to 'positions' table in GameOnDb.
 */
@Entity
@Table(name = "positions")
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "position_id")
    private Long positionId;

    @NotBlank(message = "Position name is required")
    @Column(name = "position_name", nullable = false, unique = true, length = 50)
    private String positionName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // ===== Relationships =====

    @OneToMany(mappedBy = "position", fetch = FetchType.LAZY)
    private List<FormatPosition> formatPositions = new ArrayList<>();

    // ===== Constructors =====

    public Position() {
    }

    public Position(String positionName) {
        this.positionName = positionName;
    }

    // ===== Getters and Setters =====

    public Long getPositionId() {
        return positionId;
    }

    public void setPositionId(Long positionId) {
        this.positionId = positionId;
    }

    public String getPositionName() {
        return positionName;
    }

    public void setPositionName(String positionName) {
        this.positionName = positionName;
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

    public List<FormatPosition> getFormatPositions() {
        return formatPositions;
    }

    public void setFormatPositions(List<FormatPosition> formatPositions) {
        this.formatPositions = formatPositions;
    }
}
