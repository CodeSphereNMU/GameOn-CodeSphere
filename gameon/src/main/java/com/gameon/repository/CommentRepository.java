package com.gameon.repository;

import com.gameon.model.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("SELECT c FROM Comment c JOIN FETCH c.user WHERE c.post.postId = :postId ORDER BY c.createdAt ASC")
    List<Comment> findByPostIdWithUser(@Param("postId") Long postId);

    List<Comment> findByPostPostIdOrderByCreatedAtAsc(Long postId);

    long countByPostPostId(Long postId);

    List<Comment> findByUserUserId(Long userId);
}
