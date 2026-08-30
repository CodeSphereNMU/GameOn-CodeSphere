package com.gameon.repository;

import com.gameon.model.dto.PostImageDto;
import com.gameon.model.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    /**
     * Batch-loads images for a page of posts as lightweight DTOs, ordered so callers
     * can group by post while preserving carousel order. Used by the feed to attach
     * images to {@link com.gameon.model.dto.PostFeedDto} in a single query per page
     * (no per-post query, no EAGER fetching, no open-in-view required).
     */
    @Query("SELECT new com.gameon.model.dto.PostImageDto(pi.post.postId, pi.imagePath, pi.displayOrder) " +
           "FROM PostImage pi WHERE pi.post.postId IN :postIds " +
           "ORDER BY pi.post.postId ASC, pi.displayOrder ASC")
    List<PostImageDto> findImageDtosForPosts(@Param("postIds") List<Long> postIds);

    /** All images for one post, ordered for carousel display (used by the detail view). */
    List<PostImage> findByPostPostIdOrderByDisplayOrderAsc(Long postId);
}
