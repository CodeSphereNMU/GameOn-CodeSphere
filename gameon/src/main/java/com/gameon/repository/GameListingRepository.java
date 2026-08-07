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
    @Query("SELECT gl FROM GameListing gl WHERE gl.creator.userId = :userId AND gl.isCompleted = false")
    Optional<GameListing> findActiveByCreator(@Param("userId") Long userId);

    long countByCreatorUserIdAndIsCompletedFalse(Long userId);

    List<GameListing> findByCreatorUserId(Long userId);

    // A200: Browse available listings (future, not completed, not own)
    @Query("SELECT gl FROM GameListing gl " +
           "WHERE gl.format.formatId IN :formatIds " +
           "AND gl.scheduledDate > :now " +
           "AND gl.isCompleted = false " +
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
           "AND gl.isCompleted = false " +
           "AND gl.creator.userId != :userId " +
           "AND gl.skillLevel = :skillLevel " +
           "ORDER BY gl.scheduledDate ASC")
    Page<GameListing> findAvailableListingsBySkill(
            @Param("formatIds") List<Long> formatIds,
            @Param("now") LocalDateTime now,
            @Param("userId") Long userId,
            @Param("skillLevel") SkillLevel skillLevel,
            Pageable pageable);

    // A700: Find full listings needing confirmation (2hrs before scheduled)
    @Query("SELECT gl FROM GameListing gl " +
           "WHERE gl.isCompleted = false " +
           "AND gl.scheduledDate BETWEEN :now AND :threshold " +
           "AND gl.session IS NULL")
    List<GameListing> findFullListingsNeedingConfirmation(
            @Param("now") LocalDateTime now,
            @Param("threshold") LocalDateTime threshold);

    // Lobby: Created listings for user
    @Query("SELECT gl FROM GameListing gl WHERE gl.creator.userId = :userId ORDER BY gl.scheduledDate DESC")
    List<GameListing> findCreatedByUser(@Param("userId") Long userId);

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
    @Query("SELECT COUNT(gl) FROM GameListing gl WHERE gl.format.formatId = :formatId AND gl.isCompleted = false")
    long countActiveByFormat(@Param("formatId") Long formatId);
}
