package com.gameon.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for UserSportProfile entity.
 * Combines userId and sportId.
 */
@Embeddable
public class UserSportProfileId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "sport_id")
    private Long sportId;

    public UserSportProfileId() {
    }

    public UserSportProfileId(Long userId, Long sportId) {
        this.userId = userId;
        this.sportId = sportId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getSportId() {
        return sportId;
    }

    public void setSportId(Long sportId) {
        this.sportId = sportId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserSportProfileId that = (UserSportProfileId) o;
        return Objects.equals(userId, that.userId) && Objects.equals(sportId, that.sportId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, sportId);
    }
}
