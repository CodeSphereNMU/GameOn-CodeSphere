package com.gameon.model.enums;

/**
 * The kinds of moderator/administrator actions recorded in the moderation audit trail.
 * Every action a moderator takes is stored permanently as a {@code moderation_actions} row.
 */
public enum ModerationActionType {
    /** A report was reviewed and dismissed with no action against the user/content. */
    DISMISS_REPORT,
    /** A community warning/strike was issued to a user. */
    ISSUE_WARNING,
    /** Content was soft-removed and a warning/strike was issued to its author. */
    REMOVE_CONTENT_AND_WARNING,
    /** A user account was suspended. */
    SUSPEND,
    /** A user account was banned. */
    BAN,
    /** A previously issued warning/strike was reversed (audit record kept). */
    REVERSE_WARNING,
    /** Previously removed content was restored. */
    RESTORE_CONTENT,
    /** A banned/suspended user was unbanned/reactivated. */
    UNBAN,
    /** A user's active warning count was reset to zero. */
    RESET_WARNINGS
}
