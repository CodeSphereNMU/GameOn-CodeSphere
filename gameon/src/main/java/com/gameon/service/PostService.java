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

import java.util.List;
import java.time.LocalDateTime;

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

    public PostService(PostRepository postRepository,
                       UserRepository userRepository,
                       FollowRepository followRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.followRepository = followRepository;
    }

    /**
     * Creates a new post (B100).
     * BR4: No limit on number of posts.
     */
    @Transactional
    public Post createPost(Long userId, String content, PrivacySetting privacySetting) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Post post = new Post(user, content, privacySetting);
        Post saved = postRepository.save(post);
        logger.info("Post created: ID={} | User={} | Privacy={}", saved.getPostId(), user.getUsername(), privacySetting);
        return saved;
    }

    /**
     * Updates a post (B200). Only the post owner can edit.
     */
    @Transactional
    public Post updatePost(Long postId, Long userId, String content, PrivacySetting privacySetting) {
        Post post = getPostById(postId);

        if (!post.getUser().getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("edit", "post");
        }

        if (content != null) post.setContent(content);
        if (privacySetting != null) post.setPrivacySetting(privacySetting);

        logger.info("Post {} updated by user {}", postId, userId);
        return postRepository.save(post);
    }

    /**
     * Soft-removes a post (B200). The post, comments, likes, and reports remain stored.
     */
    @Transactional
    public void deletePost(Long postId, Long userId) {
        Post post = getPostById(postId);

        if (!post.getUser().getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("delete", "post");
        }

        post.setRemovedAt(LocalDateTime.now());
        post.setRemovedBy(null);
        postRepository.save(post);
        logger.info("Post {} soft-removed by author {}", postId, userId);
    }

    /**
     * Soft-removes an active post by moderator. Existing removal attribution is preserved.
     */
    @Transactional
    public void deletePostAsModerator(Long postId, Long moderatorId) {
        Post post = getPostForModeration(postId);
        if (!post.isRemoved()) {
            User moderator = userRepository.findById(moderatorId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", moderatorId));
            post.setRemovedAt(LocalDateTime.now());
            post.setRemovedBy(moderator);
            postRepository.save(post);
        }
        logger.info("Post {} reviewed for removal by moderator {}", postId, moderatorId);
    }

    @Transactional(readOnly = true)
    public Post getPostById(Long postId) {
        return postRepository.findByPostIdAndRemovedAtIsNull(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", postId));
    }

    @Transactional(readOnly = true)
    public Post getPostWithUser(Long postId) {
        return postRepository.findActiveByIdWithUser(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", postId));
    }

    @Transactional(readOnly = true)
    public Post getPostForModeration(Long postId) {
        return postRepository.findByIdWithUserForModeration(postId)
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
     */
    @Transactional(readOnly = true)
    public Page<Post> getPostsByUser(Long userId, Pageable pageable) {
        return postRepository.findByUserUserIdAndRemovedAtIsNullOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * Gets posts visible to a viewer from a specific user's profile.
     * If viewer follows the user, shows PUBLIC + FOLLOWERS posts.
     * Otherwise, shows only PUBLIC posts.
     */
    @Transactional(readOnly = true)
    public Page<Post> getVisiblePostsByUser(Long profileUserId, Long viewerId, Pageable pageable) {
        if (profileUserId.equals(viewerId)) {
            return postRepository.findByUserUserIdAndRemovedAtIsNullOrderByCreatedAtDesc(profileUserId, pageable);
        }

        boolean isFollowing = followRepository.existsByIdFollowerUserIdAndIdFollowedUserId(viewerId, profileUserId);
        if (isFollowing) {
            return postRepository.findByUserUserIdAndRemovedAtIsNullOrderByCreatedAtDesc(profileUserId, pageable);
        }

        return postRepository.findByUserUserIdAndRemovedAtIsNullOrderByCreatedAtDesc(profileUserId, pageable);
    }
}
