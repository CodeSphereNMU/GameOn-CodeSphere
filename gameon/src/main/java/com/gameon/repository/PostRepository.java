package com.gameon.repository;

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

    // Social feed: public posts + followers-only from followed users
    @Query("SELECT p FROM Post p " +
           "WHERE (p.privacySetting = 'PUBLIC' AND p.user.userId IN :visibleUserIds) " +
           "OR (p.privacySetting = 'FOLLOWERS' AND p.user.userId IN :followedUserIds) " +
           "ORDER BY p.createdAt DESC")
    Page<Post> findFeedPosts(
            @Param("visibleUserIds") List<Long> visibleUserIds,
            @Param("followedUserIds") List<Long> followedUserIds,
            Pageable pageable);

    // All public posts ordered by date
    Page<Post> findByPrivacySettingOrderByCreatedAtDesc(PrivacySetting privacySetting, Pageable pageable);

    // Count posts by user
    long countByUserUserId(Long userId);
}
