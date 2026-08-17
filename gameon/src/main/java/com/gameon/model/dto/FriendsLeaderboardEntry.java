package com.gameon.model.dto;

/**
 * DTO for Friends Leaderboard entries on the profile page.
 * Represents a user's aggregated match statistics across all sports.
 * Structured to support future filtering (weekly, monthly, all-time).
 */
public record FriendsLeaderboardEntry(
        int rank,
        Long userId,
        String username,
        int totalWins,
        int totalMatchesPlayed,
        double winPercentage
) {
}
