package com.gameon.model.entity;

import jakarta.persistence.*;

/**
 * FormatPosition entity - Junction table mapping positions to sport formats.
 * Maps to 'format_positions' table in GameOnDb.
 * Composite PK: (formatId, positionId)
 */
@Entity
@Table(name = "format_positions")
public class FormatPosition {

    @EmbeddedId
    private FormatPositionId id;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("formatId")
    @JoinColumn(name = "format_id")
    private SportFormat format;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("positionId")
    @JoinColumn(name = "position_id")
    private Position position;

    // ===== Constructors =====

    public FormatPosition() {
    }

    public FormatPosition(SportFormat format, Position position) {
        this.id = new FormatPositionId(format.getFormatId(), position.getPositionId());
        this.format = format;
        this.position = position;
    }

    // ===== Getters and Setters =====

    public FormatPositionId getId() {
        return id;
    }

    public void setId(FormatPositionId id) {
        this.id = id;
    }

    public SportFormat getFormat() {
        return format;
    }

    public void setFormat(SportFormat format) {
        this.format = format;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }
}
