package com.gameon.service;

import com.gameon.exception.BusinessRuleException;
import com.gameon.exception.ResourceNotFoundException;
import com.gameon.exception.UnauthorizedAccessException;
import com.gameon.model.entity.GameListing;
import com.gameon.model.entity.ListingInvitation;
import com.gameon.model.entity.User;
import com.gameon.model.enums.JoinerStatus;
import com.gameon.model.enums.NotificationType;
import com.gameon.repository.GameJoinerRepository;
import com.gameon.repository.GameListingRepository;
import com.gameon.repository.ListingInvitationRepository;
import com.gameon.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Service handling additional invitations after listing creation.
 * Allows creators to invite more friends while listing remains OPEN.
 *
 * Rules:
 * - Only listing creator can invite
 * - Listing must be OPEN (not completed, no session confirmed)
 * - Prevents duplicate invitations
 * - Prevents inviting users already participating (ACCEPTED/LOCKED/PENDING)
 * - Maintains invitation history
 */
@Service
public class InvitationService {

    private static final Logger logger = LoggerFactory.getLogger(InvitationService.class);

    private final ListingInvitationRepository invitationRepository;
    private final GameListingRepository gameListingRepository;
    private final GameJoinerRepository gameJoinerRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final SessionService sessionService;

    public InvitationService(ListingInvitationRepository invitationRepository,
                             GameListingRepository gameListingRepository,
                             GameJoinerRepository gameJoinerRepository,
                             UserRepository userRepository,
                             NotificationService notificationService,
                             SessionService sessionService) {
        this.invitationRepository = invitationRepository;
        this.gameListingRepository = gameListingRepository;
        this.gameJoinerRepository = gameJoinerRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.sessionService = sessionService;
    }

    /**
     * Sends additional invitations to friends for an OPEN listing.
     * Only the listing creator can invite. Prevents duplicates and
     * inviting users who are already participating.
     *
     * @param listingId      the listing to invite to
     * @param creatorId      the creator performing the invite
     * @param invitedUserIds list of user IDs to invite
     * @return number of invitations successfully sent
     */
    @Transactional
    public int sendInvitations(Long listingId, Long creatorId, List<Long> invitedUserIds) {
        GameListing listing = gameListingRepository.findByIdWithDetails(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Game Listing", listingId));

        // Only creator can invite
        if (!listing.getCreator().getUserId().equals(creatorId)) {
            throw new UnauthorizedAccessException("invite players to", "game listing");
        }

        // Listing must be OPEN (not completed, no session confirmed)
        validateListingIsOpen(listing);

        User creator = listing.getCreator();
        int sentCount = 0;
        List<String> skippedReasons = new ArrayList<>();

        for (Long userId : invitedUserIds) {
            if (userId.equals(creatorId)) {
                continue; // Can't invite yourself
            }

            // Check if user already has a pending/active invitation
            if (invitationRepository.existsByGameListingGameListingIdAndInvitedUserUserId(listingId, userId)) {
                skippedReasons.add("User " + userId + " already invited");
                continue;
            }

            // Check if user is already participating (ACCEPTED, LOCKED, or PENDING join request)
            if (gameJoinerRepository.existsPendingRequest(userId, listingId) ||
                gameJoinerRepository.existsAcceptedOrLocked(userId, listingId)) {
                skippedReasons.add("User " + userId + " already participating or has pending request");
                continue;
            }

            // Send invitation
            User invitedUser = userRepository.findById(userId).orElse(null);
            if (invitedUser == null) {
                continue;
            }

            ListingInvitation invitation = new ListingInvitation(listing, invitedUser, creator);
            invitationRepository.save(invitation);

            // Send notification
            String notifText = creator.getUsername() + " invited you to a " +
                    listing.getFormat().getSport().getSportName() + " " +
                    listing.getFormat().getFormatName() + " game on " +
                    listing.getScheduledDate().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM HH:mm")) +
                    ". Submit a join request to participate.";
            notificationService.createNotification(userId, notifText, NotificationType.LISTING_INVITE);

            sentCount++;
            logger.info("Invitation sent: User {} invited to listing {} by {}", userId, listingId, creatorId);
        }

        if (!skippedReasons.isEmpty()) {
            logger.debug("Skipped invitations for listing {}: {}", listingId, skippedReasons);
        }

        return sentCount;
    }

    /**
     * Gets the invitation history for a listing.
     */
    @Transactional(readOnly = true)
    public List<ListingInvitation> getInvitationHistory(Long listingId) {
        return invitationRepository.findByGameListingGameListingIdOrderByCreatedAtDesc(listingId);
    }

    /**
     * Gets user IDs that have already been invited to a listing.
     */
    @Transactional(readOnly = true)
    public List<Long> getAlreadyInvitedUserIds(Long listingId) {
        return invitationRepository.findInvitedUserIdsByListingId(listingId);
    }

    /**
     * Checks if a listing is in OPEN state (eligible for additional invitations).
     * OPEN = not completed AND no session confirmed (not CONFIRMED/IN_PROGRESS/COMPLETED/CANCELLED)
     */
    @Transactional(readOnly = true)
    public boolean isListingOpen(GameListing listing) {
        if (listing.getIsCompleted()) {
            return false;
        }
        // Check if a session has been confirmed (CONFIRMED state)
        return !sessionService.isSessionConfirmed(listing.getGameListingId());
    }

    /**
     * Validates that a listing is in OPEN state for invitations.
     */
    private void validateListingIsOpen(GameListing listing) {
        if (listing.getIsCompleted()) {
            throw new BusinessRuleException("Cannot invite players to a completed listing.");
        }
        if (sessionService.isSessionConfirmed(listing.getGameListingId())) {
            throw new BusinessRuleException("Cannot invite players after the session has been confirmed.");
        }
    }
}
