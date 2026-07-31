package com.gameon.service;

import com.gameon.model.entity.UserSportProfile;
import com.gameon.repository.UserSportProfileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service handling leaderboard rankings.
 * Covers B500 (View Leaderboards).
 * Rankings are based on win percentage per sport, requiring at least 1 game played.
 */
@Service
public class LeaderboardService {

    private static final int DEFAULT_TOP_N = 50;

    private final UserSportProfileRepository userSportProfileRepository;

    public LeaderboardService(UserSportProfileRepository userSportProfileRepository) {
        this.userSportProfileRepository = userSportProfileRepository;
    }

    /**
     * Gets the leaderboard for a specific sport.
     * Only includes users who have played at least 1 game.
     * Ranked by win percentage descending.
     */
    @Transactional(readOnly = true)
    public Page<UserSportProfile> getLeaderboard(Long sportId, Pageable pageable) {
        return userSportProfileRepository.findTopBySportOrderByWinPercentageDesc(sportId, pageable);
    }

    /**
     * Gets the top N players for a sport (for summary display).
     */
    @Transactional(readOnly = true)
    public Page<UserSportProfile> getTopPlayers(Long sportId, int topN) {
        return userSportProfileRepository.findTopBySportOrderByWinPercentageDesc(
                sportId, PageRequest.of(0, topN));
    }

    /**
     * Gets the default top 50 leaderboard for a sport.
     */
    @Transactional(readOnly = true)
    public Page<UserSportProfile> getDefaultLeaderboard(Long sportId) {
        return getTopPlayers(sportId, DEFAULT_TOP_N);
    }

    /**
     * Gets leaderboard filtered by skill level for a sport.
     */
    @Transactional(readOnly = true)
    public List<UserSportProfile> getLeaderboardBySkill(Long sportId,
                                                        com.gameon.model.enums.SkillLevel skillLevel) {
        return userSportProfileRepository.findBySportAndSkillLevel(sportId, skillLevel);
    }
}
