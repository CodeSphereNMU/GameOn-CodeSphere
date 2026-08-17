package com.gameon.repository;

import com.gameon.model.entity.UserSportProfile;
import com.gameon.model.entity.UserSportProfileId;
import com.gameon.model.enums.SkillLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSportProfileRepository extends JpaRepository<UserSportProfile, UserSportProfileId> {

    List<UserSportProfile> findByIdUserId(Long userId);

    Optional<UserSportProfile> findByIdUserIdAndIdSportId(Long userId, Long sportId);

    boolean existsByIdUserIdAndIdSportId(Long userId, Long sportId);

    long countByIdUserId(Long userId);

    void deleteByIdUserIdAndIdSportId(Long userId, Long sportId);

    @Query("SELECT usp FROM UserSportProfile usp WHERE usp.sport.sportId = :sportId " +
           "AND (usp.wins + usp.losses) > 0 ORDER BY usp.winPercentage DESC")
    Page<UserSportProfile> findTopBySportOrderByWinPercentageDesc(
            @Param("sportId") Long sportId, Pageable pageable);

    @Query("SELECT usp FROM UserSportProfile usp WHERE usp.id.userId IN :userIds " +
           "AND usp.sport.sportId = :sportId ORDER BY usp.winPercentage DESC")
    List<UserSportProfile> findByUserIdsAndSport(
            @Param("userIds") List<Long> userIds, @Param("sportId") Long sportId);

    @Query("SELECT usp FROM UserSportProfile usp WHERE usp.sport.sportId = :sportId " +
           "AND usp.skillLevel = :skillLevel ORDER BY usp.winPercentage DESC")
    List<UserSportProfile> findBySportAndSkillLevel(
            @Param("sportId") Long sportId, @Param("skillLevel") SkillLevel skillLevel);

    @Query("SELECT usp.sport.sportId FROM UserSportProfile usp WHERE usp.id.userId = :userId")
    List<Long> findSportIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT DISTINCT usp.sport.sportId FROM UserSportProfile usp WHERE usp.id.userId = :userId")
    List<Long> findDistinctSportIdsByUserId(@Param("userId") Long userId);

    /**
     * Aggregates total wins and total matches played (wins + losses) across all sports
     * for a set of user IDs. Returns Object[] rows: [userId, username, totalWins, totalMatchesPlayed].
     * Used for the Friends Leaderboard feature.
     */
    @Query("SELECT usp.user.userId, usp.user.username, " +
           "SUM(usp.wins), SUM(usp.wins + usp.losses) " +
           "FROM UserSportProfile usp WHERE usp.id.userId IN :userIds " +
           "GROUP BY usp.user.userId, usp.user.username " +
           "ORDER BY SUM(usp.wins) DESC")
    List<Object[]> findAggregatedStatsByUserIds(@Param("userIds") List<Long> userIds);
}
