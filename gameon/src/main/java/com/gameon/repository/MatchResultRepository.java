package com.gameon.repository;

import com.gameon.model.entity.MatchResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchResultRepository extends JpaRepository<MatchResult, Long> {

    Optional<MatchResult> findByGameListingGameListingId(Long gameListingId);

    boolean existsByGameListingGameListingId(Long gameListingId);

    // Match history for user (as creator or participant)
    @Query("SELECT mr FROM MatchResult mr " +
           "JOIN FETCH mr.gameListing gl " +
           "JOIN FETCH gl.format sf " +
           "JOIN FETCH sf.sport " +
           "WHERE gl.listingStatus = 'COMPLETED' " +
           "AND (gl.creator.userId = :userId " +
           "     OR EXISTS (SELECT gj FROM GameJoiner gj " +
           "                WHERE gj.id.gameListingId = gl.gameListingId " +
           "                AND gj.id.userId = :userId " +
           "                AND gj.status = 'LOCKED')) " +
           "ORDER BY gl.scheduledDate DESC")
    List<MatchResult> findMatchHistoryForUser(@Param("userId") Long userId);

    // Recent results for a sport
    @Query("SELECT mr FROM MatchResult mr " +
           "JOIN mr.gameListing gl " +
           "JOIN gl.format sf " +
           "WHERE sf.sport.sportId = :sportId " +
           "ORDER BY mr.createdAt DESC")
    List<MatchResult> findRecentBySport(@Param("sportId") Long sportId);
}
