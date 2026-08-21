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

/** Handles join-request history separately from the accepted participant roster. */
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

    public GameJoinerService(GameJoinerRepository gameJoinerRepository,
                             JoinRequestRepository joinRequestRepository,
                             GameListingRepository gameListingRepository,
                             UserRepository userRepository,
                             UserSportProfileRepository userSportProfileRepository,
                             NotificationService notificationService,
                             SchedulingConflictService schedulingConflictService,
                             SportService sportService,
                             InvitationRepository invitationRepository) {
        this.gameJoinerRepository = gameJoinerRepository;
        this.joinRequestRepository = joinRequestRepository;
        this.gameListingRepository = gameListingRepository;
        this.userRepository = userRepository;
        this.userSportProfileRepository = userSportProfileRepository;
        this.notificationService = notificationService;
        this.schedulingConflictService = schedulingConflictService;
        this.sportService = sportService;
        this.invitationRepository = invitationRepository;
    }

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
        request.setStatus(JoinRequestStatus.REJECTED);
        JoinRequest saved = joinRequestRepository.save(request);

        String text = "Your join request for " + listing.getFormat().getSport().getSportName() +
                " " + listing.getFormat().getFormatName() + " was declined.";
        notificationService.createNotification(requesterId, text, NotificationType.JOIN_REJECTED,
                listing.getCreator(), listing, request, null);
        return saved;
    }

    /** @return true when a pending request was withdrawn; false when a participant left. */
    @Transactional
    public boolean leaveListing(Long userId, Long listingId) {
        GameListing listing = gameListingRepository.findByIdWithDetails(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Game Listing", listingId));
        if (listing.getCreator().getUserId().equals(userId)) {
            throw new BusinessRuleException("As the creator, you cannot leave your own listing. Cancel it instead.");
        }
        validateRequestWindowOpen(listing);

        Optional<GameJoiner> participant = gameJoinerRepository.findByUserAndListing(userId, listingId);
        if (participant.isPresent() && participant.get().getStatus() != JoinerStatus.LEFT) {
            if (participant.get().getStatus() == JoinerStatus.LOCKED) {
                throw new BusinessRuleException("You cannot leave a locked listing.", "BR6");
            }
            participant.get().setStatus(JoinerStatus.LEFT);
            gameJoinerRepository.save(participant.get());
            notifyCreatorOfWithdrawal(listing, participant.get().getUser(), null, false);
            return false;
        }

        JoinRequest request = findPendingRequest(listingId, userId);
        request.setStatus(JoinRequestStatus.WITHDRAWN);
        joinRequestRepository.save(request);
        notifyCreatorOfWithdrawal(listing, request.getUser(), request, true);
        return true;
    }

    private void notifyCreatorOfWithdrawal(GameListing listing, User user, JoinRequest request,
                                           boolean requestOnly) {
        String text = requestOnly
                ? user.getUsername() + " withdrew their join request."
                : user.getUsername() + " left your game listing.";
        notificationService.createNotification(listing.getCreator().getUserId(), text,
                NotificationType.JOIN_WITHDRAWN, user, listing, request, null);
    }

    @Transactional(readOnly = true)
    public void validateJoinAvailability(Long userId, GameListing listing) {
        validateRequestWindowOpen(listing);
        String conflictMsg = schedulingConflictService.getConflictMessageMinutes(
                userId, listing.getScheduledDate(), listing.getDurationMinutes(), null);
        if (conflictMsg != null) throw new BusinessRuleException(conflictMsg);
    }

    public boolean isRequestWindowOpen(GameListing listing) {
        return listing.getListingStatus() == ListingStatus.OPEN && LocalDateTime.now().isBefore(
                listing.getScheduledDate().minusHours(GameListingService.LOCK_IN_HOURS_BEFORE_START));
    }

    private void validateRequestWindowOpen(GameListing listing) {
        if (!isRequestWindowOpen(listing)) {
            throw new BusinessRuleException("Join requests close 2 hours before the game starts.");
        }
    }

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

    @Transactional(readOnly = true)
    public boolean isParticipant(Long userId, Long listingId) {
        return gameJoinerRepository.existsAcceptedOrLocked(userId, listingId);
    }

    @Transactional(readOnly = true)
    public long getTeamCount(Long listingId, Team team) {
        return countTeamParticipants(listingId, team);
    }

    public long countCurrentParticipants(Long listingId) {
        return gameJoinerRepository.countByIdGameListingIdAndStatusIn(
                listingId, List.of(JoinerStatus.ACCEPTED, JoinerStatus.LOCKED));
    }

    public long countTeamParticipants(Long listingId, Team team) {
        return gameJoinerRepository.countByIdGameListingIdAndTeamAndStatus(
                listingId, team, JoinerStatus.ACCEPTED)
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

    private LocalDateTime requestCutoff() {
        return currentTime().plusHours(GameListingService.LOCK_IN_HOURS_BEFORE_START);
    }

    LocalDateTime currentTime() {
        return LocalDateTime.now();
    }
}
