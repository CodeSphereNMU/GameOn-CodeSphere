package com.gameon.repository;

import com.gameon.model.entity.GameJoiner;
import com.gameon.model.entity.GameJoinerId;
import com.gameon.model.enums.JoinerStatus;
import com.gameon.model.enums.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GameJoinerRepository extends JpaRepository<GameJoiner, GameJoinerId> {

    List<GameJoiner> findByIdGameListingId(Long gameListingId);

    List<GameJoiner> findByIdGameListingIdAndStatus(Long gameListingId, JoinerStatus status);

    List<GameJoiner> findByIdGameListingIdAndTeamAndStatus(Long gameListingId, Team team, JoinerStatus status);

    List<GameJoiner> findByIdUserId(Long userId);

    boolean existsByIdUserIdAndIdGameListingId(Long userId, Long gameListingId);

    long countByIdGameListingIdAndTeamAndStatus(Long gameListingId, Team team, JoinerStatus status);

    long countByIdGameListingIdAndStatusIn(Long gameListingId, List<JoinerStatus> statuses);

    // Time conflict candidate lookup for actual participants only.
    @Query("SELECT gj FROM GameJoiner gj " +
           "JOIN gj.gameListing gl " +
           "WHERE gj.id.userId = :userId " +
           "AND gj.status IN ('ACCEPTED', 'CONFIRMED_ATTENDANCE', 'LOCKED') " +
           "AND gl.scheduledDate BETWEEN :startTime AND :endTime")
    List<GameJoiner> findUserJoinedListingsInTimeRange(
            @Param("userId") Long userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    // Joined listings for lobby (user's perspective) — current/upcoming only
    @Query("SELECT gj FROM GameJoiner gj " +
           "JOIN FETCH gj.gameListing gl " +
           "JOIN FETCH gl.format sf " +
           "JOIN FETCH sf.sport " +
           "WHERE gj.id.userId = :userId " +
           "AND gj.status IN ('ACCEPTED', 'CONFIRMED_ATTENDANCE', 'LOCKED') " +
           "AND gl.listingStatus IN ('OPEN', 'CONFIRMED') " +
           "AND gl.scheduledDate > :now " +
           "AND gl.creator.userId <> :userId " +
           "ORDER BY gl.scheduledDate ASC")
    List<GameJoiner> findJoinedListingsForUser(@Param("userId") Long userId,
                                               @Param("now") LocalDateTime now);

    // Lock all confirmed-attendance joiners for a listing at finalisation (T-1h)
    @Modifying
    @Query("UPDATE GameJoiner gj SET gj.status = 'LOCKED', gj.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE gj.id.gameListingId = :listingId AND gj.status = 'CONFIRMED_ATTENDANCE'")
    int lockAllConfirmedJoiners(@Param("listingId") Long listingId);

    // Legacy: Lock all accepted joiners (kept for backward compatibility during migration)
    @Modifying
    @Query("UPDATE GameJoiner gj SET gj.status = 'LOCKED', gj.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE gj.id.gameListingId = :listingId AND gj.status = 'ACCEPTED'")
    int lockAllAcceptedJoiners(@Param("listingId") Long listingId);

    // Get all active participants (accepted + confirmed + locked) for notifications
    @Query("SELECT gj FROM GameJoiner gj " +
           "WHERE gj.id.gameListingId = :listingId " +
           "AND gj.status IN ('ACCEPTED', 'CONFIRMED_ATTENDANCE', 'LOCKED')")
    List<GameJoiner> findParticipants(@Param("listingId") Long listingId);

    // Get confirmed-attendance participants only (for T-2h→T-1h period and finalisation)
    @Query("SELECT gj FROM GameJoiner gj " +
           "WHERE gj.id.gameListingId = :listingId " +
           "AND gj.status = 'CONFIRMED_ATTENDANCE'")
    List<GameJoiner> findConfirmedParticipants(@Param("listingId") Long listingId);

    // Get unconfirmed accepted participants (those who haven't confirmed by T-2h)
    @Query("SELECT gj FROM GameJoiner gj " +
           "WHERE gj.id.gameListingId = :listingId " +
           "AND gj.status = 'ACCEPTED' " +
           "AND gj.attendanceConfirmedAt IS NULL")
    List<GameJoiner> findUnconfirmedAccepted(@Param("listingId") Long listingId);

    // Check if user is already an accepted/confirmed/locked participant
    @Query("SELECT CASE WHEN COUNT(gj) > 0 THEN true ELSE false END FROM GameJoiner gj " +
           "WHERE gj.id.userId = :userId AND gj.id.gameListingId = :listingId " +
           "AND gj.status IN ('ACCEPTED', 'CONFIRMED_ATTENDANCE', 'LOCKED')")
    boolean existsAcceptedOrLocked(@Param("userId") Long userId, @Param("listingId") Long listingId);

    @Query("SELECT CASE WHEN COUNT(gj) > 0 THEN true ELSE false END FROM GameJoiner gj " +
           "WHERE gj.id.gameListingId = :listingId AND gj.id.userId <> :creatorId " +
           "AND gj.status IN ('ACCEPTED', 'CONFIRMED_ATTENDANCE', 'LOCKED')")
    boolean existsNonCreatorParticipant(@Param("listingId") Long listingId,
                                        @Param("creatorId") Long creatorId);

    @Query("SELECT gj FROM GameJoiner gj " +
           "WHERE gj.id.userId = :userId AND gj.id.gameListingId = :listingId")
    java.util.Optional<GameJoiner> findByUserAndListing(@Param("userId") Long userId, @Param("listingId") Long listingId);
}
