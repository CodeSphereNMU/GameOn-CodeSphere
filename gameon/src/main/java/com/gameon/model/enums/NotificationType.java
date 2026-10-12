package com.gameon.model.enums;

public enum NotificationType {
    FOLLOW_NEW("New follower!"),
    JOIN_REQUEST_RECEIVED("New Join request!"),
    JOIN_ACCEPTED("You're in!"),
    JOIN_REJECTED("Request declined"),
    JOIN_WITHDRAWN("Player withdrew"),
    LISTING_CONFIRMED("Game On!"),
    LISTING_CANCELLED_INSUFFICIENT_PLAYERS("Game cancelled"),
    MATCH_RESULT_POSTED("Results are in!"),
    MATCH_RESULT_UPDATED("Result updated"),
    LISTING_CANCELLED("Game cancelled"),
    LISTING_INVITE("You're invited!"),
    /** Sent when attendance confirmation window opens (T-24h). */
    ATTENDANCE_CONFIRMATION_OPEN("Attendance confirmation open"),
    /** Sent as a reminder that the T-2h confirmation deadline is approaching. */
    ATTENDANCE_CONFIRMATION_DEADLINE("Confirmation deadline approaching!"),
    /** Creator has approved this user for a last-call place; they can claim it. */
    LAST_CALL_OFFER("A spot just opened up!"),
    /** User successfully claimed a last-call place. */
    LAST_CALL_CLAIMED("Spot secured!"),
    /** Organiser notified that a last-call place in their game has been filled. */
    LAST_CALL_PLACE_FILLED("Spot filled!"),
    /** Player's last-call claim failed because all places were filled. */
    LAST_CALL_FULL("Spot filled!"),
    /** Player's place was released because they did not confirm by T-2h. */
    PLACE_RELEASED_UNCONFIRMED("Spot released"),
    /** Listing cancelled because the creator did not confirm attendance by T-1h. */
    LISTING_CANCELLED_CREATOR_UNCONFIRMED("Game cancelled"),
    /** Urgent reminder to creator: must confirm attendance before T-1h or listing will be cancelled. */
    CREATOR_CONFIRMATION_URGENT("Final call!"),
    ATTENDANCE_CONFIRMED("Attendance confirmed"),
    ATTENDANCE_REMINDER("Ready to play?"),
    POST_COMMENTED("New comment!"),
    MATCH_RESULT_REMINDER("How did it go?"),
    /** A moderator issued a community strike/warning against the user. */
    STRIKE_ISSUED("STRIKE ISSUED"),
    LISTING_NEEDS_PLAYERS("Still need players!");

    private final String heading;

    NotificationType(String heading) {
        this.heading = heading;
    }

    public String getHeading() {
        return heading;
    }
}
