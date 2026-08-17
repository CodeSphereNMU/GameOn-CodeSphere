package com.gameon.service;

import com.gameon.exception.BusinessRuleException;
import com.gameon.exception.ResourceNotFoundException;
import com.gameon.exception.UnauthorizedAccessException;
import com.gameon.model.entity.GameJoiner;
import com.gameon.model.entity.GameJoinerId;
import com.gameon.model.entity.GameListing;
import com.gameon.model.entity.User;
import com.gameon.model.enums.JoinerStatus;
import com.gameon.model.enums.NotificationType;
import com.gameon.model.enums.Team;
import com.gameon.repository.GameJoinerRepository;
import com.gameon.repository.GameListingRepository;
import com.gameon.repository.UserRepository;
import com.gameon.repository.UserSportProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service handling join requests for game listings.
 * Covers A300 (Send Join Request), A400 (Leave), C500 (View/Accept/Reject Requests).
 *
 * Business Rules enforced:
 * - BR9: User can only join listing if sport is on profile
 * - Scheduling conflict: 60-minute travel buffer between sessions
 * - One active pending request per user per listing
 * - Cannot join own listing (creator is already a participant)
 * - Invitations do NOT auto-accept
 */
@Service
public class GameJoinerService {

    private static final Logger logger = LoggerFactory.getLogger(GameJoinerService.class);

    private final GameJoinerRepository gameJoinerRepository;
    private final GameListingRepository gameListingRepository;
    private final UserRepository userRepository;
    private final UserSportProfileRepository userSportProfileRepository;
    private final NotificationService notificationService;
    private final SchedulingConflictService schedulingConflictService;
    private final SportService sportService;
    private final InvitationService invitationService;

    public GameJoinerService(GameJoinerRepository gameJoinerRepository,
                             GameListingRepository gameListingRepository,
                             UserRepository userRepository,
                             UserSportProfileRepository userSportProfileRepository,
                             NotificationService notificationService,
                             SchedulingConflictService schedulingConflictService,
                             SportService sportService,
                             InvitationService invitationService) {
        this.gameJoinerRepository = gameJoinerRepository;
        this.gameListingRepository = gameListingRepository;
        this.userRepository = userRepository;
        this.userSportProfileRepository = userSportProfileRepository;
        this.notificationService = notificationService;
        this.schedulingConflictService = schedulingConflictService;
        this.sportService = sportService;
        this.invitationService = invitationService;
    }

