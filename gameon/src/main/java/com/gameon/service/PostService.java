package com.gameon.service;

import com.gameon.exception.ResourceNotFoundException;
import com.gameon.exception.UnauthorizedAccessException;
import com.gameon.model.dto.PostFeedDto;
import com.gameon.model.entity.Post;
import com.gameon.model.entity.User;
import com.gameon.model.enums.PrivacySetting;
import com.gameon.repository.FollowRepository;
import com.gameon.repository.PostRepository;
import com.gameon.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Service handling post creation and management.
 * Covers B100 (Create Posts), B200 (Manage Posts), B300 (Browse Posts).
 * BR4: A user can post many posts (no limit).
 */
@Service
public class PostService {

    private static final Logger logger = LoggerFactory.getLogger(PostService.class);

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final ImageStorageService imageStorageService;

    public PostService(PostRepository postRepository,
                       UserRepository userRepository,
                       FollowRepository followRepository,
                       ImageStorageService imageStorageService) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.imageStorageService = imageStorageService;
    }

    /**
     * Creates a new post (B100).
     * BR4: No limit on number of posts.
     * Supports: text only, image only, or text + image.
     */
    @Transactional
    public Post createPost(Long userId, String content, PrivacySetting privacySetting) {
        return createPost(userId, content, privacySetting, null);
    }

    /**
     * Creates a new post with optional image (B100).
     * Rejects post only if BOTH text and image are empty.
     */
    @Transactional
    public Post createPost(Long userId, String content, PrivacySetting privacySetting, MultipartFile image) {
        boolean hasContent = content != null && !content.isBlank();
        boolean hasImage = image != null && !image.isEmpty();

        if (!hasContent && !hasImage) {
            throw new IllegalArgumentException("Post must have either text content or an image.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Post post = new Post(user, hasContent ? content : null, privacySetting);

        // Handle image upload
        if (hasImage) {
            try {
                String imagePath = imageStorageService.storeImage(image);
                post.setImagePath(imagePath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to upload image: " + e.getMessage(), e);
            }
        }

        Post saved = postRepository.save(post);
        logger.info("Post created: ID={} | User={} | Privacy={} | HasImage={}",
                saved.getPostId(), user.getUsername(), privacySetting, hasImage);
        return saved;
    }

    /**
     * Updates a post (B200). Only the post owner can edit.
     */
    @Transactional
    public Post updatePost(Long postId, Long userId, String content, PrivacySetting privacySetting) {
        return updatePost(postId, userId, content, privacySetting, null, false);
    }

    /**
     * Updates a post with image handling (B200). Only the post owner can edit.
     * Supports: keep existing image, replace image, or remove image.
     *
     * @param removeImage if true, removes the existing image without replacement
     */
    @Transactional
    public Post updatePost(Long postId, Long userId, String content, PrivacySetting privacySetting,
                           MultipartFile image, boolean removeImage) {
        Post post = getPostById(postId);

        if (!post.getUser().getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("edit", "post");
        }

        boolean hasContent = content != null && !content.isBlank();
        boolean hasNewImage = image != null && !image.isEmpty();
        boolean willHaveImage = hasNewImage || (!removeImage && post.getImagePath() != null);

        // Reject if both text and image would be empty after edit
        if (!hasContent && !willHaveImage) {
            throw new IllegalArgumentException("Post must have either text content or an image.");
        }

        post.setContent(hasContent ? content : null);
        if (privacySetting != null) post.setPrivacySetting(privacySetting);

        // Handle image changes
        if (hasNewImage) {
            // Replace: delete old image, store new one
            if (post.getImagePath() != null) {
                imageStorageService.deleteImage(post.getImagePath());
            }
            try {
                String imagePath = imageStorageService.storeImage(image);
                post.setImagePath(imagePath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to upload image: " + e.getMessage(), e);
            }
        } else if (removeImage && post.getImagePath() != null) {
            // Remove existing image
            imageStorageService.deleteImage(post.getImagePath());
            post.setImagePath(null);
        }

        logger.info("Post {} updated by user {}", postId, userId);
        return postRepository.save(post);
    }

    /**
     * Soft-deletes a post (B200). Only the post owner can delete.
     * Image is RETAINED for evidence. Post is marked as removed.
     * Comments and likes remain for audit trail.
     */
    @Transactional
    public void deletePost(Long postId, Long userId) {
        Post post = getPostById(postId);

        if (!post.getUser().getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("delete", "post");
        }

        // Soft-delete: mark as removed, retain image
        post.setIsRemoved(true);
        post.setRemovedBy(post.getUser().getUsername());
        post.setRemovedAt(java.time.LocalDateTime.now());
        postRepository.save(post);
        logger.info("Post {} soft-deleted by author {}", postId, userId);
    }

    /**
     * Soft-deletes a post by moderator (B400).
     * Image is RETAINED for moderation evidence.
     */
    @Transactional
    public void deletePostAsModerator(Long postId) {
        Post post = getPostById(postId);

        // Soft-delete: mark as removed, retain image for evidence
        post.setIsRemoved(true);
        post.setRemovedBy("MODERATOR");
        post.setRemovedAt(java.time.LocalDateTime.now());
        postRepository.save(post);
        logger.info("Post {} soft-deleted by moderator", postId);
    }

    /**
     * Hard-deletes a post permanently, removing the image file from disk.
     * This is the only path that removes image evidence.
     * Should only be used for GDPR-type requests or data cleanup.
     */
    @Transactional
    public void hardDeletePost(Long postId) {
        Post post = getPostById(postId);

        // Delete image file from disk if present
        if (post.getImagePath() != null) {
            imageStorageService.deleteImage(post.getImagePath());
        }

        postRepository.delete(post);
        logger.info("Post {} hard-deleted (image removed from disk)", postId);
    }

    @Transactional(readOnly = true)
    public Post getPostById(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", postId));
    }

    @Transactional(readOnly = true)
    public Post getPostWithUser(Long postId) {
        return postRepository.findByIdWithUser(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", postId));
    }

    /**
     * Gets the social feed for a user (B300) as DTO projections.
     * Shows: all public posts + followers-only posts from users they follow.
     * Like and comment counts are computed at the database level via COUNT subqueries,
     * eliminating any LazyInitializationException risk.
     */
    @Transactional(readOnly = true)
    public Page<PostFeedDto> getFeed(Long userId, Pageable pageable) {
        return getFilteredFeed(userId, "ALL", pageable);
    }

    /**
     * Gets a filtered social feed for a user (B300) as DTO projections.
     * Filter options:
     *   ALL       - public posts + followers-only from followed users + own posts
     *   PUBLIC    - only public posts
     *   FOLLOWERS - only followers-only posts from users the current user follows
     *   MY_POSTS  - all posts by the current user regardless of privacy
     *
     * Privacy enforcement:
     *   - Public posts are always visible to everyone.
     *   - Followers-only posts are only visible to followers and the post owner.
     *   - Users cannot access followers-only posts via URL manipulation.
     */
    @Transactional(readOnly = true)
    public Page<PostFeedDto> getFilteredFeed(Long userId, String filter, Pageable pageable) {
        switch (filter.toUpperCase()) {
            case "PUBLIC":
                return postRepository.findPublicPostDtos(pageable);

            case "FOLLOWERS":
                // Security: only show followers-only posts from users the current user actually follows
                List<Long> followedIdsForFilter = followRepository.findFollowingUserIds(userId);
                if (followedIdsForFilter.isEmpty()) {
                    return Page.empty(pageable);
                }
                return postRepository.findFollowersOnlyPostDtos(followedIdsForFilter, pageable);

            case "MY_POSTS":
                return postRepository.findMyPostDtos(userId, pageable);

            default: // "ALL" - all visible posts
                List<Long> followedIds = followRepository.findFollowingUserIds(userId);
                if (followedIds.isEmpty()) {
                    return postRepository.findAllVisiblePostDtosNoFollows(userId, pageable);
                }
                return postRepository.findAllVisiblePostDtos(userId, followedIds, pageable);
        }
    }

    /**
     * Gets posts by a specific user (for profile view).
     * Excludes soft-deleted posts from normal view.
     */
    @Transactional(readOnly = true)
    public Page<Post> getPostsByUser(Long userId, Pageable pageable) {
        return postRepository.findByUserUserIdAndIsRemovedFalseOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * Gets posts visible to a viewer from a specific user's profile.
     * If viewer follows the user, shows PUBLIC + FOLLOWERS posts.
     * Otherwise, shows only PUBLIC posts.
     * Excludes soft-deleted posts.
     */
    @Transactional(readOnly = true)
    public Page<Post> getVisiblePostsByUser(Long profileUserId, Long viewerId, Pageable pageable) {
        if (profileUserId.equals(viewerId)) {
            return postRepository.findByUserUserIdAndIsRemovedFalseOrderByCreatedAtDesc(profileUserId, pageable);
        }

        boolean isFollowing = followRepository.existsByIdFollowerUserIdAndIdFollowedUserId(viewerId, profileUserId);
        if (isFollowing) {
            return postRepository.findByUserUserIdAndIsRemovedFalseOrderByCreatedAtDesc(profileUserId, pageable);
        }

        return postRepository.findByUserUserIdAndIsRemovedFalseOrderByCreatedAtDesc(profileUserId, pageable);
    }
}
