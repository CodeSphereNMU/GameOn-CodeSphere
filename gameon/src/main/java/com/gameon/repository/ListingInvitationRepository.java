package com.gameon.repository;

import com.gameon.model.entity.ListingInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ListingInvitationRepository extends JpaRepository<ListingInvitation, Long> {

    /**
     * Checks if an invitation already exists for this user and listing.
     */
    boolean existsByGameListingGameListingIdAndInvitedUserUserId(Long gameListingId, Long invitedUserId);

    /**
     * Finds an invitation by listing and invited user.
     */
    Optional<ListingInvitation> findByGameListingGameListingIdAndInvitedUserUserId(Long gameListingId, Long invitedUserId);

    /**
     * Gets all invitations for a listing (invitation history).
     */
    List<ListingInvitation> findByGameListingGameListingIdOrderByCreatedAtDesc(Long gameListingId);

    /**
     * Gets all pending invitations for a user.
     */
    List<ListingInvitation> findByInvitedUserUserIdAndStatus(Long userId, String status);

    /**
     * Gets invited user IDs for a listing (for duplicate prevention).
     */
    @Query("SELECT li.invitedUser.userId FROM ListingInvitation li WHERE li.gameListing.gameListingId = :listingId")
    List<Long> findInvitedUserIdsByListingId(@Param("listingId") Long listingId);
}
