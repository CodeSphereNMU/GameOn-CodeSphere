package com.gameon.repository;

import com.gameon.model.entity.Like;
import com.gameon.model.entity.LikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface LikeRepository extends JpaRepository<Like, LikeId> {

    boolean existsByIdUserIdAndIdPostId(Long userId, Long postId);

    long countByIdPostId(Long postId);

    /**
     * Returns the subset of {@code postIds} that the given user has liked. Used to batch-resolve
     * the "liked by current user" state for a whole page of feed posts in a single query,
     * avoiding per-post N+1 lookups.
     */
    @Query("SELECT l.id.postId FROM Like l WHERE l.id.userId = :userId AND l.id.postId IN :postIds")
    List<Long> findLikedPostIds(@Param("userId") Long userId, @Param("postIds") Collection<Long> postIds);

    @Modifying
    @Query("DELETE FROM Like l WHERE l.id.userId = :userId AND l.id.postId = :postId")
    void deleteByUserIdAndPostId(@Param("userId") Long userId, @Param("postId") Long postId);
}
