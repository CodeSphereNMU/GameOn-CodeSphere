package com.gameon.model.dto;

import com.gameon.model.enums.PrivacySetting;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO projection for the social feed.
 * Carries only the data needed to render a feed card, with like/comment counts
 * computed at the database level via COUNT subqueries. This eliminates
 * LazyInitializationException and N+1 query problems entirely.
 *
 * <p>Image paths are NOT part of the JPQL constructor projection (JPQL cannot build a
 * nested collection). They are batch-loaded separately for a page of posts and attached
 * via {@link #setImagePaths(List)}, keeping the existing projection architecture intact
 * and avoiding EAGER fetching / open-in-view.</p>
 */
public class PostFeedDto {

    private final Long postId;
    private final String content;
    private final PrivacySetting privacySetting;
    private final LocalDateTime createdAt;
    private final Long userId;
    private final String username;
    private final long likeCount;
    private final long commentCount;

    /** Ordered image URLs for this post's carousel; empty for text-only posts. */
    private List<String> imagePaths = new ArrayList<>();

    /**
     * Author's profile picture URL, attached separately (like {@link #imagePaths}) so the JPQL
     * projection constructor stays unchanged. {@code null} when the author has no picture; the
     * feed then renders the default avatar icon.
     */
    private String authorProfilePictureUrl;

    /**
     * Constructor used directly by the JPQL feed projections. The signature must stay
     * unchanged so existing repository queries keep compiling.
     */
    public PostFeedDto(Long postId, String content, PrivacySetting privacySetting,
                       LocalDateTime createdAt, Long userId, String username,
                       long likeCount, long commentCount) {
        this.postId = postId;
        this.content = content;
        this.privacySetting = privacySetting;
        this.createdAt = createdAt;
        this.userId = userId;
        this.username = username;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
    }

    public Long postId() {
        return postId;
    }

    public String content() {
        return content;
    }

    public PrivacySetting privacySetting() {
        return privacySetting;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public Long userId() {
        return userId;
    }

    public String username() {
        return username;
    }

    public long likeCount() {
        return likeCount;
    }

    public long commentCount() {
        return commentCount;
    }

    public List<String> getImagePaths() {
        return imagePaths;
    }

    public void setImagePaths(List<String> imagePaths) {
        this.imagePaths = (imagePaths != null) ? imagePaths : new ArrayList<>();
    }

    public String getAuthorProfilePictureUrl() {
        return authorProfilePictureUrl;
    }

    public void setAuthorProfilePictureUrl(String authorProfilePictureUrl) {
        this.authorProfilePictureUrl = authorProfilePictureUrl;
    }

    /** Convenience for templates: whether this post has any attached images. */
    public boolean isHasImages() {
        return imagePaths != null && !imagePaths.isEmpty();
    }

    /** Convenience for templates: number of attached images. */
    public int getImageCount() {
        return imagePaths == null ? 0 : imagePaths.size();
    }
}
