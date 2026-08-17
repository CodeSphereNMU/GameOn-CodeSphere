package com.gameon.service;

import com.gameon.model.dto.FriendsLeaderboardEntry;
import com.gameon.repository.FollowRepository;
import com.gameon.repository.UserSportProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for the Friends Leaderboard feature on the profile page.
 * Ranks the current user and their friends (followers + following) by total match wins.
 *
 * Structured to support future filter additions (weekly, monthly, all-time).
 */
@Service
public class FriendsLeaderboardService {

    private final FollowRepository followRepository;
    private final UserSportProfileRepository userSportProfileRepository;

    public FriendsLeaderboardService(FollowRepository followRepository,
                                     UserSportProfileRepository userSportProfileRepository) {
        this.followRepository = followRepository;
        this.userSportProfileRepository = userSportProfileRepository;
    }

    /**
     * Builds the friends leaderboard for a given user.
     * Includes: the user themselves + users they follow + users who follow them.
     * Ranked by total wins descending (all-time, across all sports).
     *
     * @param userId the current user's ID
     * @return ranked list of leaderboard entries, empty list if no friends
     */
    @Transactional(readOnly = true)
    public List<FriendsLeaderboardEntry> getFriendsLeaderboard(Long userId) {
        // Gather friend user IDs (following + followers)
        Set<Long> friendIds = new HashSet<>();
        friendIds.addAll(followRepository.findFollowingUserIds(userId));
        friendIds.addAll(followRepository.findFollowerUserIds(userId));

        // If no friends, return empty
        if (friendIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Always include the current user
        friendIds.add(userId);

        // Get aggregated stats across all sports
        List<Object[]> rawStats = userSportProfileRepository.findAggregatedStatsByUserIds(
                new ArrayList<>(friendIds));

        // Map to DTOs with ranking
        List<FriendsLeaderboardEntry> entries = new ArrayList<>();
        int rank = 1;

        for (Object[] row : rawStats) {
            Long uid = (Long) row[0];
            String username = (String) row[1];
            int totalWins = ((Number) row[2]).intValue();
            int totalMatchesPlayed = ((Number) row[3]).intValue();

            double winPct = totalMatchesPlayed > 0
                    ? Math.round((double) totalWins / totalMatchesPlayed * 1000.0) / 10.0
                    : 0.0;

            entries.add(new FriendsLeaderboardEntry(rank, uid, username, totalWins, totalMatchesPlayed, winPct));
            rank++;
        }

        // Include friends who have no sport profiles (0 wins, 0 matches)
        // They won't appear in the aggregated query, so add them at the end
        Set<Long> usersWithStats = entries.stream()
                .map(FriendsLeaderboardEntry::userId)
                .collect(Collectors.toSet());

        // We don't add users without any sport profile to the leaderboard
        // since they have no match data to rank

        return entries;
    }
}