    /**
     * Sends a join request for a game listing (A300).
     * Validation order:
     * 1. Listing exists and is joinable
     * 2. Cannot join own listing
     * 3. BR9: Sport must be on user's profile
     * 4. User does not already participate (ACCEPTED/LOCKED)
     * 5. User does not have an active PENDING request
     * 6. Scheduling conflict check with 60-min travel buffer
     * Capacity is deliberately checked only when a creator accepts the request.
     */
    @Transactional
    public GameJoiner sendJoinRequest(Long userId, Long listingId, Team team,
                                      Long formatPositionId, Long altFormatPositionId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        GameListing listing = gameListingRepository.findByIdWithDetails(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Game Listing", listingId));

        // Cannot join completed listing
        if (listing.getIsCompleted()) {
            throw new BusinessRuleException("This listing is no longer active.");
        }

        validateRequestWindowOpen(listing);

        // Cannot join own listing (creator is already a participant)
        if (listing.getCreator().getUserId().equals(userId)) {
            throw new BusinessRuleException("You are already participating in this listing as the creator.");
        }

        // BR9: Sport must be on profile
        Long sportId = listing.getFormat().getSport().getSportId();
        if (!userSportProfileRepository.existsByIdUserIdAndIdSportId(userId, sportId)
                && !invitationService.isInvited(listingId, userId)) {
            throw new BusinessRuleException(
                    "This sport is not included in your sports profile. " +
                    "You must add " + listing.getFormat().getSport().getSportName() +
                    " to your profile before joining this listing.", "BR9");
        }

        // Check existing joiner record (duplicate participant prevention)
        Optional<GameJoiner> existingJoiner = gameJoinerRepository.findByUserAndListing(userId, listingId);
        if (existingJoiner.isPresent()) {
            GameJoiner existing = existingJoiner.get();
            switch (existing.getStatus()) {
                case ACCEPTED, LOCKED:
                    throw new BusinessRuleException("You are already participating in this listing.");
                case PENDING:
                    throw new BusinessRuleException("You already have a pending join request for this listing.");
                case REJECTED, LEFT:
                    // Allow re-request: delete old record and create new one
                    gameJoinerRepository.delete(existing);
                    gameJoinerRepository.flush();
                    break;
            }
        }

        validatePositionSelection(listing, formatPositionId, altFormatPositionId);

        // Scheduling conflict check with 60-minute travel buffer
        int duration = listing.getSessionDuration() != null ? listing.getSessionDuration() : 1;
        String conflictMsg = schedulingConflictService.getConflictMessage(
                userId, listing.getScheduledDate(), duration, null);
        if (conflictMsg != null) {
            throw new BusinessRuleException(conflictMsg);
        }

        // Create join request
        GameJoiner joiner = new GameJoiner(user, listing, team);
        joiner.setFormatPositionId(formatPositionId);
        joiner.setAltFormatPositionId(altFormatPositionId);

        GameJoiner saved = gameJoinerRepository.save(joiner);

        // Notify listing creator
        String notifText = user.getUsername() + " wants to join your " +
                listing.getFormat().getSport().getSportName() + " game.";
        notificationService.createNotification(
                listing.getCreator().getUserId(), notifText, NotificationType.JOIN_REQUEST_RECEIVED);

        logger.info("Join request sent: User {} → Listing {} (Team {})", userId, listingId, team);
        return saved;
    }

    /**
     * Accepts a join request (C500). Only listing creator can accept.
     * Validates:
     * - Scheduling conflict for the joiner at acceptance time
     * - Listing capacity (total players)
     * - Team capacity (per-team limit)
     * - User is not already a participant (prevents duplicates)
     */
    @Transactional
    public GameJoiner acceptRequest(Long listingId, Long joinerId, Long creatorId) {
        GameListing listing = gameListingRepository.findByIdWithDetails(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Game Listing", listingId));

        if (!listing.getCreator().getUserId().equals(creatorId)) {
            throw new UnauthorizedAccessException("accept requests for", "game listing");
        }

        validateRequestWindowOpen(listing);

        GameJoiner joiner = gameJoinerRepository.findById(new GameJoinerId(joinerId, listingId))
                .orElseThrow(() -> new ResourceNotFoundException("Join request not found"));

        if (joiner.getStatus() != JoinerStatus.PENDING) {
            throw new BusinessRuleException("This request has already been processed.");
        }

        // Capacity check: listing must not be full
        int maxPlayers = listing.getFormat().getNoPlayers();
        long currentParticipants = countCurrentParticipants(listingId);
        if (currentParticipants >= maxPlayers) {
            throw new BusinessRuleException(
                    "Cannot accept this request. The listing is already full (" +
                    currentParticipants + "/" + maxPlayers + " players).");
        }

        // Team capacity check: the joiner's team must not be full
        int teamCapacity = maxPlayers / 2;
        long teamCount = countTeamParticipants(listingId, joiner.getTeam());
        if (teamCount >= teamCapacity) {
            throw new BusinessRuleException(
                    "Cannot accept this request. Team " + joiner.getTeam().name() +
                    " is already full (" + teamCount + "/" + teamCapacity + " players).");
        }

        // Re-validate scheduling conflict at acceptance time (situation may have changed)
        int duration = listing.getSessionDuration() != null ? listing.getSessionDuration() : 1;
        String conflictMsg = schedulingConflictService.getConflictMessage(
                joinerId, listing.getScheduledDate(), duration, null);
        if (conflictMsg != null) {
            throw new BusinessRuleException(
                    "Cannot accept this request. " + joiner.getUser().getUsername() +
                    " has a scheduling conflict: " + conflictMsg);
        }

        joiner.setStatus(JoinerStatus.ACCEPTED);
        GameJoiner saved = gameJoinerRepository.save(joiner);

        // Notify joiner
        String notifText = "Your join request for " + listing.getFormat().getSport().getSportName() +
                " " + listing.getFormat().getFormatName() + " was accepted!";
        notificationService.createNotification(joinerId, notifText, NotificationType.JOIN_ACCEPTED);

        logger.info("Join request accepted: User {} → Listing {}", joinerId, listingId);
        return saved;
    }

    /**
     * Rejects a join request (C500). Only listing creator can reject.
     */
    @Transactional
    public GameJoiner rejectRequest(Long listingId, Long joinerId, Long creatorId) {
        GameListing listing = gameListingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Game Listing", listingId));

        if (!listing.getCreator().getUserId().equals(creatorId)) {
            throw new UnauthorizedAccessException("reject requests for", "game listing");
        }

        validateRequestWindowOpen(listing);

        GameJoiner joiner = gameJoinerRepository.findById(new GameJoinerId(joinerId, listingId))
                .orElseThrow(() -> new ResourceNotFoundException("Join request not found"));

        if (joiner.getStatus() != JoinerStatus.PENDING) {
            throw new BusinessRuleException("This request has already been processed.");
        }

        joiner.setStatus(JoinerStatus.REJECTED);
        GameJoiner saved = gameJoinerRepository.save(joiner);

        // Notify joiner
        String notifText = "Your join request for " + listing.getFormat().getSport().getSportName() +
                " " + listing.getFormat().getFormatName() + " was declined.";
        notificationService.createNotification(joinerId, notifText, NotificationType.JOIN_REJECTED);

        logger.info("Join request rejected: User {} → Listing {}", joinerId, listingId);
        return saved;
    }

    /**
     * Leaves a game listing (A400). Only accepted/pending joiners can leave.
     * Locked joiners cannot leave (BR6).
     * Creator cannot leave their own listing.
     */
    @Transactional
    public void leaveListing(Long userId, Long listingId) {
        GameJoiner joiner = gameJoinerRepository.findById(new GameJoinerId(userId, listingId))
                .orElseThrow(() -> new ResourceNotFoundException("You are not part of this listing"));

        // Check if this is the creator trying to leave
        GameListing listing = gameListingRepository.findById(listingId).orElse(null);
        if (listing != null && listing.getCreator().getUserId().equals(userId)) {
            throw new BusinessRuleException("As the creator, you cannot leave your own listing. Delete it instead.");
        }

        if (listing != null && !isRequestWindowOpen(listing)) {
            throw new BusinessRuleException("This listing is locked. Membership can no longer be changed.");
        }

        if (joiner.getStatus() == JoinerStatus.LOCKED) {
            throw new BusinessRuleException(
                    "You cannot leave a locked listing. The session has been confirmed.", "BR6");
        }

        if (joiner.getStatus() != JoinerStatus.PENDING && joiner.getStatus() != JoinerStatus.ACCEPTED) {
            throw new BusinessRuleException("You do not have an active membership or request for this listing.");
        }

        joiner.setStatus(JoinerStatus.LEFT);
        gameJoinerRepository.save(joiner);
        logger.info("User {} left listing {}", userId, listingId);
    }

    @Transactional(readOnly = true)
    public void validateJoinAvailability(Long userId, GameListing listing) {
        validateRequestWindowOpen(listing);
        int duration = listing.getSessionDuration() != null ? listing.getSessionDuration() : 1;
        String conflictMsg = schedulingConflictService.getConflictMessage(
                userId, listing.getScheduledDate(), duration, null);
        if (conflictMsg != null) {
            throw new BusinessRuleException(conflictMsg);
        }
    }

    public boolean isRequestWindowOpen(GameListing listing) {
        return LocalDateTime.now().isBefore(
                listing.getScheduledDate().minusHours(GameListingService.LOCK_IN_HOURS_BEFORE_START));
    }

    private void validateRequestWindowOpen(GameListing listing) {
        if (!isRequestWindowOpen(listing)) {
            throw new BusinessRuleException("Join requests close 2 hours before the session starts.");
        }
    }

    private void validatePositionSelection(GameListing listing, Long primaryId, Long alternateId) {
        if (!listing.getFormat().getHasPositions()) {
            if (primaryId != null || alternateId != null) {
                throw new BusinessRuleException("This format does not use player positions.");
            }
            return;
        }

        // NULL primary and alternate means Any Position / no preference.
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

    /**
     * Gets pending join requests for a listing (C500 - creator view).
     */
    @Transactional(readOnly = true)
    public List<GameJoiner> getPendingRequests(Long listingId) {
        return gameJoinerRepository.findByIdGameListingIdAndStatus(listingId, JoinerStatus.PENDING);
    }

    /**
     * Gets all joiners for a listing with a specific status.
     */
    @Transactional(readOnly = true)
    public List<GameJoiner> getJoinersByStatus(Long listingId, JoinerStatus status) {
        return gameJoinerRepository.findByIdGameListingIdAndStatus(listingId, status);
    }

    /**
     * Gets team members for a specific team in a listing.
     */
    @Transactional(readOnly = true)
    public List<GameJoiner> getTeamMembers(Long listingId, Team team) {
        return gameJoinerRepository.findByIdGameListingIdAndTeamAndStatus(
                listingId, team, JoinerStatus.ACCEPTED);
    }

    /**
     * Gets all joiners for a listing (regardless of status).
     */
    @Transactional(readOnly = true)
    public List<GameJoiner> getAllJoiners(Long listingId) {
        return gameJoinerRepository.findByIdGameListingId(listingId);
    }

    /**
     * Gets listings a user has joined (for Lobby - Joined tab).
     */
    @Transactional(readOnly = true)
    public List<GameJoiner> getJoinedListings(Long userId) {
        return gameJoinerRepository.findJoinedListingsForUser(userId);
    }

    /**
     * Gets the count of accepted+locked joiners for a team.
     */
    @Transactional(readOnly = true)
    public long getTeamCount(Long listingId, Team team) {
        return gameJoinerRepository.countByIdGameListingIdAndTeamAndStatus(
                listingId, team, JoinerStatus.ACCEPTED) +
               gameJoinerRepository.countByIdGameListingIdAndTeamAndStatus(
                listingId, team, JoinerStatus.LOCKED);
    }

    /**
     * Counts current participants in a listing (ACCEPTED + LOCKED status).
     * This includes the creator who is auto-added as ACCEPTED.
     */
    public long countCurrentParticipants(Long listingId) {
        return gameJoinerRepository.countByIdGameListingIdAndStatusIn(
                listingId, List.of(JoinerStatus.ACCEPTED, JoinerStatus.LOCKED));
    }

    /**
     * Counts participants in a specific team (ACCEPTED + LOCKED status).
     */
    public long countTeamParticipants(Long listingId, Team team) {
        return gameJoinerRepository.countByIdGameListingIdAndTeamAndStatus(listingId, team, JoinerStatus.ACCEPTED) +
               gameJoinerRepository.countByIdGameListingIdAndTeamAndStatus(listingId, team, JoinerStatus.LOCKED);
    }

    /**
     * Checks if a listing is full (all player positions filled).
     */
    @Transactional(readOnly = true)
    public boolean isListingFull(Long listingId, int maxPlayers) {
        long currentParticipants = countCurrentParticipants(listingId);
        return currentParticipants >= maxPlayers;
    }

    /**
     * Checks if a specific team is full.
     */
    @Transactional(readOnly = true)
    public boolean isTeamFull(Long listingId, Team team, int maxPlayers) {
        int teamCapacity = maxPlayers / 2;
        long teamCount = countTeamParticipants(listingId, team);
        return teamCount >= teamCapacity;
    }

    /**
     * Gets the user's current join request status for a listing.
     * Returns the JoinerStatus if an active request exists (PENDING, ACCEPTED, LOCKED),
     * or null if no active request exists (no record, REJECTED, or LEFT).
     */
    @Transactional(readOnly = true)
    public JoinerStatus getUserJoinRequestStatus(Long userId, Long listingId) {
        Optional<GameJoiner> existing = gameJoinerRepository.findByUserAndListing(userId, listingId);
        if (existing.isPresent()) {
            JoinerStatus status = existing.get().getStatus();
            // REJECTED and LEFT are not considered active — user can re-request
            if (status == JoinerStatus.REJECTED || status == JoinerStatus.LEFT) {
                return null;
            }
            return status;
        }
        return null;
    }
}
