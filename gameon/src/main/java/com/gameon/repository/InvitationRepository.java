package com.gameon.repository;

import com.gameon.model.entity.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import com.gameon.model.enums.InvitationStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    boolean existsByGameListingGameListingIdAndInviteeUserId(Long listingId, Long userId);
    boolean existsByGameListingGameListingId(Long listingId);
    Optional<Invitation> findByGameListingGameListingIdAndInviteeUserId(Long listingId, Long userId);
    List<Invitation> findByInviteeUserIdOrderByCreatedAtDesc(Long userId);
    List<Invitation> findByGameListingGameListingIdAndStatus(Long listingId, InvitationStatus status);

    @Modifying
    @Query("UPDATE Invitation i SET i.status = 'EXPIRED', i.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE i.gameListing.gameListingId = :listingId AND i.status = 'PENDING'")
    int expirePendingForListing(@Param("listingId") Long listingId);
}
