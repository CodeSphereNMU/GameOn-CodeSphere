package com.gameon.service;

import com.gameon.exception.ResourceNotFoundException;
import com.gameon.model.entity.GameJoiner;
import com.gameon.model.entity.GameListing;
import com.gameon.model.enums.InvitationStatus;
import com.gameon.model.enums.ListingStatus;
import com.gameon.model.enums.NotificationType;
import com.gameon.repository.GameJoinerRepository;
import com.gameon.repository.GameListingRepository;
import com.gameon.repository.InvitationRepository;
import com.gameon.repository.JoinRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Applies the two-hour lock-in rules directly to a game listing. */
@Service
public class ListingLifecycleService {

    private static final Logger logger = LoggerFactory.getLogger(ListingLifecycleService.class);
    private static final int LOCK_IN_HOURS_BEFORE_START = 2;

    private final GameListingRepository gameListingRepository;
    private final GameJoinerRepository gameJoinerRepository;
    private final JoinRequestRepository joinRequestRepository;
    private final InvitationRepository invitationRepository;
    private final NotificationService notificationService;

    public ListingLifecycleService(GameListingRepository gameListingRepository,
                                   GameJoinerRepository gameJoinerRepository,
                                   JoinRequestRepository joinRequestRepository,
                                   InvitationRepository invitationRepository,
                                   NotificationService notificationService) {
        this.gameListingRepository = gameListingRepository;
        this.gameJoinerRepository = gameJoinerRepository;
        this.joinRequestRepository = joinRequestRepository;
        this.invitationRepository = invitationRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<GameListing> findListingsNeedingLockIn() {
        return gameListingRepository.findListingsNeedingLockIn(
                LocalDateTime.now().plusHours(LOCK_IN_HOURS_BEFORE_START));
    }

    @Transactional
    public void lockInListing(Long listingId) {
        GameListing listing = gameListingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Game Listing", listingId));
        if (listing.getListingStatus() != ListingStatus.OPEN) {
            return;
        }

        List<GameJoiner> participants = gameJoinerRepository.findParticipants(listingId);
        Set<Long> pendingUserIds = new LinkedHashSet<>(joinRequestRepository.findPendingUserIds(listingId));
        invitationRepository.findByGameListingGameListingIdAndStatus(listingId, InvitationStatus.PENDING)
                .forEach(invitation -> pendingUserIds.add(invitation.getInvitee().getUserId()));

        int requiredPlayers = listing.getFormat().getNoPlayers();
        if (participants.size() >= requiredPlayers) {
            listing.setListingStatus(ListingStatus.CONFIRMED);
            gameJoinerRepository.lockAllAcceptedJoiners(listingId);
            joinRequestRepository.expirePendingForListing(listingId);
            invitationRepository.expirePendingForListing(listingId);
            gameListingRepository.save(listing);

            List<Long> participantIds = participants.stream()
                    .map(joiner -> joiner.getUser().getUserId())
                    .distinct()
                    .toList();
            String text = "Your " + listing.getFormat().getSport().getSportName()
                    + " game at " + listing.getLocation() + " is confirmed and starts in about 2 hours.";
            notificationService.createBulkNotifications(participantIds, text,
                    NotificationType.LISTING_CONFIRMED, null, listing, null, null);
            logger.info("Listing {} confirmed and {} participants locked", listingId, participants.size());
            return;
        }

        listing.setListingStatus(ListingStatus.CANCELLED_INSUFFICIENT_PLAYERS);
        joinRequestRepository.expirePendingForListing(listingId);
        invitationRepository.expirePendingForListing(listingId);
        gameListingRepository.save(listing);

        Set<Long> recipientIds = new LinkedHashSet<>(pendingUserIds);
        participants.stream().map(joiner -> joiner.getUser().getUserId()).forEach(recipientIds::add);
        String text = "The " + listing.getFormat().getSport().getSportName()
                + " game at " + listing.getLocation() + " was cancelled because it did not have enough players.";
        notificationService.createBulkNotifications(List.copyOf(recipientIds), text,
                NotificationType.LISTING_CANCELLED_INSUFFICIENT_PLAYERS, null, listing, null, null);
        logger.info("Listing {} cancelled at lock-in with {}/{} participants",
                listingId, participants.size(), requiredPlayers);
    }
}
