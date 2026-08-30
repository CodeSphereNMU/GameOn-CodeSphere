package com.gameon.service;

import com.gameon.exception.ResourceNotFoundException;
import com.gameon.model.entity.GameJoiner;
import com.gameon.model.entity.GameListing;
import com.gameon.model.enums.InvitationStatus;
import com.gameon.model.enums.JoinerStatus;
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

/**
 * Applies the attendance-confirmation lifecycle rules to game listings.
 *
 * Phase 1 — T-2h (Confirmation Deadline):
 *   - Release unconfirmed accepted participants (except creator).
 *   - Creator not yet confirmed: retain, but send urgent warning.
 *   - Do NOT cancel the listing yet — enter replacement period.
 *
 * Phase 2 — T-1h (Finalisation):
 *   - If required capacity is met by confirmed participants AND creator is confirmed: CONFIRM listing.
 *   - If creator still unconfirmed: cancel (CANCELLED_CREATOR_UNCONFIRMED uses existing CANCELLED_INSUFFICIENT_PLAYERS).
 *   - If capacity not met: cancel for insufficient players.
 *   - Lock all confirmed participants.
 *   - Expire remaining pending requests and invitations.
 *   - No further join requests accepted; listing removed from Browse.
 */
@Service
public class ListingLifecycleService {

    private static final Logger logger = LoggerFactory.getLogger(ListingLifecycleService.class);
    public static final int CONFIRMATION_DEADLINE_HOURS = 2;
    public static final int FINALISATION_HOURS = 1;
    public static final int CONFIRMATION_WINDOW_HOURS = 24;

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

    // ========== Phase 1: T-2h Confirmation Deadline ==========

    /**
     * Finds OPEN listings that have reached the T-2h confirmation deadline
     * but have NOT yet reached T-1h (those go through finalisation instead).
     */
    @Transactional(readOnly = true)
    public List<GameListing> findListingsNeedingConfirmationDeadline() {
        LocalDateTime now = currentTime();
        LocalDateTime twoHoursFromNow = now.plusHours(CONFIRMATION_DEADLINE_HOURS);
        LocalDateTime oneHourFromNow = now.plusHours(FINALISATION_HOURS);
        // Need listings where scheduled_date <= now + 2h AND scheduled_date > now + 1h
        // i.e. they are in the T-2h window but have not yet reached T-1h
        return gameListingRepository.findListingsNeedingLockIn(twoHoursFromNow).stream()
                .filter(gl -> gl.getScheduledDate().isAfter(oneHourFromNow))
                .toList();
    }

    /**
     * Finds OPEN listings that have reached (or passed) the T-1h finalisation point.
     */
    @Transactional(readOnly = true)
    public List<GameListing> findListingsNeedingFinalisation() {
        return gameListingRepository.findListingsNeedingFinalisation(
                currentTime().plusHours(FINALISATION_HOURS));
    }

    /** Legacy method kept for backward compatibility with existing callers. */
    @Transactional(readOnly = true)
    public List<GameListing> findListingsNeedingLockIn() {
        return gameListingRepository.findListingsNeedingLockIn(
                currentTime().plusHours(CONFIRMATION_DEADLINE_HOURS));
    }

    /**
     * Phase 1: Process the T-2h confirmation deadline for a listing.
     * - Releases unconfirmed accepted participants (except creator).
     * - Sends urgent warning to creator if they haven't confirmed.
     * - Does NOT cancel or confirm the listing (that happens at T-1h).
     */
    @Transactional
    public void processConfirmationDeadline(Long listingId) {
        GameListing listing = gameListingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Game Listing", listingId));
        if (listing.getListingStatus() != ListingStatus.OPEN) {
            return;
        }

        // Already past T-1h? Skip deadline processing and let finalisation handle it.
        if (!listing.getScheduledDate().isAfter(currentTime().plusHours(FINALISATION_HOURS))) {
            return;
        }

        Long creatorId = listing.getCreator().getUserId();
        List<GameJoiner> unconfirmed = gameJoinerRepository.findUnconfirmedAccepted(listingId);

        int releasedCount = 0;
        boolean creatorUnconfirmed = false;

        for (GameJoiner joiner : unconfirmed) {
            if (joiner.getUser().getUserId().equals(creatorId)) {
                // Creator gets a grace period until T-1h — do not release, but warn
                creatorUnconfirmed = true;
                continue;
            }
            // Release unconfirmed participant
            joiner.setStatus(JoinerStatus.LEFT);
            gameJoinerRepository.save(joiner);
            releasedCount++;

            // Notify the player that their place was released
            String text = "Your place in the " + listing.getFormat().getSport().getSportName() +
                    " game at " + listing.getLocation() + " was released because you did not confirm attendance by the deadline.";
            notificationService.createNotification(joiner.getUser().getUserId(), text,
                    NotificationType.PLACE_RELEASED_UNCONFIRMED, null, listing, null, null);
        }

        // Send urgent warning to creator if they haven't confirmed
        if (creatorUnconfirmed) {
            String urgentText = "URGENT: You must confirm attendance for your " +
                    listing.getFormat().getSport().getSportName() + " game at " + listing.getLocation() +
                    " before the 1-hour finalisation deadline, or the listing will be automatically cancelled.";
            notificationService.createNotification(creatorId, urgentText,
                    NotificationType.CREATOR_CONFIRMATION_URGENT, null, listing, null, null);
        }

        logger.info("Listing {} confirmation deadline processed: {} unconfirmed players released, creator unconfirmed={}",
                listingId, releasedCount, creatorUnconfirmed);
    }

