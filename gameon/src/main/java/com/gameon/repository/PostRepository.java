package com.gameon.repository;

import com.gameon.model.dto.PostFeedDto;
import com.gameon.model.entity.Post;
import com.gameon.model.enums.PrivacySetting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Post p " +
           "WHERE p.postId = :postId AND p.removedAt IS NULL AND (p.user.userId = :viewerId " +
           "OR p.privacySetting = 'PUBLIC' OR (p.privacySetting = 'FOLLOWERS' AND EXISTS " +
           "(SELECT f FROM Follow f WHERE f.follower.userId = :viewerId AND f.followed.userId = p.user.userId)))")
    boolean existsVisibleToUser(@Param("postId") Long postId, @Param("viewerId") Long viewerId);

    List<Post> findByUserUserIdAndRemovedAtIsNull(Long userId);

    Page<Post> findByUserUserIdAndRemovedAtIsNullOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT p FROM Post p JOIN FETCH p.user WHERE p.postId = :postId AND p.removedAt IS NULL")
    Optional<Post> findActiveByIdWithUser(@Param("postId") Long postId);

    @Query("SELECT p FROM Post p JOIN FETCH p.user LEFT JOIN FETCH p.removedBy " +
           "WHERE p.postId = :postId")
    Optional<Post> findByIdWithUserForModeration(@Param("postId") Long postId);

    Optional<Post> findByPostIdAndRemovedAtIsNull(Long postId);

    // ===== Feed DTO projections (counts computed at DB level) =====

    /**
     * Social feed with DTO projection: public posts + followers-only from followed users.
     * Computes like/comment counts via subqueries — no lazy collections touched.
     */
    @Query("SELECT new com.gameon.model.dto.PostFeedDto(" +
           "p.postId, p.content, p.privacySetting, p.createdAt, " +
           "p.user.userId, p.user.username, " +
           "(SELECT COUNT(l) FROM Like l WHERE l.post = p), " +
           "(SELECT COUNT(c) FROM Comment c WHERE c.post = p)) " +
           "FROM Post p " +
           "WHERE p.removedAt IS NULL AND ((p.privacySetting = 'PUBLIC' AND p.user.userId IN :visibleUserIds) " +
           "OR (p.privacySetting = 'FOLLOWERS' AND p.user.userId IN :followedUserIds)) " +
           "ORDER BY p.createdAt DESC")
    Page<PostFeedDto> findFeedPostDtos(
            @Param("visibleUserIds") List<Long> visibleUserIds,
            @Param("followedUserIds") List<Long> followedUserIds,
            Pageable pageable);

    /**
     * Public-only feed with DTO projection (used when user follows nobody).
     */
    @Query("SELECT new com.gameon.model.dto.PostFeedDto(" +
           "p.postId, p.content, p.privacySetting, p.createdAt, " +
           "p.user.userId, p.user.username, " +
           "(SELECT COUNT(l) FROM Like l WHERE l.post = p), " +
           "(SELECT COUNT(c) FROM Comment c WHERE c.post = p)) " +
           "FROM Post p " +
           "WHERE p.removedAt IS NULL AND p.privacySetting = 'PUBLIC' " +
           "ORDER BY p.createdAt DESC")
    Page<PostFeedDto> findPublicFeedPostDtos(Pageable pageable);

    // ===== Legacy entity queries (used by edit/delete/detail flows) =====

    @Query("SELECT p FROM Post p " +
           "WHERE p.removedAt IS NULL AND ((p.privacySetting = 'PUBLIC' AND p.user.userId IN :visibleUserIds) " +
           "OR (p.privacySetting = 'FOLLOWERS' AND p.user.userId IN :followedUserIds)) " +
           "ORDER BY p.createdAt DESC")
    Page<Post> findFeedPosts(
            @Param("visibleUserIds") List<Long> visibleUserIds,
            @Param("followedUserIds") List<Long> followedUserIds,
            Pageable pageable);

    Page<Post> findByPrivacySettingAndRemovedAtIsNullOrderByCreatedAtDesc(
            PrivacySetting privacySetting, Pageable pageable);

    // Count posts by user
    long countByUserUserIdAndRemovedAtIsNull(Long userId);

    // ===== Filtered Feed DTO projections =====

    /**
     * All visible posts authored by users the current user follows (filter: FOLLOWING).
     * Returns both PUBLIC and FOLLOWERS posts, but only from followed authors; the current
     * user's own posts are excluded (a user does not follow themselves).
     *
     * <p>Privacy is preserved intrinsically: a FOLLOWERS post is only ever returned when its
     * author is in {@code followedUserIds}, i.e. the viewer follows them. Posts from non-followed
     * users are never returned, regardless of privacy.</p>
     */
    @Query("SELECT new com.gameon.model.dto.PostFeedDto(" +
           "p.postId, p.content, p.privacySetting, p.createdAt, " +
           "p.user.userId, p.user.username, " +
           "(SELECT COUNT(l) FROM Like l WHERE l.post = p), " +
           "(SELECT COUNT(c) FROM Comment c WHERE c.post = p)) " +
           "FROM Post p " +
           "WHERE p.removedAt IS NULL AND p.user.userId IN :followedUserIds " +
           "ORDER BY p.createdAt DESC")
    Page<PostFeedDto> findFollowingPostDtos(
            @Param("followedUserIds") List<Long> followedUserIds,
            Pageable pageable);

    /**
     * All posts by a specific user regardless of privacy (filter: MY_POSTS).
     */
    @Query("SELECT new com.gameon.model.dto.PostFeedDto(" +
           "p.postId, p.content, p.privacySetting, p.createdAt, " +
           "p.user.userId, p.user.username, " +
           "(SELECT COUNT(l) FROM Like l WHERE l.post = p), " +
           "(SELECT COUNT(c) FROM Comment c WHERE c.post = p)) " +
           "FROM Post p " +
           "WHERE p.removedAt IS NULL AND p.user.userId = :userId " +
           "ORDER BY p.createdAt DESC")
    Page<PostFeedDto> findMyPostDtos(@Param("userId") Long userId, Pageable pageable);

    /**
     * Full visible feed including user's own posts (filter: ALL).
     * Shows: public posts + followers-only from followed users + all own posts.
     */
    @Query("SELECT new com.gameon.model.dto.PostFeedDto(" +
           "p.postId, p.content, p.privacySetting, p.createdAt, " +
           "p.user.userId, p.user.username, " +
           "(SELECT COUNT(l) FROM Like l WHERE l.post = p), " +
           "(SELECT COUNT(c) FROM Comment c WHERE c.post = p)) " +
           "FROM Post p " +
           "WHERE p.removedAt IS NULL AND (p.privacySetting = 'PUBLIC' " +
           "OR (p.privacySetting = 'FOLLOWERS' AND p.user.userId IN :followedUserIds) " +
           "OR p.user.userId = :userId) " +
           "ORDER BY p.createdAt DESC")
    Page<PostFeedDto> findAllVisiblePostDtos(
            @Param("userId") Long userId,
            @Param("followedUserIds") List<Long> followedUserIds,
            Pageable pageable);

    /**
     * Full visible feed when user follows nobody (filter: ALL, no follows).
     * Shows: public posts + own posts.
     */
    @Query("SELECT new com.gameon.model.dto.PostFeedDto(" +
           "p.postId, p.content, p.privacySetting, p.createdAt, " +
           "p.user.userId, p.user.username, " +
           "(SELECT COUNT(l) FROM Like l WHERE l.post = p), " +
           "(SELECT COUNT(c) FROM Comment c WHERE c.post = p)) " +
           "FROM Post p " +
           "WHERE p.removedAt IS NULL AND (p.privacySetting = 'PUBLIC' " +
           "OR p.user.userId = :userId) " +
           "ORDER BY p.createdAt DESC")
    Page<PostFeedDto> findAllVisiblePostDtosNoFollows(@Param("userId") Long userId, Pageable pageable);

    // ===== Mutual blocking: block-filtered feed variants =====
    // Each mirrors its non-blocked counterpart but excludes posts authored by any user on the
    // viewer's "block wall" (blocked-either-way). Filtering is done in the database via NOT IN.

    /**
     * Block-filtered variant of {@link #findFollowingPostDtos} (filter: FOLLOWING).
     * Same semantics, additionally excluding any author on the viewer's block wall.
     */
    @Query("SELECT new com.gameon.model.dto.PostFeedDto(" +
           "p.postId, p.content, p.privacySetting, p.createdAt, " +
           "p.user.userId, p.user.username, " +
           "(SELECT COUNT(l) FROM Like l WHERE l.post = p), " +
           "(SELECT COUNT(c) FROM Comment c WHERE c.post = p)) " +
           "FROM Post p " +
           "WHERE p.removedAt IS NULL AND p.user.userId IN :followedUserIds " +
           "AND p.user.userId NOT IN :blockedUserIds " +
           "ORDER BY p.createdAt DESC")
    Page<PostFeedDto> findFollowingPostDtosExcludingBlocked(
            @Param("followedUserIds") List<Long> followedUserIds,
            @Param("blockedUserIds") List<Long> blockedUserIds,
            Pageable pageable);

    @Query("SELECT new com.gameon.model.dto.PostFeedDto(" +
           "p.postId, p.content, p.privacySetting, p.createdAt, " +
           "p.user.userId, p.user.username, " +
           "(SELECT COUNT(l) FROM Like l WHERE l.post = p), " +
           "(SELECT COUNT(c) FROM Comment c WHERE c.post = p)) " +
           "FROM Post p " +
           "WHERE p.removedAt IS NULL AND (p.privacySetting = 'PUBLIC' " +
           "OR (p.privacySetting = 'FOLLOWERS' AND p.user.userId IN :followedUserIds) " +
           "OR p.user.userId = :userId) " +
           "AND p.user.userId NOT IN :blockedUserIds " +
           "ORDER BY p.createdAt DESC")
    Page<PostFeedDto> findAllVisiblePostDtosExcludingBlocked(
            @Param("userId") Long userId,
            @Param("followedUserIds") List<Long> followedUserIds,
            @Param("blockedUserIds") List<Long> blockedUserIds,
            Pageable pageable);

    @Query("SELECT new com.gameon.model.dto.PostFeedDto(" +
           "p.postId, p.content, p.privacySetting, p.createdAt, " +
           "p.user.userId, p.user.username, " +
           "(SELECT COUNT(l) FROM Like l WHERE l.post = p), " +
           "(SELECT COUNT(c) FROM Comment c WHERE c.post = p)) " +
           "FROM Post p " +
           "WHERE p.removedAt IS NULL AND (p.privacySetting = 'PUBLIC' " +
           "OR p.user.userId = :userId) " +
           "AND p.user.userId NOT IN :blockedUserIds " +
           "ORDER BY p.createdAt DESC")
    Page<PostFeedDto> findAllVisiblePostDtosNoFollowsExcludingBlocked(
            @Param("userId") Long userId,
            @Param("blockedUserIds") List<Long> blockedUserIds,
            Pageable pageable);
}
