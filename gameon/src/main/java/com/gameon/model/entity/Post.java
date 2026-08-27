package com.gameon.model.entity;

import com.gameon.model.enums.PrivacySetting;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

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

    @Size(max = 500, message = "Content must be at most 500 characters")
    @Column(name = "content", length = 500)
    private String content;

    @Column(name = "image_path", length = 500)
    private String imagePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "privacy_setting", nullable = false, length = 10)
    private PrivacySetting privacySetting = PrivacySetting.PUBLIC;

    @Column(name = "is_removed", nullable = false)
    private Boolean isRemoved = false;

    @Column(name = "removed_by", length = 50)
    private String removedBy;

    @Column(name = "removed_at")
    private java.time.LocalDateTime removedAt;

    // ===== Relationships =====

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

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

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public Boolean getIsRemoved() {
        return isRemoved;
    }

    public void setIsRemoved(Boolean isRemoved) {
        this.isRemoved = isRemoved;
    }

    public String getRemovedBy() {
        return removedBy;
    }

    public void setRemovedBy(String removedBy) {
        this.removedBy = removedBy;
    }

    public java.time.LocalDateTime getRemovedAt() {
        return removedAt;
    }

    public void setRemovedAt(java.time.LocalDateTime removedAt) {
        this.removedAt = removedAt;
    }
}