    // ========== Phase 2: T-1h Finalisation ==========

    /**
     * Phase 2: Finalise the listing at T-1h.
     * - If all required places are filled by confirmed participants AND creator confirmed: CONFIRM.
     * - If creator not confirmed: cancel.
     * - If capacity insufficient: cancel.
     * - Lock all confirmed participants.
     * - Expire outstanding pending requests/invitations.
     */
    @Transactional
    public void finaliseListing(Long listingId) {
        GameListing listing = gameListingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Game Listing", listingId));
        if (listing.getListingStatus() != ListingStatus.OPEN) {
            return;
        }

        Long creatorId = listing.getCreator().getUserId();
        int requiredPlayers = listing.getFormat().getNoPlayers();

        // First, release any remaining unconfirmed participants (shouldn't be many if T-2h ran,
        // but handles edge cases where T-2h was missed or listing was created within 2h)
        List<GameJoiner> unconfirmed = gameJoinerRepository.findUnconfirmedAccepted(listingId);
        for (GameJoiner joiner : unconfirmed) {
            if (!joiner.getUser().getUserId().equals(creatorId)) {
                joiner.setStatus(JoinerStatus.LEFT);
                gameJoinerRepository.save(joiner);
                String text = "Your place in the " + listing.getFormat().getSport().getSportName() +
                        " game at " + listing.getLocation() + " was released because you did not confirm attendance.";
                notificationService.createNotification(joiner.getUser().getUserId(), text,
                        NotificationType.PLACE_RELEASED_UNCONFIRMED, null, listing, null, null);
            }
        }

        // Check creator confirmation
        GameJoiner creatorJoiner = gameJoinerRepository.findByUserAndListing(creatorId, listingId)
                .orElse(null);
        boolean creatorConfirmed = creatorJoiner != null &&
                (creatorJoiner.getStatus() == JoinerStatus.CONFIRMED_ATTENDANCE
                        || creatorJoiner.getStatus() == JoinerStatus.LOCKED);

        if (!creatorConfirmed) {
            // Creator did not confirm — cancel listing
            listing.setListingStatus(ListingStatus.CANCELLED_INSUFFICIENT_PLAYERS);
            joinRequestRepository.expirePendingForListing(listingId);
            invitationRepository.expirePendingForListing(listingId);
            gameListingRepository.save(listing);

            // Notify all affected users
            Set<Long> recipientIds = new LinkedHashSet<>();
            gameJoinerRepository.findConfirmedParticipants(listingId).stream()
                    .map(gj -> gj.getUser().getUserId())
                    .forEach(recipientIds::add);
            joinRequestRepository.findPendingUserIds(listingId).forEach(recipientIds::add);

            String text = "The " + listing.getFormat().getSport().getSportName() +
                    " game at " + listing.getLocation() +
                    " was cancelled because the creator did not confirm attendance.";
            notificationService.createBulkNotifications(List.copyOf(recipientIds), text,
                    NotificationType.LISTING_CANCELLED_INSUFFICIENT_PLAYERS, null, listing, null, null);
            // Also notify creator
            notificationService.createNotification(creatorId,
                    "Your " + listing.getFormat().getSport().getSportName() +
                            " listing was automatically cancelled because you did not confirm attendance by the deadline.",
                    NotificationType.LISTING_CANCELLED_INSUFFICIENT_PLAYERS, null, listing, null, null);

            logger.info("Listing {} cancelled at finalisation: creator did not confirm", listingId);
            return;
        }

        // Count confirmed participants (those with CONFIRMED_ATTENDANCE status, plus the creator if confirmed)
        List<GameJoiner> confirmedParticipants = gameJoinerRepository.findConfirmedParticipants(listingId);
        // Include locked participants (from last-call claims which are already confirmed)
        long confirmedCount = confirmedParticipants.size();
        // Creator is CONFIRMED_ATTENDANCE so already counted in confirmedParticipants

        if (confirmedCount >= requiredPlayers) {
            // Listing is CONFIRMED
            listing.setListingStatus(ListingStatus.CONFIRMED);
            gameJoinerRepository.lockAllConfirmedJoiners(listingId);
            joinRequestRepository.expirePendingForListing(listingId);
            invitationRepository.expirePendingForListing(listingId);
            gameListingRepository.save(listing);

            List<Long> participantIds = confirmedParticipants.stream()
                    .map(gj -> gj.getUser().getUserId())
                    .distinct()
                    .toList();
            String text = "Your " + listing.getFormat().getSport().getSportName() +
                    " game at " + listing.getLocation() + " is confirmed and starts in about 1 hour.";
            notificationService.createBulkNotifications(participantIds, text,
                    NotificationType.LISTING_CONFIRMED, null, listing, null, null);

            logger.info("Listing {} confirmed at finalisation with {}/{} participants",
                    listingId, confirmedCount, requiredPlayers);
        } else {
            // Insufficient players — cancel
            listing.setListingStatus(ListingStatus.CANCELLED_INSUFFICIENT_PLAYERS);
            joinRequestRepository.expirePendingForListing(listingId);
            invitationRepository.expirePendingForListing(listingId);
            gameListingRepository.save(listing);

            Set<Long> recipientIds = new LinkedHashSet<>();
            confirmedParticipants.stream()
                    .map(gj -> gj.getUser().getUserId())
                    .forEach(recipientIds::add);
            joinRequestRepository.findPendingUserIds(listingId).forEach(recipientIds::add);
            invitationRepository.findByGameListingGameListingIdAndStatus(listingId, InvitationStatus.PENDING)
                    .forEach(inv -> recipientIds.add(inv.getInvitee().getUserId()));

            String text = "The " + listing.getFormat().getSport().getSportName() +
                    " game at " + listing.getLocation() +
                    " was cancelled because it did not have enough confirmed players.";
            notificationService.createBulkNotifications(List.copyOf(recipientIds), text,
                    NotificationType.LISTING_CANCELLED_INSUFFICIENT_PLAYERS, null, listing, null, null);

            logger.info("Listing {} cancelled at finalisation with {}/{} confirmed participants",
                    listingId, confirmedCount, requiredPlayers);
        }
    }

