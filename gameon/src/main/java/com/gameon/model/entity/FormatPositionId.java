package com.gameon.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for FormatPosition entity.
 * Combines formatId and positionId.
 */
@Embeddable
public class FormatPositionId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "format_id")
    private Long formatId;

    @Column(name = "position_id")
    private Long positionId;

    public FormatPositionId() {
    }

    public FormatPositionId(Long formatId, Long positionId) {
        this.formatId = formatId;
        this.positionId = positionId;
    }

    public Long getFormatId() {
        return formatId;
    }

    public void setFormatId(Long formatId) {
        this.formatId = formatId;
    }

    public Long getPositionId() {
        return positionId;
    }

    public void setPositionId(Long positionId) {
        this.positionId = positionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FormatPositionId that = (FormatPositionId) o;
        return Objects.equals(formatId, that.formatId) && Objects.equals(positionId, that.positionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(formatId, positionId);
    }
}
