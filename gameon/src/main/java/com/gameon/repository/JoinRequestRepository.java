package com.gameon.repository;

import com.gameon.model.entity.JoinRequest;
import com.gameon.model.enums.JoinRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface JoinRequestRepository extends JpaRepository<JoinRequest, Long> {

    Optional<JoinRequest> findFirstByGameListingGameListingIdAndUserUserIdAndStatusOrderByCreatedAtDesc(
            Long listingId, Long userId, JoinRequestStatus status);

    boolean existsByGameListingGameListingIdAndUserUserIdAndStatus(
            Long listingId, Long userId, JoinRequestStatus status);

    boolean existsByGameListingGameListingIdAndStatusIn(
            Long listingId, List<JoinRequestStatus> statuses);

    @Query("SELECT jr FROM JoinRequest jr JOIN jr.gameListing gl " +
           "WHERE gl.gameListingId = :listingId AND jr.status = 'PENDING' " +
           "AND gl.listingStatus = 'OPEN' AND gl.scheduledDate > :cutoff " +
           "ORDER BY CASE WHEN jr.invitation IS NULL THEN 1 ELSE 0 END, jr.createdAt ASC")
    List<JoinRequest> findPendingForCreator(@Param("listingId") Long listingId,
                                            @Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT jr FROM JoinRequest jr " +
           "JOIN FETCH jr.gameListing gl JOIN FETCH gl.format sf JOIN FETCH sf.sport " +
           "WHERE jr.user.userId = :userId AND jr.status = 'PENDING' " +
           "AND gl.listingStatus = 'OPEN' AND gl.scheduledDate > :cutoff " +
           "ORDER BY gl.scheduledDate ASC")
    List<JoinRequest> findActiveForUser(@Param("userId") Long userId,
                                        @Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT jr.user.userId FROM JoinRequest jr " +
           "WHERE jr.gameListing.gameListingId = :listingId AND jr.status = 'PENDING'")
    List<Long> findPendingUserIds(@Param("listingId") Long listingId);

    @Modifying
    @Query("UPDATE JoinRequest jr SET jr.status = 'EXPIRED', jr.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE jr.gameListing.gameListingId = :listingId AND jr.status = 'PENDING'")
    int expirePendingForListing(@Param("listingId") Long listingId);

    /** Find pending requests that the creator has marked as last-call approved. */
    @Query("SELECT jr FROM JoinRequest jr " +
           "WHERE jr.gameListing.gameListingId = :listingId " +
           "AND jr.status = 'PENDING' AND jr.lastCallApproved = true " +
           "ORDER BY jr.createdAt ASC")
    List<JoinRequest> findLastCallApprovedForListing(@Param("listingId") Long listingId);

    /** Find all pending requests for a listing (regardless of cutoff) for the last-call selection period. */
    @Query("SELECT jr FROM JoinRequest jr JOIN jr.gameListing gl " +
           "WHERE gl.gameListingId = :listingId AND jr.status = 'PENDING' " +
           "AND gl.listingStatus = 'OPEN' " +
           "ORDER BY CASE WHEN jr.invitation IS NULL THEN 1 ELSE 0 END, jr.createdAt ASC")
    List<JoinRequest> findPendingForLastCall(@Param("listingId") Long listingId);
}
