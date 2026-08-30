package com.gameon.model.enums;

public enum JoinerStatus {
    /** Player accepted by creator but has not yet confirmed attendance. */
    ACCEPTED,
    /** Player explicitly confirmed attendance (before or at T-2h deadline). */
    CONFIRMED_ATTENDANCE,
    /** Player locked into a confirmed game (legacy/finalised state after T-1h). */
    LOCKED,
    /** Player left or was released from the listing. */
    LEFT
}
