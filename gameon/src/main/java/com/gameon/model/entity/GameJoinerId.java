package com.gameon.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for GameJoiner entity.
 * Combines userId and gameListingId.
 */
@Embeddable
public class GameJoinerId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "game_listing_id")
    private Long gameListingId;

    public GameJoinerId() {
    }

    public GameJoinerId(Long userId, Long gameListingId) {
        this.userId = userId;
        this.gameListingId = gameListingId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getGameListingId() {
        return gameListingId;
    }

    public void setGameListingId(Long gameListingId) {
        this.gameListingId = gameListingId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameJoinerId that = (GameJoinerId) o;
        return Objects.equals(userId, that.userId) && Objects.equals(gameListingId, that.gameListingId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, gameListingId);
    }
}
