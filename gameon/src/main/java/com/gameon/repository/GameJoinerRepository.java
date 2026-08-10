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

    // BR10: Time conflict check (within 3 hours)
    @Query("SELECT gj FROM GameJoiner gj " +
           "JOIN gj.gameListing gl " +
           "WHERE gj.id.userId = :userId " +
           "AND gj.status IN ('PENDING', 'ACCEPTED', 'LOCKED') " +
           "AND gl.scheduledDate BETWEEN :startTime AND :endTime")
    List<GameJoiner> findUserJoinedListingsInTimeRange(
            @Param("userId") Long userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    // Joined listings for lobby (user's perspective)
    @Query("SELECT gj FROM GameJoiner gj " +
           "JOIN FETCH gj.gameListing gl " +
           "JOIN FETCH gl.format sf " +
           "JOIN FETCH sf.sport " +
           "WHERE gj.id.userId = :userId " +
           "AND gj.status IN ('ACCEPTED', 'LOCKED', 'PENDING') " +
           "ORDER BY gl.scheduledDate ASC")
    List<GameJoiner> findJoinedListingsForUser(@Param("userId") Long userId);

    // Lock all accepted joiners for a listing (A700)
    @Modifying
    @Query("UPDATE GameJoiner gj SET gj.status = 'LOCKED', gj.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE gj.id.gameListingId = :listingId AND gj.status = 'ACCEPTED'")
    int lockAllAcceptedJoiners(@Param("listingId") Long listingId);

    // Get all participants (accepted + locked) for notifications
    @Query("SELECT gj FROM GameJoiner gj " +
           "WHERE gj.id.gameListingId = :listingId " +
           "AND gj.status IN ('ACCEPTED', 'LOCKED')")
    List<GameJoiner> findParticipants(@Param("listingId") Long listingId);

    // Check if user has an active (PENDING) join request for a listing
    @Query("SELECT CASE WHEN COUNT(gj) > 0 THEN true ELSE false END FROM GameJoiner gj " +
           "WHERE gj.id.userId = :userId AND gj.id.gameListingId = :listingId " +
           "AND gj.status = 'PENDING'")
    boolean existsPendingRequest(@Param("userId") Long userId, @Param("listingId") Long listingId);

    // Check if user is already an accepted/locked participant
    @Query("SELECT CASE WHEN COUNT(gj) > 0 THEN true ELSE false END FROM GameJoiner gj " +
           "WHERE gj.id.userId = :userId AND gj.id.gameListingId = :listingId " +
           "AND gj.status IN ('ACCEPTED', 'LOCKED')")
    boolean existsAcceptedOrLocked(@Param("userId") Long userId, @Param("listingId") Long listingId);

    // Find joiner by user and listing with specific statuses (for re-request logic)
    @Query("SELECT gj FROM GameJoiner gj " +
           "WHERE gj.id.userId = :userId AND gj.id.gameListingId = :listingId")
    java.util.Optional<GameJoiner> findByUserAndListing(@Param("userId") Long userId, @Param("listingId") Long listingId);
}
