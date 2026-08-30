package com.gameon.model.enums;

public enum NotificationType {
    FOLLOW_NEW,
    JOIN_REQUEST_RECEIVED,
    JOIN_ACCEPTED,
    JOIN_REJECTED,
    JOIN_WITHDRAWN,
    LISTING_CONFIRMED,
    LISTING_CANCELLED_INSUFFICIENT_PLAYERS,
    MATCH_RESULT_POSTED,
    MATCH_RESULT_UPDATED,
    LISTING_CANCELLED,
    LISTING_INVITE,
    /** Sent when attendance confirmation window opens (T-24h). */
    ATTENDANCE_CONFIRMATION_OPEN,
    /** Sent as a reminder that the T-2h confirmation deadline is approaching. */
    ATTENDANCE_CONFIRMATION_DEADLINE,
    /** Creator has approved this user for a last-call place; they can claim it. */
    LAST_CALL_OFFER,
    /** User successfully claimed a last-call place. */
    LAST_CALL_CLAIMED,
    /** Last-call claim failed because all places are now filled. */
    LAST_CALL_FULL,
    /** Player's place was released because they did not confirm by T-2h. */
    PLACE_RELEASED_UNCONFIRMED,
    /** Listing cancelled because the creator did not confirm attendance by T-1h. */
    LISTING_CANCELLED_CREATOR_UNCONFIRMED,
    /** Urgent reminder to creator: must confirm attendance before T-1h or listing will be cancelled. */
    CREATOR_CONFIRMATION_URGENT
}
