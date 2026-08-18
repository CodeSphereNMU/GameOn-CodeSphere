package com.gameon.model.dto;

import com.gameon.model.enums.PrivacySetting;

import java.time.LocalDateTime;

/**
 * DTO projection for the social feed.
 * Carries only the data needed to render a feed card, with like/comment counts
 * computed at the database level via COUNT subqueries. This eliminates
 * LazyInitializationException and N+1 query problems entirely.
 */
public record PostFeedDto(
        Long postId,
        String content,
        String imagePath,
        PrivacySetting privacySetting,
        LocalDateTime createdAt,
        Long userId,
        String username,
        long likeCount,
        long commentCount
) {
}