    // ========== Legacy lockInListing (redirects to new lifecycle) ==========

    /**
     * Legacy entry point preserved for SchedulingConfig compatibility.
     * Routes to the appropriate phase based on time remaining.
     */
    @Transactional
    public void lockInListing(Long listingId) {
        GameListing listing = gameListingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Game Listing", listingId));
        if (listing.getListingStatus() != ListingStatus.OPEN) {
            return;
        }

        LocalDateTime now = currentTime();
        LocalDateTime scheduledDate = listing.getScheduledDate();

        if (!scheduledDate.isAfter(now.plusHours(FINALISATION_HOURS))) {
            // Past T-1h — finalise
            finaliseListing(listingId);
        } else if (!scheduledDate.isAfter(now.plusHours(CONFIRMATION_DEADLINE_HOURS))) {
            // Past T-2h but before T-1h — process confirmation deadline
            processConfirmationDeadline(listingId);
        }
        // else not yet at T-2h, nothing to do
    }

    // ========== Utility Methods ==========

    /**
     * Checks whether attendance confirmation is currently available for a listing.
     * Available from T-24h (or immediately if listing was created within 24h of start).
     */
    public boolean isConfirmationWindowOpen(GameListing listing) {
        if (listing.getListingStatus() != ListingStatus.OPEN) {
            return false;
        }
        LocalDateTime now = currentTime();
        LocalDateTime scheduledDate = listing.getScheduledDate();
        // Confirmation closes at T-1h (finalisation)
        if (!scheduledDate.isAfter(now.plusHours(FINALISATION_HOURS))) {
            return false;
        }
        // Confirmation opens at T-24h
        LocalDateTime confirmationOpens = scheduledDate.minusHours(CONFIRMATION_WINDOW_HOURS);
        return !now.isBefore(confirmationOpens);
    }

    /**
     * Checks whether the listing is currently in the late withdrawal / last-call period (T-2h → T-1h).
     */
    public boolean isInLastCallPeriod(GameListing listing) {
        if (listing.getListingStatus() != ListingStatus.OPEN) {
            return false;
        }
        LocalDateTime now = currentTime();
        LocalDateTime scheduledDate = listing.getScheduledDate();
        LocalDateTime tMinus2h = scheduledDate.minusHours(CONFIRMATION_DEADLINE_HOURS);
        LocalDateTime tMinus1h = scheduledDate.minusHours(FINALISATION_HOURS);
        return !now.isBefore(tMinus2h) && now.isBefore(tMinus1h);
    }

    /**
     * Checks whether the listing has passed the T-2h confirmation deadline.
     */
    public boolean isPastConfirmationDeadline(GameListing listing) {
        LocalDateTime now = currentTime();
        return !listing.getScheduledDate().isAfter(now.plusHours(CONFIRMATION_DEADLINE_HOURS));
    }

    /**
     * Checks whether the listing has passed T-1h (finalised or should be).
     */
    public boolean isPastFinalisation(GameListing listing) {
        LocalDateTime now = currentTime();
        return !listing.getScheduledDate().isAfter(now.plusHours(FINALISATION_HOURS));
    }

    LocalDateTime currentTime() {
        return LocalDateTime.now();
    }
}
