package com.gameon.repository;

import com.gameon.model.entity.Like;
import com.gameon.model.entity.LikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LikeRepository extends JpaRepository<Like, LikeId> {

    boolean existsByIdUserIdAndIdPostId(Long userId, Long postId);

    long countByIdPostId(Long postId);

    @Modifying
    @Query("DELETE FROM Like l WHERE l.id.userId = :userId AND l.id.postId = :postId")
    void deleteByUserIdAndPostId(@Param("userId") Long userId, @Param("postId") Long postId);
}
