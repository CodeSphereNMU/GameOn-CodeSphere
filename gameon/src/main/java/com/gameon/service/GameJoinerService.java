package com.gameon.service;

import com.gameon.exception.BusinessRuleException;
import com.gameon.exception.DuplicateResourceException;
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

/**
 * Service handling join requests for game listings.
 * Covers A300 (Send Join Request), A400 (Leave), C500 (View/Accept/Reject Requests).
 *
 * Business Rules enforced:
 * - BR9: User can only join listing if sport is on profile
 * - BR14: Cannot join 2 listings within 3 hours of each other
 */
@Service
public class GameJoinerService {

    private static final Logger logger = LoggerFactory.getLogger(GameJoinerService.class);

    private static final int TIME_CONFLICT_HOURS = 3;

    private final GameJoinerRepository gameJoinerRepository;
    private final GameListingRepository gameListingRepository;
    private final UserRepository userRepository;
    private final UserSportProfileRepository userSportProfileRepository;
    private final NotificationService notificationService;

    public GameJoinerService(GameJoinerRepository gameJoinerRepository,
                             GameListingRepository gameListingRepository,
                             UserRepository userRepository,
                             UserSportProfileRepository userSportProfileRepository,
                             NotificationService notificationService) {
        this.gameJoinerRepository = gameJoinerRepository;
        this.gameListingRepository = gameListingRepository;
        this.userRepository = userRepository;
        this.userSportProfileRepository = userSportProfileRepository;
        this.notificationService = notificationService;
    }

    /**
     * Sends a join request for a game listing (A300).
     * BR9: Sport must be on user's profile.
     * BR14: Cannot join if within 3 hours of another listing.
     */
    @Transactional
    public GameJoiner sendJoinRequest(Long userId, Long listingId, Team team,
                                      Long formatPositionId, Long altFormatPositionId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        GameListing listing = gameListingRepository.findByIdWithDetails(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Game Listing", listingId));

        // Cannot join own listing
        if (listing.getCreator().getUserId().equals(userId)) {
            throw new BusinessRuleException("You cannot join your own listing.");
        }

        // Cannot join completed listing
        if (listing.getIsCompleted()) {
            throw new BusinessRuleException("This listing is no longer active.");
        }

        // Check for duplicate request
        if (gameJoinerRepository.existsByIdUserIdAndIdGameListingId(userId, listingId)) {
            throw new DuplicateResourceException("You have already requested to join this listing.");
        }

        // BR9: Sport must be on profile
        Long sportId = listing.getFormat().getSport().getSportId();
        if (!userSportProfileRepository.existsByIdUserIdAndIdSportId(userId, sportId)) {
            throw new BusinessRuleException(
                    "You must add " + listing.getFormat().getSport().getSportName() +
                    " to your profile before joining this listing.", "BR9");
        }

        // BR14: Time conflict check (within 3 hours)
        LocalDateTime listingTime = listing.getScheduledDate();
        LocalDateTime startWindow = listingTime.minusHours(TIME_CONFLICT_HOURS);
        LocalDateTime endWindow = listingTime.plusHours(TIME_CONFLICT_HOURS);

        List<GameJoiner> conflicts = gameJoinerRepository.findUserJoinedListingsInTimeRange(
                userId, startWindow, endWindow);
        if (!conflicts.isEmpty()) {
            throw new BusinessRuleException(
                    "You cannot join this listing. You have another game within 3 hours of this time.", "BR14");
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
     */
    @Transactional
    public GameJoiner acceptRequest(Long listingId, Long joinerId, Long creatorId) {
        GameListing listing = gameListingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Game Listing", listingId));

        if (!listing.getCreator().getUserId().equals(creatorId)) {
            throw new UnauthorizedAccessException("accept requests for", "game listing");
        }

        GameJoiner joiner = gameJoinerRepository.findById(new GameJoinerId(joinerId, listingId))
                .orElseThrow(() -> new ResourceNotFoundException("Join request not found"));

        if (joiner.getStatus() != JoinerStatus.PENDING) {
            throw new BusinessRuleException("This request has already been processed.");
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
     */
    @Transactional
    public void leaveListing(Long userId, Long listingId) {
        GameJoiner joiner = gameJoinerRepository.findById(new GameJoinerId(userId, listingId))
                .orElseThrow(() -> new ResourceNotFoundException("You are not part of this listing"));

        if (joiner.getStatus() == JoinerStatus.LOCKED) {
            throw new BusinessRuleException(
                    "You cannot leave a locked listing. The session has been confirmed.", "BR6");
        }

        joiner.setStatus(JoinerStatus.LEFT);
        gameJoinerRepository.save(joiner);
        logger.info("User {} left listing {}", userId, listingId);
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
}
