package com.gameon.model.enums;

/**
 * The kind of content a moderation strike relates to.
 * Mirrors the content categories that can be reported and moderated.
 */
public enum ContentType {
    /** Strike relates to a post. */
    POST,
    /** Strike relates to a comment. */
    COMMENT,
    /** Strike relates to the user's general conduct (no single content item). */
    USER,
    /** Strike issued without a specific content reference. */
    OTHER
}
