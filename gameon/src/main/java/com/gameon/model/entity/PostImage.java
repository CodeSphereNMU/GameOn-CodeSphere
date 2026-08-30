package com.gameon.model.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * PostImage entity - an image attached to a Social Feed {@link Post}.
 * Maps to the 'post_images' table.
 *
 * <p>A post can carry zero to four images. Only a server-controlled file path
 * (reference) is stored here; the uploaded image file lives on the filesystem
 * under the configured uploads directory. {@code displayOrder} preserves the
 * carousel ordering and is kept sequential (1..N) by the service layer.</p>
 */
@Entity
@Table(name = "post_images")
public class PostImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_image_id")
    private Long postImageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    /** Public URL/reference path to the stored image, e.g. /uploads/posts/ab12cd34.jpg */
    @Column(name = "image_path", nullable = false, length = 255)
    private String imagePath;

    /** 1-based ordering used to render the carousel; first uploaded image is 1. */
    @Column(name = "display_order", nullable = false)
    private int displayOrder = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // ===== Constructors =====

    public PostImage() {
    }

    public PostImage(Post post, String imagePath, int displayOrder) {
        this.post = post;
        this.imagePath = imagePath;
        this.displayOrder = displayOrder;
    }

    // ===== Getters and Setters =====

    public Long getPostImageId() {
        return postImageId;
    }

    public void setPostImageId(Long postImageId) {
        this.postImageId = postImageId;
    }

    public Post getPost() {
        return post;
    }

    public void setPost(Post post) {
        this.post = post;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
