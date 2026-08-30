package com.gameon.service;

import com.gameon.exception.BusinessRuleException;
import com.gameon.exception.ResourceNotFoundException;
import com.gameon.exception.UnauthorizedAccessException;
import com.gameon.model.entity.*;
import com.gameon.model.enums.*;
import com.gameon.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Handles join-request history separately from the accepted participant roster.
 * Updated to support the attendance-confirmation lifecycle:
 *   - Confirmation window opens T-24h (or immediately for listings created within 24h).
 *   - Normal withdrawal available before T-2h.
 *   - Late withdrawal available T-2h → T-1h (recorded as late).
 *   - Last-call place claims available T-2h → T-1h for creator-approved requesters.
 *   - Join requests accepted until T-1h (was previously T-2h).
 */
@Service
public class GameJoinerService {

    private static final Logger logger = LoggerFactory.getLogger(GameJoinerService.class);

    private final GameJoinerRepository gameJoinerRepository;
    private final JoinRequestRepository joinRequestRepository;
    private final GameListingRepository gameListingRepository;
    private final UserRepository userRepository;
    private final UserSportProfileRepository userSportProfileRepository;
    private final NotificationService notificationService;
    private final SchedulingConflictService schedulingConflictService;
    private final SportService sportService;
    private final InvitationRepository invitationRepository;
    private final ListingLifecycleService listingLifecycleService;

    public GameJoinerService(GameJoinerRepository gameJoinerRepository,
                             JoinRequestRepository joinRequestRepository,
                             GameListingRepository gameListingRepository,
                             UserRepository userRepository,
                             UserSportProfileRepository userSportProfileRepository,
                             NotificationService notificationService,
                             SchedulingConflictService schedulingConflictService,
                             SportService sportService,
                             InvitationRepository invitationRepository,
                             ListingLifecycleService listingLifecycleService) {
        this.gameJoinerRepository = gameJoinerRepository;
        this.joinRequestRepository = joinRequestRepository;
        this.gameListingRepository = gameListingRepository;
        this.userRepository = userRepository;
        this.userSportProfileRepository = userSportProfileRepository;
        this.notificationService = notificationService;
        this.schedulingConflictService = schedulingConflictService;
        this.sportService = sportService;
        this.invitationRepository = invitationRepository;
        this.listingLifecycleService = listingLifecycleService;
    }

    // ========== Join Request Flow ==========

