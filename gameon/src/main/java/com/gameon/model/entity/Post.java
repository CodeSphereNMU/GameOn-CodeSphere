package com.gameon.model.entity;

import com.gameon.model.enums.PrivacySetting;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

/**
 * Post entity - Social content posted by users.
 * Maps to 'posts' table in GameOnDb.
 */
@Entity
@Table(name = "posts")
public class Post extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long postId;

    @NotBlank(message = "Content is required")
    @Size(max = 500, message = "Content must be at most 500 characters")
    @Column(name = "content", nullable = false, length = 500)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "privacy_setting", nullable = false, length = 10)
    private PrivacySetting privacySetting = PrivacySetting.PUBLIC;

    // ===== Relationships =====

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "removed_at")
    private LocalDateTime removedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "removed_by_user_id")
    private User removedBy;

    @OneToMany(mappedBy = "post", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "post", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Like> likes = new ArrayList<>();

    // ===== Constructors =====

    public Post() {
    }

    public Post(User user, String content, PrivacySetting privacySetting) {
        this.user = user;
        this.content = content;
        this.privacySetting = privacySetting;
    }

    // ===== Getters and Setters =====

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public PrivacySetting getPrivacySetting() {
        return privacySetting;
    }

    public void setPrivacySetting(PrivacySetting privacySetting) {
        this.privacySetting = privacySetting;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getRemovedAt() { return removedAt; }
    public void setRemovedAt(LocalDateTime removedAt) { this.removedAt = removedAt; }
    public User getRemovedBy() { return removedBy; }
    public void setRemovedBy(User removedBy) { this.removedBy = removedBy; }

    @Transient
    public boolean isRemoved() { return removedAt != null; }

    @Transient
    public boolean isRemovedByAuthor() { return removedAt != null && removedBy == null; }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public List<Like> getLikes() {
        return likes;
    }

    public void setLikes(List<Like> likes) {
        this.likes = likes;
    }
}
