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
     * Deletes a post (B200). Only the post owner can delete.
     * Comments and likes are cascade-deleted.
     */
    @Transactional
    public void deletePost(Long postId, Long userId) {
        Post post = getPostById(postId);

        if (!post.getUser().getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("delete", "post");
        }

        postRepository.delete(post);
        logger.info("Post {} deleted by user {}", postId, userId);
    }

    /**
     * Deletes a post by moderator (B400).
     */
    @Transactional
    public void deletePostAsModerator(Long postId) {
        Post post = getPostById(postId);
        postRepository.delete(post);
        logger.info("Post {} removed by moderator", postId);
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
        List<Long> followedIds = followRepository.findFollowingUserIds(userId);

        if (followedIds.isEmpty()) {
            // Only show public posts
            return postRepository.findPublicFeedPostDtos(pageable);
        }

        // All active users' public posts are visible, plus followers-only from followed users
        List<Long> allVisibleUserIds = userRepository.findByIsActiveTrue().stream()
                .map(User::getUserId)
                .toList();

        return postRepository.findFeedPostDtos(allVisibleUserIds, followedIds, pageable);
    }

    /**
     * Gets posts by a specific user (for profile view).
     */
    @Transactional(readOnly = true)
    public Page<Post> getPostsByUser(Long userId, Pageable pageable) {
        return postRepository.findByUserUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * Gets posts visible to a viewer from a specific user's profile.
     * If viewer follows the user, shows PUBLIC + FOLLOWERS posts.
     * Otherwise, shows only PUBLIC posts.
     */
    @Transactional(readOnly = true)
    public Page<Post> getVisiblePostsByUser(Long profileUserId, Long viewerId, Pageable pageable) {
        if (profileUserId.equals(viewerId)) {
            return postRepository.findByUserUserIdOrderByCreatedAtDesc(profileUserId, pageable);
        }

        boolean isFollowing = followRepository.existsByIdFollowerUserIdAndIdFollowedUserId(viewerId, profileUserId);
        if (isFollowing) {
            return postRepository.findByUserUserIdOrderByCreatedAtDesc(profileUserId, pageable);
        }

        return postRepository.findByUserUserIdOrderByCreatedAtDesc(profileUserId, pageable);
    }
}
