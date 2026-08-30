package com.gameon.repository;

import com.gameon.model.entity.GameListing;
import com.gameon.model.enums.SkillLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GameListingRepository extends JpaRepository<GameListing, Long> {

    // BR1: Check if user has active listing
    @Query("SELECT gl FROM GameListing gl WHERE gl.creator.userId = :userId " +
           "AND gl.listingStatus IN ('OPEN', 'CONFIRMED')")
    Optional<GameListing> findActiveByCreator(@Param("userId") Long userId);

    List<GameListing> findByCreatorUserId(Long userId);

    // A200: Browse available listings (future, not completed, not own)
    @Query("SELECT gl FROM GameListing gl " +
           "WHERE gl.format.formatId IN :formatIds " +
           "AND gl.scheduledDate > :now " +
           "AND gl.listingStatus = 'OPEN' " +
           "AND gl.creator.userId != :userId " +
           "ORDER BY gl.scheduledDate ASC")
    Page<GameListing> findAvailableListings(
            @Param("formatIds") List<Long> formatIds,
            @Param("now") LocalDateTime now,
            @Param("userId") Long userId,
            Pageable pageable);

    // Browse with skill filter
    @Query("SELECT gl FROM GameListing gl " +
           "WHERE gl.format.formatId IN :formatIds " +
           "AND gl.scheduledDate > :now " +
           "AND gl.listingStatus = 'OPEN' " +
           "AND gl.creator.userId != :userId " +
           "AND gl.skillLevel = :skillLevel " +
           "ORDER BY gl.scheduledDate ASC")
    Page<GameListing> findAvailableListingsBySkill(
            @Param("formatIds") List<Long> formatIds,
            @Param("now") LocalDateTime now,
            @Param("userId") Long userId,
            @Param("skillLevel") SkillLevel skillLevel,
            Pageable pageable);

    // A700: Find every open listing that has reached the two-hour confirmation deadline.
    @Query("SELECT gl FROM GameListing gl " +
           "WHERE gl.listingStatus = 'OPEN' AND gl.scheduledDate <= :threshold " +
           "ORDER BY gl.scheduledDate ASC")
    List<GameListing> findListingsNeedingLockIn(@Param("threshold") LocalDateTime threshold);

    // Find open listings that have reached the T-1h finalisation point.
    @Query("SELECT gl FROM GameListing gl " +
           "WHERE gl.listingStatus = 'OPEN' AND gl.scheduledDate <= :threshold " +
           "ORDER BY gl.scheduledDate ASC")
    List<GameListing> findListingsNeedingFinalisation(@Param("threshold") LocalDateTime threshold);

    // Lobby: active listings created by the user that have not started yet.
    @Query("SELECT gl FROM GameListing gl " +
           "WHERE gl.creator.userId = :userId " +
           "AND gl.listingStatus IN ('OPEN', 'CONFIRMED') " +
           "AND gl.scheduledDate > :now " +
           "ORDER BY gl.scheduledDate DESC")
    List<GameListing> findCreatedByUser(@Param("userId") Long userId,
                                        @Param("now") LocalDateTime now);

    // Lobby: confirmed games move to history at their start time. A result is optional.
    @Query("SELECT DISTINCT gl FROM GameListing gl " +
           "JOIN FETCH gl.format sf " +
           "JOIN FETCH sf.sport " +
           "JOIN FETCH gl.creator " +
           "LEFT JOIN FETCH gl.matchResult " +
           "WHERE gl.scheduledDate <= :now " +
           "AND gl.listingStatus IN ('CONFIRMED', 'COMPLETED') " +
           "AND (gl.creator.userId = :userId " +
           "     OR EXISTS (SELECT gj FROM GameJoiner gj " +
           "                WHERE gj.id.gameListingId = gl.gameListingId " +
           "                AND gj.id.userId = :userId " +
           "                AND gj.status IN ('CONFIRMED_ATTENDANCE', 'LOCKED'))) " +
           "ORDER BY gl.scheduledDate DESC")
    List<GameListing> findMatchHistoryForUser(@Param("userId") Long userId,
                                              @Param("now") LocalDateTime now);

    // With details (eager load format and sport)
    @Query("SELECT gl FROM GameListing gl " +
           "JOIN FETCH gl.format sf " +
           "JOIN FETCH sf.sport " +
           "JOIN FETCH gl.creator " +
           "WHERE gl.gameListingId = :id")
    Optional<GameListing> findByIdWithDetails(@Param("id") Long id);

    // Detail page query (eager load all associations needed for listings/detail.html)
    @Query("SELECT DISTINCT gl FROM GameListing gl " +
           "LEFT JOIN FETCH gl.creator " +
           "LEFT JOIN FETCH gl.format f " +
           "LEFT JOIN FETCH f.sport " +
           "LEFT JOIN FETCH gl.joiners j " +
           "LEFT JOIN FETCH j.user " +
           "WHERE gl.gameListingId = :id")
    Optional<GameListing> findDetailById(@Param("id") Long id);

    // Count active listings by sport format
    @Query("SELECT COUNT(gl) FROM GameListing gl WHERE gl.format.formatId = :formatId " +
           "AND gl.listingStatus IN ('OPEN', 'CONFIRMED')")
    long countActiveByFormat(@Param("formatId") Long formatId);

    // Upcoming listings where user is creator or accepted/confirmed/locked participant (for scheduling conflict checks)
    @Query("SELECT DISTINCT gl FROM GameListing gl " +
           "LEFT JOIN FETCH gl.format sf " +
           "LEFT JOIN FETCH sf.sport " +
           "WHERE gl.listingStatus IN ('OPEN', 'CONFIRMED') " +
           "AND gl.scheduledDate > :now " +
           "AND (gl.creator.userId = :userId " +
           "     OR gl.gameListingId IN (SELECT gj.id.gameListingId FROM GameJoiner gj " +
           "                             WHERE gj.id.userId = :userId AND gj.status IN ('ACCEPTED', 'CONFIRMED_ATTENDANCE', 'LOCKED')))")
    List<GameListing> findUpcomingListingsForUserAfter(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    // A200/A500: Browse available PUBLIC listings outside the one-hour finalisation window.
    @Query("SELECT gl FROM GameListing gl " +
           "WHERE gl.format.formatId IN :formatIds " +
           "AND gl.scheduledDate > :cutoff " +
           "AND gl.listingStatus = 'OPEN' " +
           "AND gl.creator.userId != :userId " +
           "AND gl.privacySetting = 'PUBLIC' " +
           "AND gl.gameListingId NOT IN (SELECT gj.id.gameListingId FROM GameJoiner gj " +
           "     WHERE gj.id.userId = :userId AND gj.status IN ('ACCEPTED', 'CONFIRMED_ATTENDANCE', 'LOCKED')) " +
           "ORDER BY gl.scheduledDate ASC")
    Page<GameListing> findAvailablePublicListings(
            @Param("formatIds") List<Long> formatIds,
            @Param("cutoff") LocalDateTime cutoff,
            @Param("userId") Long userId,
            Pageable pageable);

    // Browse PUBLIC listings with skill filter
    @Query("SELECT gl FROM GameListing gl " +
           "WHERE gl.format.formatId IN :formatIds " +
           "AND gl.scheduledDate > :cutoff " +
           "AND gl.listingStatus = 'OPEN' " +
           "AND gl.creator.userId != :userId " +
           "AND gl.privacySetting = 'PUBLIC' " +
           "AND gl.skillLevel = :skillLevel " +
           "AND gl.gameListingId NOT IN (SELECT gj.id.gameListingId FROM GameJoiner gj " +
           "     WHERE gj.id.userId = :userId AND gj.status IN ('ACCEPTED', 'CONFIRMED_ATTENDANCE', 'LOCKED')) " +
           "ORDER BY gl.scheduledDate ASC")
    Page<GameListing> findAvailablePublicListingsBySkill(
            @Param("formatIds") List<Long> formatIds,
            @Param("cutoff") LocalDateTime cutoff,
            @Param("userId") Long userId,
            @Param("skillLevel") SkillLevel skillLevel,
            Pageable pageable);

    @Query("SELECT gl FROM GameListing gl " +
           "WHERE gl.format.formatId IN :formatIds " +
           "AND gl.scheduledDate > :cutoff " +
           "AND gl.listingStatus = 'OPEN' " +
           "AND gl.creator.userId != :userId " +
           "AND gl.privacySetting = 'PUBLIC' " +
           "AND (:sportId IS NULL OR gl.format.sport.sportId = :sportId) " +
           "AND (:skillLevel IS NULL OR gl.skillLevel = :skillLevel) " +
           "AND (:fromDate IS NULL OR gl.scheduledDate >= :fromDate) " +
           "AND (:toDate IS NULL OR gl.scheduledDate < :toDate) " +
           "AND (:hideFull = false OR " +
           "     (SELECT COUNT(gj) FROM GameJoiner gj " +
           "      WHERE gj.gameListing = gl AND gj.status IN ('ACCEPTED', 'CONFIRMED_ATTENDANCE', 'LOCKED')) < gl.format.noPlayers) " +
           "AND gl.gameListingId NOT IN (SELECT gj2.id.gameListingId FROM GameJoiner gj2 " +
           "     WHERE gj2.id.userId = :userId AND gj2.status IN ('ACCEPTED', 'CONFIRMED_ATTENDANCE', 'LOCKED')) " +
           "ORDER BY gl.scheduledDate ASC")
    Page<GameListing> searchAvailablePublicListings(
            @Param("formatIds") List<Long> formatIds,
            @Param("cutoff") LocalDateTime cutoff,
            @Param("userId") Long userId,
            @Param("sportId") Long sportId,
            @Param("skillLevel") SkillLevel skillLevel,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("hideFull") boolean hideFull,
            Pageable pageable);
}
