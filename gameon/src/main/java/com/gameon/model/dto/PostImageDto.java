package com.gameon.model.dto;

/**
 * Lightweight projection of a single post image for feed/detail rendering.
 * Carries the parent post id, the public image path, and its display order.
 */
public record PostImageDto(
        Long postId,
        String imagePath,
        int displayOrder
) {
}