    @Transactional
    public JoinRequest sendJoinRequest(Long userId, Long listingId, Team team,
                                       Long primaryPositionId, Long alternatePositionId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        GameListing listing = gameListingRepository.findByIdWithDetails(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Game Listing", listingId));

        validateRequestWindowOpen(listing);
        if (listing.getCreator().getUserId().equals(userId)) {
            throw new BusinessRuleException("You are already participating in this listing as the creator.");
        }
        if (gameJoinerRepository.existsAcceptedOrLocked(userId, listingId)) {
            throw new BusinessRuleException("You are already participating in this listing.");
        }
        if (joinRequestRepository.existsByGameListingGameListingIdAndUserUserIdAndStatus(
                listingId, userId, JoinRequestStatus.PENDING)) {
            throw new BusinessRuleException("You already have a pending join request for this listing.");
        }

        Optional<Invitation> invitation = invitationRepository
                .findByGameListingGameListingIdAndInviteeUserId(listingId, userId)
                .filter(item -> item.getStatus() != InvitationStatus.EXPIRED
                        && item.getStatus() != InvitationStatus.DECLINED);

        Long sportId = listing.getFormat().getSport().getSportId();
        if (!userSportProfileRepository.existsByIdUserIdAndIdSportId(userId, sportId)
                && invitation.isEmpty()) {
            throw new BusinessRuleException(
                    "This sport is not included in your sports profile. You must add " +
                    listing.getFormat().getSport().getSportName() +
                    " to your profile before joining this listing.", "BR9");
        }

        validatePositionSelection(listing, primaryPositionId, alternatePositionId);
        String conflictMsg = schedulingConflictService.getConflictMessageMinutes(
                userId, listing.getScheduledDate(), listing.getDurationMinutes(), null);
        if (conflictMsg != null) throw new BusinessRuleException(conflictMsg);

        JoinRequest request = new JoinRequest(user, listing, team);
        request.setPrimaryPositionId(primaryPositionId);
        request.setAlternatePositionId(alternatePositionId);
        invitation.ifPresent(request::setInvitation);
        JoinRequest saved = joinRequestRepository.save(request);

        invitation.ifPresent(item -> {
            item.setStatus(InvitationStatus.USED);
            invitationRepository.save(item);
        });

        String text = user.getUsername() + " wants to join your " +
                listing.getFormat().getSport().getSportName() + " game.";
        notificationService.createNotification(listing.getCreator().getUserId(), text,
                NotificationType.JOIN_REQUEST_RECEIVED, user, listing, saved, null);
        logger.info("Join request {} sent: User {} -> Listing {}", saved.getJoinRequestId(), userId, listingId);
        return saved;
    }

    @Transactional
    public GameJoiner acceptRequest(Long listingId, Long requesterId, Long creatorId) {
        GameListing listing = gameListingRepository.findByIdWithDetails(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Game Listing", listingId));
        verifyCreator(listing, creatorId, "accept requests for");
        validateRequestWindowOpen(listing);
        // During the last-call window (T-2h to T-1h), the creator cannot directly accept a player.
        // Replacements must go through the last-call offer/claim flow so that no spot is reserved
        // and the first eligible claimer takes the available place.
        if (listingLifecycleService.isInLastCallPeriod(listing)) {
            throw new BusinessRuleException(
                    "The game starts within 2 hours. Players can only join now by claiming a last-call offer. "
                    + "Send a last-call notification instead of accepting directly.");
        }
        JoinRequest request = findPendingRequest(listingId, requesterId);

        int maxPlayers = listing.getFormat().getNoPlayers();
        if (countCurrentParticipants(listingId) >= maxPlayers) {
            throw new BusinessRuleException("Cannot accept this request. The listing is already full.");
        }
        if (countTeamParticipants(listingId, request.getTeam()) >= maxPlayers / 2) {
            throw new BusinessRuleException("Cannot accept this request. Team " +
                    request.getTeam().name() + " is already full.");
        }

        String conflictMsg = schedulingConflictService.getConflictMessageMinutes(
                requesterId, listing.getScheduledDate(), listing.getDurationMinutes(), null);
        if (conflictMsg != null) {
            throw new BusinessRuleException("Cannot accept this request. " +
                    request.getUser().getUsername() + " has a scheduling conflict: " + conflictMsg);
        }

        GameJoiner participant = gameJoinerRepository.findByUserAndListing(requesterId, listingId)
                .orElseGet(() -> new GameJoiner(request.getUser(), listing, request.getTeam()));
        participant.setJoinRequest(request);
        participant.setFormat(listing.getFormat());
        participant.setTeam(request.getTeam());
        participant.setPrimaryPositionId(request.getPrimaryPositionId());
        participant.setAlternatePositionId(request.getAlternatePositionId());
        participant.setStatus(JoinerStatus.ACCEPTED);

        request.setStatus(JoinRequestStatus.ACCEPTED);
        joinRequestRepository.save(request);
        GameJoiner saved = gameJoinerRepository.save(participant);

        String text = "Your join request for " + listing.getFormat().getSport().getSportName() +
                " " + listing.getFormat().getFormatName() + " was accepted.";
        notificationService.createNotification(requesterId, text, NotificationType.JOIN_ACCEPTED,
                listing.getCreator(), listing, request, null);
        return saved;
    }

    @Transactional
    public JoinRequest rejectRequest(Long listingId, Long requesterId, Long creatorId) {
        GameListing listing = gameListingRepository.findByIdWithDetails(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Game Listing", listingId));
        verifyCreator(listing, creatorId, "reject requests for");
        validateRequestWindowOpen(listing);
        JoinRequest request = findPendingRequest(listingId, requesterId);
        // A requester who has already been sent a last-call offer is waiting to respond and
        // must not be rejected out from under that offer. Non-offered pending requesters may
        // still be rejected normally, including during the last-call window.
        if (request.isLastCallApproved()) {
            throw new BusinessRuleException(
                    "This requester has an outstanding last-call offer and cannot be rejected while awaiting their response.");
        }
        request.setStatus(JoinRequestStatus.REJECTED);
        JoinRequest saved = joinRequestRepository.save(request);

        String text = "Your join request for " + listing.getFormat().getSport().getSportName() +
                " " + listing.getFormat().getFormatName() + " was declined.";
        notificationService.createNotification(requesterId, text, NotificationType.JOIN_REJECTED,
                listing.getCreator(), listing, request, null);
        return saved;
    }

    // ========== Attendance Confirmation ==========

    /**
     * Player explicitly confirms attendance.
     * Available from T-24h until T-1h for accepted participants.
     */
    @Transactional
    public GameJoiner confirmAttendance(Long userId, Long listingId) {
        GameListing listing = gameListingRepository.findByIdWithDetails(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Game Listing", listingId));

        if (listing.getListingStatus() != ListingStatus.OPEN) {
            throw new BusinessRuleException("Attendance can only be confirmed for open listings.");
        }

        if (!listingLifecycleService.isConfirmationWindowOpen(listing)) {
            throw new BusinessRuleException("Attendance confirmation is not yet available for this listing.");
        }

        GameJoiner joiner = gameJoinerRepository.findByUserAndListing(userId, listingId)
                .orElseThrow(() -> new BusinessRuleException("You are not a participant in this listing."));

        if (joiner.getStatus() == JoinerStatus.LEFT) {
            throw new BusinessRuleException("You are no longer a participant in this listing.");
        }
        if (joiner.getStatus() == JoinerStatus.CONFIRMED_ATTENDANCE || joiner.getStatus() == JoinerStatus.LOCKED) {
            throw new BusinessRuleException("You have already confirmed attendance.");
        }
        if (joiner.getStatus() != JoinerStatus.ACCEPTED) {
            throw new BusinessRuleException("Only accepted participants can confirm attendance.");
        }

        joiner.setStatus(JoinerStatus.CONFIRMED_ATTENDANCE);
        joiner.setAttendanceConfirmedAt(currentTime());
        GameJoiner saved = gameJoinerRepository.save(joiner);

        logger.info("User {} confirmed attendance for listing {}", userId, listingId);

        // Notify creator that a player confirmed
        if (!listing.getCreator().getUserId().equals(userId)) {
            String text = joiner.getUser().getUsername() + " confirmed attendance for your " +
                    listing.getFormat().getSport().getSportName() + " game.";
            notificationService.createNotification(listing.getCreator().getUserId(), text,
                    NotificationType.ATTENDANCE_CONFIRMATION_OPEN, joiner.getUser(), listing, null, null);
        }

        return saved;
    }

    // ========== Withdrawal (Normal + Late) ==========

    /**
     * Player leaves/withdraws from a listing.
     * Before T-2h: normal withdrawal.
     * T-2h → T-1h: late withdrawal (with warning confirmation from UI).
     * After T-1h: withdrawal unavailable.
     *
     * @return true when a pending request was withdrawn; false when a participant left.
     */
    @Transactional
    public boolean leaveListing(Long userId, Long listingId) {
        GameListing listing = gameListingRepository.findByIdWithDetails(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Game Listing", listingId));
        if (listing.getCreator().getUserId().equals(userId)) {
            throw new BusinessRuleException("As the creator, you cannot leave your own listing. Cancel it instead.");
        }

        // Check if past T-1h — no withdrawal allowed
        if (listingLifecycleService.isPastFinalisation(listing)) {
            throw new BusinessRuleException("Withdrawal is no longer available. The participant list has been finalised.");
        }

        // Check if listing is still open
        if (listing.getListingStatus() != ListingStatus.OPEN) {
            throw new BusinessRuleException("You cannot leave a confirmed or cancelled listing.");
        }

        Optional<GameJoiner> participant = gameJoinerRepository.findByUserAndListing(userId, listingId);
        if (participant.isPresent() && participant.get().getStatus() != JoinerStatus.LEFT) {
            if (participant.get().getStatus() == JoinerStatus.LOCKED) {
                throw new BusinessRuleException("You cannot leave a locked listing.", "BR6");
            }

            boolean isLateWithdrawal = listingLifecycleService.isInLastCallPeriod(listing);

            participant.get().setStatus(JoinerStatus.LEFT);
            participant.get().setLateWithdrawal(isLateWithdrawal);
            gameJoinerRepository.save(participant.get());

            String withdrawalType = isLateWithdrawal ? " (late withdrawal)" : "";
            notifyCreatorOfWithdrawal(listing, participant.get().getUser(), null, false, isLateWithdrawal);

            logger.info("User {} left listing {}{}", userId, listingId, withdrawalType);
            return false;
        }

        // Try to withdraw a pending request instead
        JoinRequest request = findPendingRequest(listingId, userId);
        request.setStatus(JoinRequestStatus.WITHDRAWN);
        joinRequestRepository.save(request);
        notifyCreatorOfWithdrawal(listing, request.getUser(), request, true, false);
        return true;
    }

    private void notifyCreatorOfWithdrawal(GameListing listing, User user, JoinRequest request,
                                           boolean requestOnly, boolean isLate) {
        String text;
        if (requestOnly) {
            text = user.getUsername() + " withdrew their join request.";
        } else if (isLate) {
            text = user.getUsername() + " withdrew from your game listing (late withdrawal, within 2 hours of start).";
        } else {
            text = user.getUsername() + " left your game listing.";
        }
        notificationService.createNotification(listing.getCreator().getUserId(), text,
                NotificationType.JOIN_WITHDRAWN, user, listing, request, null);
    }

    // ========== Last-Call Place Claim ==========

    /**
     * Creator selects multiple pending requesters for last-call notification.
     * Marks them as last-call approved so they can claim available places.
     * Only available during T-2h → T-1h when capacity is available.
     */
    @Transactional
    public void approveLastCallRequesters(Long listingId, Long creatorId, List<Long> requesterUserIds) {
        GameListing listing = gameListingRepository.findByIdWithDetails(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Game Listing", listingId));
        verifyCreator(listing, creatorId, "manage last-call for");

        if (!listingLifecycleService.isInLastCallPeriod(listing)) {
            throw new BusinessRuleException("Last-call selection is only available during the replacement period (2h to 1h before start).");
        }

        int maxPlayers = listing.getFormat().getNoPlayers();
        long currentCount = countCurrentParticipants(listingId);
        if (currentCount >= maxPlayers) {
            throw new BusinessRuleException("The listing is already full. No last-call places are available.");
        }

        for (Long requesterId : requesterUserIds) {
            JoinRequest request = joinRequestRepository
                    .findFirstByGameListingGameListingIdAndUserUserIdAndStatusOrderByCreatedAtDesc(
                            listingId, requesterId, JoinRequestStatus.PENDING)
                    .orElse(null);
            if (request == null) {
                continue; // Skip if no pending request
            }

            request.setLastCallApproved(true);
            joinRequestRepository.save(request);

            // Notify the user they have a last-call offer
            String text = "A place has opened in the " + listing.getFormat().getSport().getSportName() +
                    " game at " + listing.getLocation() + " that you requested to join. " +
                    "Claim it now — places are filled on a first-come basis!";
            notificationService.createNotification(requesterId, text,
                    NotificationType.LAST_CALL_OFFER, listing.getCreator(), listing, request, null);
        }

        logger.info("Creator {} approved {} users for last-call on listing {}",
                creatorId, requesterUserIds.size(), listingId);
    }

    /**
     * User claims a last-call place. Available during T-2h → T-1h for last-call-approved requesters.
     * Revalidates eligibility at claim time. Concurrency-safe via capacity check.
     * Successful claim counts as confirmed attendance — no additional confirmation needed.
     */
    @Transactional
    public GameJoiner claimLastCallPlace(Long userId, Long listingId) {
        GameListing listing = gameListingRepository.findByIdWithDetails(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Game Listing", listingId));

        if (listing.getListingStatus() != ListingStatus.OPEN) {
            throw new BusinessRuleException("This listing is no longer accepting players.");
        }

        if (!listingLifecycleService.isInLastCallPeriod(listing)) {
            throw new BusinessRuleException("The last-call period has ended. Places can no longer be claimed.");
        }

        // Verify the user has a last-call-approved pending request
        JoinRequest request = joinRequestRepository
                .findFirstByGameListingGameListingIdAndUserUserIdAndStatusOrderByCreatedAtDesc(
                        listingId, userId, JoinRequestStatus.PENDING)
                .orElseThrow(() -> new BusinessRuleException("You do not have an active request for this listing."));

        if (!request.isLastCallApproved()) {
            throw new BusinessRuleException("You have not been approved for a last-call place in this listing.");
        }

        // Revalidate capacity (concurrency-safe: check count inside transaction)
        int maxPlayers = listing.getFormat().getNoPlayers();
        long currentCount = countCurrentParticipants(listingId);
        if (currentCount >= maxPlayers) {
            // Notify the user that the game filled
            String text = "The " + listing.getFormat().getSport().getSportName() +
                    " game at " + listing.getLocation() + " has already been filled.";
            notificationService.createNotification(userId, text,
                    NotificationType.LAST_CALL_FULL, null, listing, request, null);
            throw new BusinessRuleException("This game has already been filled. No places remain.");
        }

        // Revalidate team capacity
        if (countTeamParticipants(listingId, request.getTeam()) >= maxPlayers / 2) {
            throw new BusinessRuleException("Team " + request.getTeam().name() + " is already full.");
        }

        // Revalidate scheduling conflict
        String conflictMsg = schedulingConflictService.getConflictMessageMinutes(
                userId, listing.getScheduledDate(), listing.getDurationMinutes(), null);
        if (conflictMsg != null) {
            throw new BusinessRuleException("Cannot claim this place: " + conflictMsg);
        }

        // Create or update participant — this counts as confirmed attendance
        GameJoiner participant = gameJoinerRepository.findByUserAndListing(userId, listingId)
                .orElseGet(() -> new GameJoiner(request.getUser(), listing, request.getTeam()));
        participant.setJoinRequest(request);
        participant.setFormat(listing.getFormat());
        participant.setTeam(request.getTeam());
        participant.setPrimaryPositionId(request.getPrimaryPositionId());
        participant.setAlternatePositionId(request.getAlternatePositionId());
        participant.setStatus(JoinerStatus.CONFIRMED_ATTENDANCE);
        participant.setAttendanceConfirmedAt(currentTime());
        participant.setLateWithdrawal(false);

        request.setStatus(JoinRequestStatus.ACCEPTED);
        joinRequestRepository.save(request);
        GameJoiner saved = gameJoinerRepository.save(participant);

        // Notify the user of successful claim
        String claimText = "You successfully claimed a place in the " +
                listing.getFormat().getSport().getSportName() + " game at " + listing.getLocation() + "!";
        notificationService.createNotification(userId, claimText,
                NotificationType.LAST_CALL_CLAIMED, null, listing, request, null);

        // Notify creator
        String creatorText = request.getUser().getUsername() + " claimed a last-call place in your " +
                listing.getFormat().getSport().getSportName() + " game.";
        notificationService.createNotification(listing.getCreator().getUserId(), creatorText,
                NotificationType.LAST_CALL_CLAIMED, request.getUser(), listing, request, null);

        logger.info("User {} claimed last-call place in listing {}", userId, listingId);
        return saved;
    }

    // ========== Request Window Logic ==========

    @Transactional(readOnly = true)
    public void validateJoinAvailability(Long userId, GameListing listing) {
        validateRequestWindowOpen(listing);
        String conflictMsg = schedulingConflictService.getConflictMessageMinutes(
                userId, listing.getScheduledDate(), listing.getDurationMinutes(), null);
        if (conflictMsg != null) throw new BusinessRuleException(conflictMsg);
    }

    /**
     * Join requests are now open until T-1h (was T-2h).
     * Listing must be OPEN and scheduled_date > now + 1h.
     */
    public boolean isRequestWindowOpen(GameListing listing) {
        return listing.getListingStatus() == ListingStatus.OPEN
                && currentTime().isBefore(listing.getScheduledDate().minusHours(
                        ListingLifecycleService.FINALISATION_HOURS));
    }

    private void validateRequestWindowOpen(GameListing listing) {
        if (!isRequestWindowOpen(listing)) {
            throw new BusinessRuleException("Join requests close 1 hour before the game starts.");
        }
    }

    // ========== Utility Methods ==========

    private void verifyCreator(GameListing listing, Long creatorId, String action) {
        if (!listing.getCreator().getUserId().equals(creatorId)) {
            throw new UnauthorizedAccessException(action, "game listing");
        }
    }

    private JoinRequest findPendingRequest(Long listingId, Long userId) {
        return joinRequestRepository
                .findFirstByGameListingGameListingIdAndUserUserIdAndStatusOrderByCreatedAtDesc(
                        listingId, userId, JoinRequestStatus.PENDING)
                .orElseThrow(() -> new ResourceNotFoundException("Pending join request not found"));
    }

    private void validatePositionSelection(GameListing listing, Long primaryId, Long alternateId) {
        if (!listing.getFormat().getHasPositions()) {
            if (primaryId != null || alternateId != null) {
                throw new BusinessRuleException("This format does not use player positions.");
            }
            return;
        }
        if (primaryId == null) {
            if (alternateId != null) {
                throw new BusinessRuleException("Choose a primary position before an alternative position.");
            }
            return;
        }
        if (primaryId.equals(alternateId)) {
            throw new BusinessRuleException("Primary and alternative positions must be different.");
        }
        Set<Long> validIds = sportService.getPositionIdsForFormat(listing.getFormat().getFormatId());
        if (!validIds.contains(primaryId) || (alternateId != null && !validIds.contains(alternateId))) {
            throw new BusinessRuleException("Select positions that belong to this sport format.");
        }
    }

    @Transactional(readOnly = true)
    public List<JoinRequest> getPendingRequests(Long listingId) {
        // During last-call period, show all pending (no cutoff restriction)
        GameListing listing = gameListingRepository.findById(listingId).orElse(null);
        if (listing != null && listingLifecycleService.isInLastCallPeriod(listing)) {
            return joinRequestRepository.findPendingForLastCall(listingId);
        }
        return joinRequestRepository.findPendingForCreator(listingId, requestCutoff());
    }

    @Transactional(readOnly = true)
    public List<JoinRequest> getPendingRequestsForUser(Long userId) {
        return joinRequestRepository.findActiveForUser(userId, requestCutoff());
    }

    @Transactional(readOnly = true)
    public List<GameJoiner> getJoinersByStatus(Long listingId, JoinerStatus status) {
        return gameJoinerRepository.findByIdGameListingIdAndStatus(listingId, status);
    }

    @Transactional(readOnly = true)
    public List<GameJoiner> getTeamMembers(Long listingId, Team team) {
        return gameJoinerRepository.findByIdGameListingIdAndTeamAndStatus(
                listingId, team, JoinerStatus.ACCEPTED);
    }

    @Transactional(readOnly = true)
    public List<GameJoiner> getAllJoiners(Long listingId) {
        return gameJoinerRepository.findByIdGameListingId(listingId);
    }

    @Transactional(readOnly = true)
    public List<GameJoiner> getParticipants(Long listingId) {
        return gameJoinerRepository.findParticipants(listingId);
    }

    @Transactional(readOnly = true)
    public List<GameJoiner> getJoinedListings(Long userId) {
        return gameJoinerRepository.findJoinedListingsForUser(userId, currentTime());
    }

    @Transactional(readOnly = true)
    public boolean hasPendingRequest(Long userId, Long listingId) {
        return joinRequestRepository.existsByGameListingGameListingIdAndUserUserIdAndStatus(
                listingId, userId, JoinRequestStatus.PENDING);
    }

    /**
     * Checks if user has a last-call-approved pending request for this listing.
     */
    @Transactional(readOnly = true)
    public boolean hasLastCallOffer(Long userId, Long listingId) {
        return joinRequestRepository
                .findFirstByGameListingGameListingIdAndUserUserIdAndStatusOrderByCreatedAtDesc(
                        listingId, userId, JoinRequestStatus.PENDING)
                .map(JoinRequest::isLastCallApproved)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean isParticipant(Long userId, Long listingId) {
        return gameJoinerRepository.existsAcceptedOrLocked(userId, listingId);
    }

    @Transactional(readOnly = true)
    public long getTeamCount(Long listingId, Team team) {
        return countTeamParticipants(listingId, team);
    }

    /**
     * Count current active participants (ACCEPTED + CONFIRMED_ATTENDANCE + LOCKED).
     */
    public long countCurrentParticipants(Long listingId) {
        return gameJoinerRepository.countByIdGameListingIdAndStatusIn(
                listingId, List.of(JoinerStatus.ACCEPTED, JoinerStatus.CONFIRMED_ATTENDANCE, JoinerStatus.LOCKED));
    }

    public long countTeamParticipants(Long listingId, Team team) {
        return gameJoinerRepository.countByIdGameListingIdAndTeamAndStatus(
                listingId, team, JoinerStatus.ACCEPTED)
                + gameJoinerRepository.countByIdGameListingIdAndTeamAndStatus(
                listingId, team, JoinerStatus.CONFIRMED_ATTENDANCE)
                + gameJoinerRepository.countByIdGameListingIdAndTeamAndStatus(
                listingId, team, JoinerStatus.LOCKED);
    }

    @Transactional(readOnly = true)
    public boolean isListingFull(Long listingId, int maxPlayers) {
        return countCurrentParticipants(listingId) >= maxPlayers;
    }

    @Transactional(readOnly = true)
    public boolean isTeamFull(Long listingId, Team team, int maxPlayers) {
        return countTeamParticipants(listingId, team) >= maxPlayers / 2;
    }

    /**
     * Whether the confirmation window is open for this listing (for UI display).
     */
    public boolean isConfirmationAvailable(GameListing listing) {
        return listingLifecycleService.isConfirmationWindowOpen(listing);
    }

    /**
     * Whether the listing is in the late withdrawal / last-call period (T-2h → T-1h).
     */
    public boolean isInLastCallPeriod(GameListing listing) {
        return listingLifecycleService.isInLastCallPeriod(listing);
    }

    private LocalDateTime requestCutoff() {
        // Pending requests shown to user are those for listings starting > T-1h from now
        return currentTime().plusHours(ListingLifecycleService.FINALISATION_HOURS);
    }

    LocalDateTime currentTime() {
        return LocalDateTime.now();
    }
}
