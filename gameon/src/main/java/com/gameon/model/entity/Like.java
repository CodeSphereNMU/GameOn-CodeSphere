package com.gameon.model.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Like entity - Tracks which users liked which posts.
 * Maps to 'likes' table in GameOnDb.
 * Composite PK: (userId, postId)
 */
@Entity
@Table(name = "likes")
public class Like {

    @EmbeddedId
    private LikeId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("postId")
    @JoinColumn(name = "post_id")
    private Post post;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // ===== Constructors =====

    public Like() {
    }

    public Like(User user, Post post) {
        this.id = new LikeId(user.getUserId(), post.getPostId());
        this.user = user;
        this.post = post;
    }

    // ===== Getters and Setters =====

    public LikeId getId() {
        return id;
    }

    public void setId(LikeId id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Post getPost() {
        return post;
    }

    public void setPost(Post post) {
        this.post = post;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
