package com.gameon.service;

import com.gameon.exception.BusinessRuleException;
import com.gameon.exception.ResourceNotFoundException;
import com.gameon.exception.UnauthorizedAccessException;
import com.gameon.model.dto.PostFeedDto;
import com.gameon.model.dto.PostImageDto;
import com.gameon.model.entity.Post;
import com.gameon.model.entity.PostImage;
import com.gameon.model.entity.User;
import com.gameon.model.enums.PrivacySetting;
import com.gameon.repository.FollowRepository;
import com.gameon.repository.PostImageRepository;
import com.gameon.repository.PostRepository;
import com.gameon.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

/**
 * Service handling post creation and management.
 * Covers B100 (Create Posts), B200 (Manage Posts), B300 (Browse Posts).
 * BR4: A user can post many posts (no limit).
 */
@Service
public class PostService {

    private static final Logger logger = LoggerFactory.getLogger(PostService.class);

    /** Maximum number of images a single post may carry. */
    public static final int MAX_IMAGES_PER_POST = 4;

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final PostImageRepository postImageRepository;
    private final ImageStorageService imageStorageService;

    public PostService(PostRepository postRepository,
                       UserRepository userRepository,
                       FollowRepository followRepository,
                       PostImageRepository postImageRepository,
                       ImageStorageService imageStorageService) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.postImageRepository = postImageRepository;
        this.imageStorageService = imageStorageService;
    }

    /**
     * Creates a new text-only post (B100). BR4: No limit on number of posts.
     */
    @Transactional
    public Post createPost(Long userId, String content, PrivacySetting privacySetting) {
        return createPost(userId, content, privacySetting, Collections.emptyList());
    }

    /**
     * Creates a new post with optional images (B100).
     *
     * <p>A post is valid if it has non-blank text OR at least one valid image. Content is
     * normalised to an empty string for image-only posts so the NOT NULL column is honoured.
     * Up to {@link #MAX_IMAGES_PER_POST} images are accepted; each is validated and stored
     * by {@link ImageStorageService}. Images are assigned sequential display orders 1..N in
     * upload order.</p>
     */
    @Transactional
    public Post createPost(Long userId, String content, PrivacySetting privacySetting,
                           List<MultipartFile> images) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        List<MultipartFile> incoming = nonEmptyFiles(images);
        boolean hasText = content != null && !content.isBlank();
        if (!hasText && incoming.isEmpty()) {
            throw new BusinessRuleException("A post must contain text or at least one image.");
        }
        if (incoming.size() > MAX_IMAGES_PER_POST) {
            throw new BusinessRuleException("A post can have at most " + MAX_IMAGES_PER_POST + " images.");
        }

        Post post = new Post(user, hasText ? content : "", privacySetting);

        // Store files first; collect paths so we can clean up if a later one fails.
        List<String> storedPaths = new ArrayList<>();
        try {
            int order = 1;
            for (MultipartFile file : incoming) {
                String path = imageStorageService.store(file);
                storedPaths.add(path);
                post.addImage(new PostImage(post, path, order++));
            }
        } catch (RuntimeException ex) {
            storedPaths.forEach(imageStorageService::delete);
            throw ex;
        }

        Post saved = postRepository.save(post);
        logger.info("Post created: ID={} | User={} | Privacy={} | Images={}",
                saved.getPostId(), user.getUsername(), privacySetting, incoming.size());
        return saved;
    }

    /**
     * Updates a text/privacy-only post (B200). Only the post owner can edit.
     */
    @Transactional
    public Post updatePost(Long postId, Long userId, String content, PrivacySetting privacySetting) {
        return updatePost(postId, userId, content, privacySetting, Collections.emptyList(), Collections.emptyList());
    }

    /**
     * Updates a post including its images (B200). Only the post owner can edit.
     *
     * <p>Editing keeps images the user did not remove, deletes those explicitly removed
     * (both the DB record and the stored file), and appends any newly uploaded images.
     * The total is capped at {@link #MAX_IMAGES_PER_POST}. Display order is normalised to a
     * clean sequential 1..N afterwards: surviving images keep their relative order, new ones
     * are appended.</p>
     *
     * @param removeImagePaths public paths of existing images to remove (may be empty/null)
     * @param newImages        newly uploaded images to append (may be empty/null)
     */
    @Transactional
    public Post updatePost(Long postId, Long userId, String content, PrivacySetting privacySetting,
                           List<String> removeImagePaths, List<MultipartFile> newImages) {
        Post post = getPostById(postId);

        if (!post.getUser().getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("edit", "post");
        }

        List<String> toRemove = (removeImagePaths != null) ? removeImagePaths : Collections.emptyList();
        List<MultipartFile> incoming = nonEmptyFiles(newImages);

        // Determine which existing images survive vs. are removed.
        List<PostImage> surviving = new ArrayList<>();
        List<PostImage> removed = new ArrayList<>();
        for (PostImage img : post.getImages()) {
            if (toRemove.contains(img.getImagePath())) {
                removed.add(img);
            } else {
                surviving.add(img);
            }
        }

        int projectedTotal = surviving.size() + incoming.size();
        if (projectedTotal > MAX_IMAGES_PER_POST) {
            throw new BusinessRuleException("A post can have at most " + MAX_IMAGES_PER_POST + " images.");
        }

        boolean hasText = content != null && !content.isBlank();
        if (!hasText && projectedTotal == 0) {
            throw new BusinessRuleException("A post must contain text or at least one image.");
        }

        // Store any new files up front; roll back stored files if validation of a later file fails.
        List<String> newlyStored = new ArrayList<>();
        try {
            for (MultipartFile file : incoming) {
                newlyStored.add(imageStorageService.store(file));
            }
        } catch (RuntimeException ex) {
            newlyStored.forEach(imageStorageService::delete);
            throw ex;
        }

        // Detach removed images (orphanRemoval deletes their DB rows on flush).
        for (PostImage img : removed) {
            post.removeImage(img);
        }
        // Append the newly stored images.
        for (String path : newlyStored) {
            post.addImage(new PostImage(post, path, 0));
        }
        // Normalise display order to a clean 1..N (survivors keep relative order, new appended).
        normalizeDisplayOrder(post);

        post.setContent(hasText ? content : "");
        if (privacySetting != null) {
            post.setPrivacySetting(privacySetting);
        }

        Post saved = postRepository.save(post);

        // Files are removed only after a successful persist of the removals.
        for (PostImage img : removed) {
            imageStorageService.delete(img.getImagePath());
        }

        logger.info("Post {} updated by user {} | removed {} image(s), added {} image(s)",
                postId, userId, removed.size(), newlyStored.size());
        return saved;
    }

    /**
     * Reassigns sequential display orders (1..N) to a post's images, preserving their
     * current ordering (by existing displayOrder, then insertion order for new ones).
     */
    private void normalizeDisplayOrder(Post post) {
        List<PostImage> images = post.getImages();
        // Stable sort: existing images (order >= 1) first by their order, new ones (order 0) last.
        images.sort((a, b) -> {
            int oa = a.getDisplayOrder() <= 0 ? Integer.MAX_VALUE : a.getDisplayOrder();
            int ob = b.getDisplayOrder() <= 0 ? Integer.MAX_VALUE : b.getDisplayOrder();
            return Integer.compare(oa, ob);
        });
        int order = 1;
        for (PostImage img : images) {
            img.setDisplayOrder(order++);
        }
    }

    private List<MultipartFile> nonEmptyFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }
        List<MultipartFile> result = new ArrayList<>();
        for (MultipartFile f : files) {
            if (f != null && !f.isEmpty()) {
                result.add(f);
            }
        }
        return result;
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
     * Returns the ordered image URLs for a single post (used by the detail view).
     * Loaded via an explicit query rather than the lazy collection so it works with
     * open-in-view=false.
     */
    @Transactional(readOnly = true)
    public List<String> getImagePathsForPost(Long postId) {
        return postImageRepository.findByPostPostIdOrderByDisplayOrderAsc(postId).stream()
                .map(PostImage::getImagePath)
                .toList();
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
        Page<PostFeedDto> page;
        switch (filter.toUpperCase()) {
            case "PUBLIC":
                page = postRepository.findPublicPostDtos(pageable);
                break;

            case "FOLLOWERS":
                // Security: only show followers-only posts from users the current user actually follows
                List<Long> followedIdsForFilter = followRepository.findFollowingUserIds(userId);
                if (followedIdsForFilter.isEmpty()) {
                    return Page.empty(pageable);
                }
                page = postRepository.findFollowersOnlyPostDtos(followedIdsForFilter, pageable);
                break;

            case "MY_POSTS":
                page = postRepository.findMyPostDtos(userId, pageable);
                break;

            default: // "ALL" - all visible posts
                List<Long> followedIds = followRepository.findFollowingUserIds(userId);
                if (followedIds.isEmpty()) {
                    page = postRepository.findAllVisiblePostDtosNoFollows(userId, pageable);
                } else {
                    page = postRepository.findAllVisiblePostDtos(userId, followedIds, pageable);
                }
                break;
        }
        attachImages(page.getContent());
        return page;
    }

    /**
     * Attaches ordered image URLs to a page of feed DTOs using a single batch query
     * (one query for the whole page). Preserves the existing DTO/projection architecture
     * and avoids EAGER fetching, open-in-view, and per-post N+1 queries.
     */
    private void attachImages(List<PostFeedDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return;
        }
        List<Long> postIds = dtos.stream().map(PostFeedDto::postId).toList();
        Map<Long, List<String>> byPost = getImagePathsForPosts(postIds);
        if (byPost.isEmpty()) {
            return;
        }
        for (PostFeedDto dto : dtos) {
            List<String> paths = byPost.get(dto.postId());
            if (paths != null) {
                dto.setImagePaths(paths);
            }
        }
    }

    /**
     * Batch-loads ordered image URLs for a set of posts, keyed by post id. Uses the same
     * single-query batch approach as the Social Feed (no per-post N+1 queries, no EAGER
     * fetching, no open-in-view). Posts without images simply have no map entry. Ordering
     * within each list follows display_order. Intended for pages that render post entities
     * (e.g. the profile posts page) but still need images via the shared carousel.
     */
    @Transactional(readOnly = true)
    public Map<Long, List<String>> getImagePathsForPosts(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<PostImageDto> imageRows = postImageRepository.findImageDtosForPosts(postIds);
        // Grouped in query order (post_id ASC, display_order ASC), so paths land ordered.
        Map<Long, List<String>> byPost = new LinkedHashMap<>();
        for (PostImageDto row : imageRows) {
            byPost.computeIfAbsent(row.postId(), k -> new ArrayList<>()).add(row.imagePath());
        }
        return byPost;
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
