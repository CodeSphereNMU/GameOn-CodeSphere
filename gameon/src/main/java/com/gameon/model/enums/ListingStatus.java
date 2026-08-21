package com.gameon.model.enums;

/** Persistent business lifecycle for a game listing. */
public enum ListingStatus {
    OPEN,
    CONFIRMED,
    CANCELLED_INSUFFICIENT_PLAYERS,
    CANCELLED_BY_CREATOR,
    COMPLETED
}
