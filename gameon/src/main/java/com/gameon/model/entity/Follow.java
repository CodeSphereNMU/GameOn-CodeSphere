package com.gameon.model.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Follow entity - Social graph tracking who follows whom.
 * Maps to 'follows' table in GameOnDb.
 * Composite PK: (followerUserId, followedUserId)
 * Self-referencing many-to-many on User.
 */
@Entity
@Table(name = "follows")
public class Follow {

    @EmbeddedId
    private FollowId id;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("followerUserId")
    @JoinColumn(name = "follower_user_id")
    private User follower;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("followedUserId")
    @JoinColumn(name = "followed_user_id")
    private User followed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // ===== Constructors =====

    public Follow() {
    }

    public Follow(User follower, User followed) {
        this.id = new FollowId(follower.getUserId(), followed.getUserId());
        this.follower = follower;
        this.followed = followed;
    }

    // ===== Getters and Setters =====

    public FollowId getId() {
        return id;
    }

    public void setId(FollowId id) {
        this.id = id;
    }

    public User getFollower() {
        return follower;
    }

    public void setFollower(User follower) {
        this.follower = follower;
    }

    public User getFollowed() {
        return followed;
    }

    public void setFollowed(User followed) {
        this.followed = followed;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
