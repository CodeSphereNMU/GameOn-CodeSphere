package com.gameon.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for Follow entity.
 * Combines followerUserId and followedUserId.
 */
@Embeddable
public class FollowId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "follower_user_id")
    private Long followerUserId;

    @Column(name = "followed_user_id")
    private Long followedUserId;

    public FollowId() {
    }

    public FollowId(Long followerUserId, Long followedUserId) {
        this.followerUserId = followerUserId;
        this.followedUserId = followedUserId;
    }

    public Long getFollowerUserId() {
        return followerUserId;
    }

    public void setFollowerUserId(Long followerUserId) {
        this.followerUserId = followerUserId;
    }

    public Long getFollowedUserId() {
        return followedUserId;
    }

    public void setFollowedUserId(Long followedUserId) {
        this.followedUserId = followedUserId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FollowId that = (FollowId) o;
        return Objects.equals(followerUserId, that.followerUserId) && Objects.equals(followedUserId, that.followedUserId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(followerUserId, followedUserId);
    }
}
