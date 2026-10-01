package com.gameon.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for {@link Block}.
 * A block is directional in storage: (blockerUserId, blockedUserId).
 * Mutual visibility/interaction is enforced in the service layer by checking both directions.
 */
@Embeddable
public class BlockId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "blocker_user_id")
    private Long blockerUserId;

    @Column(name = "blocked_user_id")
    private Long blockedUserId;

    public BlockId() {
    }

    public BlockId(Long blockerUserId, Long blockedUserId) {
        this.blockerUserId = blockerUserId;
        this.blockedUserId = blockedUserId;
    }

    public Long getBlockerUserId() {
        return blockerUserId;
    }

    public void setBlockerUserId(Long blockerUserId) {
        this.blockerUserId = blockerUserId;
    }

    public Long getBlockedUserId() {
        return blockedUserId;
    }

    public void setBlockedUserId(Long blockedUserId) {
        this.blockedUserId = blockedUserId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockId that = (BlockId) o;
        return Objects.equals(blockerUserId, that.blockerUserId)
                && Objects.equals(blockedUserId, that.blockedUserId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blockerUserId, blockedUserId);
    }
}
