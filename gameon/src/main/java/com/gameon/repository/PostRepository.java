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

    List<Post> findByUserUserId(Long userId);

    Page<Post> findByUserUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT p FROM Post p JOIN FETCH p.user WHERE p.postId = :postId")
    Optional<Post> findByIdWithUser(@Param("postId") Long postId);

    // ===== Feed DTO projections (counts computed at DB level) =====

    /**
     * Social feed with DTO projection: public posts + followers-only from followed users.
     * Computes like/comment counts via subqueries — no lazy collections touched.
     */
    @Query("SELECT new com.gameon.model.dto.PostFeedDto(" +
           "p.postId, p.content, p.imagePath, p.privacySetting, p.createdAt, " +
           "p.user.userId, p.user.username, " +
           "(SELECT COUNT(l) FROM Like l WHERE l.post = p), " +
           "(SELECT COUNT(c) FROM Comment c WHERE c.post = p)) " +
           "FROM Post p " +
           "WHERE (p.privacySetting = 'PUBLIC' AND p.user.userId IN :visibleUserIds) " +
           "OR (p.privacySetting = 'FOLLOWERS' AND p.user.userId IN :followedUserIds) " +
           "ORDER BY p.createdAt DESC")
    Page<PostFeedDto> findFeedPostDtos(
            @Param("visibleUserIds") List<Long> visibleUserIds,
            @Param("followedUserIds") List<Long> followedUserIds,
            Pageable pageable);

    /**
     * Public-only feed with DTO projection (used when user follows nobody).
     */
    @Query("SELECT new com.gameon.model.dto.PostFeedDto(" +
           "p.postId, p.content, p.imagePath, p.privacySetting, p.createdAt, " +
           "p.user.userId, p.user.username, " +
           "(SELECT COUNT(l) FROM Like l WHERE l.post = p), " +
           "(SELECT COUNT(c) FROM Comment c WHERE c.post = p)) " +
           "FROM Post p " +
           "WHERE p.privacySetting = 'PUBLIC' " +
           "ORDER BY p.createdAt DESC")
    Page<PostFeedDto> findPublicFeedPostDtos(Pageable pageable);

    // ===== Legacy entity queries (used by edit/delete/detail flows) =====

    @Query("SELECT p FROM Post p " +
           "WHERE (p.privacySetting = 'PUBLIC' AND p.user.userId IN :visibleUserIds) " +
           "OR (p.privacySetting = 'FOLLOWERS' AND p.user.userId IN :followedUserIds) " +
           "ORDER BY p.createdAt DESC")
    Page<Post> findFeedPosts(
            @Param("visibleUserIds") List<Long> visibleUserIds,
            @Param("followedUserIds") List<Long> followedUserIds,
            Pageable pageable);

    Page<Post> findByPrivacySettingOrderByCreatedAtDesc(PrivacySetting privacySetting, Pageable pageable);

    // Count posts by user
    long countByUserUserId(Long userId);

    // ===== Filtered Feed DTO projections =====

    /**
     * Public-only posts from all users (filter: PUBLIC).
     */
    @Query("SELECT new com.gameon.model.dto.PostFeedDto(" +
           "p.postId, p.content, p.imagePath, p.privacySetting, p.createdAt, " +
           "p.user.userId, p.user.username, " +
           "(SELECT COUNT(l) FROM Like l WHERE l.post = p), " +
           "(SELECT COUNT(c) FROM Comment c WHERE c.post = p)) " +
           "FROM Post p " +
           "WHERE p.privacySetting = 'PUBLIC' " +
           "ORDER BY p.createdAt DESC")
    Page<PostFeedDto> findPublicPostDtos(Pageable pageable);

    /**
     * Followers-only posts from users the current user follows (filter: FOLLOWERS).
     * Security: only returns FOLLOWERS posts where the author is in the followedUserIds list.
     */
    @Query("SELECT new com.gameon.model.dto.PostFeedDto(" +
           "p.postId, p.content, p.imagePath, p.privacySetting, p.createdAt, " +
           "p.user.userId, p.user.username, " +
           "(SELECT COUNT(l) FROM Like l WHERE l.post = p), " +
           "(SELECT COUNT(c) FROM Comment c WHERE c.post = p)) " +
           "FROM Post p " +
           "WHERE p.privacySetting = 'FOLLOWERS' AND p.user.userId IN :followedUserIds " +
           "ORDER BY p.createdAt DESC")
    Page<PostFeedDto> findFollowersOnlyPostDtos(
            @Param("followedUserIds") List<Long> followedUserIds,
            Pageable pageable);

    /**
     * All posts by a specific user regardless of privacy (filter: MY_POSTS).
     */
    @Query("SELECT new com.gameon.model.dto.PostFeedDto(" +
           "p.postId, p.content, p.imagePath, p.privacySetting, p.createdAt, " +
           "p.user.userId, p.user.username, " +
           "(SELECT COUNT(l) FROM Like l WHERE l.post = p), " +
           "(SELECT COUNT(c) FROM Comment c WHERE c.post = p)) " +
           "FROM Post p " +
           "WHERE p.user.userId = :userId " +
           "ORDER BY p.createdAt DESC")
    Page<PostFeedDto> findMyPostDtos(@Param("userId") Long userId, Pageable pageable);

    /**
     * Full visible feed including user's own posts (filter: ALL).
     * Shows: public posts + followers-only from followed users + all own posts.
     */
    @Query("SELECT new com.gameon.model.dto.PostFeedDto(" +
           "p.postId, p.content, p.imagePath, p.privacySetting, p.createdAt, " +
           "p.user.userId, p.user.username, " +
           "(SELECT COUNT(l) FROM Like l WHERE l.post = p), " +
           "(SELECT COUNT(c) FROM Comment c WHERE c.post = p)) " +
           "FROM Post p " +
           "WHERE p.privacySetting = 'PUBLIC' " +
           "OR (p.privacySetting = 'FOLLOWERS' AND p.user.userId IN :followedUserIds) " +
           "OR p.user.userId = :userId " +
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
           "p.postId, p.content, p.imagePath, p.privacySetting, p.createdAt, " +
           "p.user.userId, p.user.username, " +
           "(SELECT COUNT(l) FROM Like l WHERE l.post = p), " +
           "(SELECT COUNT(c) FROM Comment c WHERE c.post = p)) " +
           "FROM Post p " +
           "WHERE p.privacySetting = 'PUBLIC' " +
           "OR p.user.userId = :userId " +
           "ORDER BY p.createdAt DESC")
    Page<PostFeedDto> findAllVisiblePostDtosNoFollows(@Param("userId") Long userId, Pageable pageable);
}
