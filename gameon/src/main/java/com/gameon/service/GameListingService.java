package com.gameon.service;

import com.gameon.exception.BusinessRuleException;
import com.gameon.exception.ResourceNotFoundException;
import com.gameon.exception.UnauthorizedAccessException;
import com.gameon.model.entity.*;
import com.gameon.model.enums.*;
import com.gameon.repository.GameJoinerRepository;
import com.gameon.repository.GameListingRepository;
import com.gameon.repository.InvitationRepository;
import com.gameon.repository.JoinRequestRepository;
import com.gameon.repository.UserRepository;
import com.gameon.repository.UserSportProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Service handling game listing lifecycle.
 * Covers A100 (Create), A200 (Browse), A500 (Hide Expired), C300 (Manage).
 *
 * Business Rules enforced:
 * - User can create MULTIPLE listings (old BR1 one-listing limit removed)
 * - Scheduling conflict: new listing must not conflict with user's existing sessions (60-min buffer)
 * - Listing must be created at least 3 hours before start time
 * - BR8: User can only create listing if sport is on profile
 * - Position validation: Any Position (NULL) or up to two valid preferences
 * - Creator is automatically a participant
 * - Privacy: public listings appear in browse, private do not
 */
@Service
public class GameListingService {

    private static final Logger logger = LoggerFactory.getLogger(GameListingService.class);
    private static final int MIN_CREATION_HOURS_BEFORE_START = 3;
    /** Browse Listings cutoff: listings hidden from browse at T-1h (was T-2h). */
    public static final int BROWSE_CUTOFF_HOURS_BEFORE_START = 1;
    /** Legacy constant kept for any external references; prefer BROWSE_CUTOFF_HOURS_BEFORE_START. */
    public static final int LOCK_IN_HOURS_BEFORE_START = 2;

    private final GameListingRepository gameListingRepository;
    private final GameJoinerRepository gameJoinerRepository;
    private final JoinRequestRepository joinRequestRepository;
    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final UserSportProfileRepository userSportProfileRepository;
    private final SportService sportService;
    private final NotificationService notificationService;
    private final SchedulingConflictService schedulingConflictService;
    private final InvitationService invitationService;

    public GameListingService(GameListingRepository gameListingRepository,
                              GameJoinerRepository gameJoinerRepository,
                              JoinRequestRepository joinRequestRepository,
                              InvitationRepository invitationRepository,
                              UserRepository userRepository,
                              UserSportProfileRepository userSportProfileRepository,
                              SportService sportService,
                              NotificationService notificationService,
                              SchedulingConflictService schedulingConflictService,
                              InvitationService invitationService) {
        this.gameListingRepository = gameListingRepository;
        this.gameJoinerRepository = gameJoinerRepository;
        this.joinRequestRepository = joinRequestRepository;
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.userSportProfileRepository = userSportProfileRepository;
        this.sportService = sportService;
        this.notificationService = notificationService;
        this.schedulingConflictService = schedulingConflictService;
        this.invitationService = invitationService;
    }

    /**
     * Creates a new game listing with full validation.
     * - BR8: Sport must be on creator's profile
     * - Listing must be created at least 3 hours before start
     * - Session duration must be valid
     * - Scheduling conflict check with 60-min travel buffer
     * - Position validation (if sport has positions)
     * - Creator becomes automatic participant
     */
    @Transactional
    public GameListing createListing(Long creatorId, Long formatId, SkillLevel skillLevel,
                                     LocalDateTime scheduledDate, String location,
                                     PrivacySetting privacySetting, Integer durationMinutes,
                                     List<Long> positionIds, List<Long> invitedFriendIds) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", creatorId));
        SportFormat format = validateListingDetails(
                creatorId, formatId, skillLevel, scheduledDate, location, privacySetting, durationMinutes);
        validatePositionSelection(format, positionIds);

        GameListing listing = new GameListing(creator, format, skillLevel, scheduledDate,
                location, privacySetting, durationMinutes);
        GameListing saved = gameListingRepository.save(listing);

        // Rule 8: Creator automatically becomes a participant (Team A, ACCEPTED)
        GameJoiner creatorJoiner = new GameJoiner(creator, saved, Team.A);
        creatorJoiner.setStatus(JoinerStatus.ACCEPTED);
        if (positionIds != null && !positionIds.isEmpty()) {
            // Set first selected position for the creator
            creatorJoiner.setFormatPositionId(positionIds.get(0));
            if (positionIds.size() > 1) {
                creatorJoiner.setAltFormatPositionId(positionIds.get(1));
            }
        }
        gameJoinerRepository.save(creatorJoiner);

        invitationService.createInvitations(saved, creatorId, invitedFriendIds);

        logger.info("Game listing created: ID={} | Creator={} | Sport={} | Date={} | Duration={}m",
                saved.getGameListingId(), creator.getUsername(),
                format.getSport().getSportName(), scheduledDate, durationMinutes);

        // Notify invited friends (invitation is courtesy only - does NOT auto-accept)
        if (invitedFriendIds != null && !invitedFriendIds.isEmpty()) {
            String notifText = creator.getUsername() + " invited you to game listing #" +
                    saved.getGameListingId() + ". View the listing for its current details, then submit a join request to participate.";
            notificationService.createBulkNotifications(invitedFriendIds, notifText,
                    NotificationType.LISTING_INVITE, creator, saved, null, null);
        }

        return saved;
    }

    /**
     * Validates position selection rules.
     * - NULL is a missing choice; an empty list intentionally means Any Position
     * - Up to two distinct positions, both valid for the selected format
     */
    public void validatePositionSelection(SportFormat format, List<Long> positionIds) {
        if (!format.getHasPositions()) {
            if (positionIds != null && !positionIds.isEmpty()) {
                throw new BusinessRuleException("This format does not use player positions.");
            }
            return;
        }
        if (positionIds == null) {
            throw new BusinessRuleException("Choose Any Position or up to 2 preferred positions.");
        }
        // An empty list intentionally represents Any Position.
        if (positionIds.isEmpty()) {
            return;
        }
        if (positionIds.size() > 2) {
            throw new BusinessRuleException("Select no more than 2 preferred positions.");
        }
        Set<Long> selected = new HashSet<>(positionIds);
        if (selected.size() != positionIds.size()) {
            throw new BusinessRuleException("Preferred positions must be different.");
        }
        Set<Long> validIds = sportService.getPositionIdsForFormat(format.getFormatId());
        if (!validIds.containsAll(positionIds)) {
            throw new BusinessRuleException("Select positions that belong to this sport format.");
        }
    }

    @Transactional(readOnly = true)
    public SportFormat validateListingDetails(Long creatorId, Long formatId, SkillLevel skillLevel,
                                              LocalDateTime scheduledDate, String location,
                                              PrivacySetting privacySetting, Integer durationMinutes) {
        SportFormat format = sportService.getFormatById(formatId);
        Long sportId = format.getSport().getSportId();
        if (!userSportProfileRepository.existsByIdUserIdAndIdSportId(creatorId, sportId)) {
            throw new BusinessRuleException(
                    "This sport is not included in your sports profile. You must add " +
                    format.getSport().getSportName() + " before creating a listing.", "BR8");
        }
        if (skillLevel == null || privacySetting == null) {
            throw new BusinessRuleException("Skill level and privacy are required.");
        }
        if (location == null || location.isBlank()) {
            throw new BusinessRuleException("Location is required.");
        }
        if (scheduledDate == null) {
            throw new BusinessRuleException("Date and time are required.");
        }
        LocalDateTime earliestAllowedStart = earliestAllowedStart();
        if (scheduledDate.isBefore(earliestAllowedStart)) {
            throw new BusinessRuleException(
                    "This listing must be created at least 3 hours before the start time. The earliest allowed start time is " +
                    earliestAllowedStart.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")) + ".");
        }
        if (durationMinutes == null || durationMinutes < 1 || durationMinutes > 480) {
            throw new BusinessRuleException("Session duration must be between 1 and 480 minutes.");
        }
        if (!durationMinutes.equals(format.getDurationMinutes())) {
            throw new BusinessRuleException("Session duration must match the selected sport format.");
        }
        String conflictMsg = schedulingConflictService.getConflictMessageMinutes(
                creatorId, scheduledDate, durationMinutes, null);
        if (conflictMsg != null) {
            throw new BusinessRuleException(conflictMsg);
        }
        return format;
    }

    /**
     * Browse available PUBLIC listings (A200).
     * Shows future, non-completed PUBLIC listings that the user hasn't created, matching their sports.
     * Private listings are excluded from browse.
     * Listings are hidden from browse at T-1h (finalisation point).
     */
    @Transactional(readOnly = true)
    public Page<GameListing> browseAvailableListings(Long userId, Pageable pageable) {
        List<Long> userSportIds = userSportProfileRepository.findDistinctSportIdsByUserId(userId);
        if (userSportIds.isEmpty()) {
            return Page.empty(pageable);
        }
        List<Long> formatIds = sportService.getFormatsBySportIds(userSportIds).stream()
                .map(SportFormat::getFormatId)
                .toList();
        if (formatIds.isEmpty()) {
            return Page.empty(pageable);
        }
        LocalDateTime browseCutoff = LocalDateTime.now().plusHours(BROWSE_CUTOFF_HOURS_BEFORE_START);
        return gameListingRepository.findAvailablePublicListings(formatIds, browseCutoff, userId, pageable);
    }

    /**
     * Browse with skill level filter (PUBLIC only).
     */
    @Transactional(readOnly = true)
    public Page<GameListing> browseAvailableListings(Long userId, SkillLevel skillLevel, Pageable pageable) {
        List<Long> userSportIds = userSportProfileRepository.findDistinctSportIdsByUserId(userId);
        if (userSportIds.isEmpty()) {
            return Page.empty(pageable);
        }
        List<Long> formatIds = sportService.getFormatsBySportIds(userSportIds).stream()
                .map(SportFormat::getFormatId)
                .toList();
        if (formatIds.isEmpty()) {
            return Page.empty(pageable);
        }
        LocalDateTime browseCutoff = LocalDateTime.now().plusHours(BROWSE_CUTOFF_HOURS_BEFORE_START);
        return gameListingRepository.findAvailablePublicListingsBySkill(formatIds, browseCutoff, userId, skillLevel, pageable);
    }

    @Transactional(readOnly = true)
    public Page<GameListing> browseAvailableListings(Long userId, Long sportId, SkillLevel skillLevel,
                                                     LocalDate date, boolean hideFull, Pageable pageable) {
        List<Long> userSportIds = userSportProfileRepository.findDistinctSportIdsByUserId(userId);
        if (userSportIds.isEmpty()) return Page.empty(pageable);
        if (sportId != null && !userSportIds.contains(sportId)) {
            throw new BusinessRuleException("You can only browse sports included in your profile.");
        }
        List<Long> formatIds = sportService.getFormatsBySportIds(userSportIds).stream()
                .map(SportFormat::getFormatId).toList();
        if (formatIds.isEmpty()) return Page.empty(pageable);
        LocalDateTime fromDate = null;
        LocalDateTime toDate = null;
        if (date != null) {
            fromDate = date.atStartOfDay();
            toDate = date.plusDays(1).atStartOfDay();
        }
        return gameListingRepository.searchAvailablePublicListings(
                formatIds, LocalDateTime.now().plusHours(BROWSE_CUTOFF_HOURS_BEFORE_START), userId,
                sportId, skillLevel, fromDate, toDate, hideFull, pageable);
    }

    @Transactional(readOnly = true)
    public GameListing getListingById(Long listingId) {
        return gameListingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Game Listing", listingId));
    }

    @Transactional(readOnly = true)
    public GameListing getListingWithDetails(Long listingId) {
        return gameListingRepository.findByIdWithDetails(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Game Listing", listingId));
    }

    @Transactional(readOnly = true)
    public GameListing getListingDetail(Long listingId) {
        return gameListingRepository.findDetailById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Game Listing", listingId));
    }

    /**
     * Gets listings created by a specific user (for Lobby - Created tab).
     */
    @Transactional(readOnly = true)
    public List<GameListing> getCreatedListings(Long userId) {
        return gameListingRepository.findCreatedByUser(userId, currentTime());
    }

    /**
     * Updates a game listing (C300). Only the creator can update.
     * Re-validates scheduling conflict if date changes.
     */
    @Transactional
    public GameListing updateListing(Long listingId, Long userId, LocalDateTime scheduledDate,
                                     String location, SkillLevel skillLevel, PrivacySetting privacySetting) {
        GameListing listing = getListingById(listingId);

        if (!listing.getCreator().getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("update", "game listing");
        }

        validateEditable(listing);

        // If date is changing, validate the new date
        if (scheduledDate != null && !scheduledDate.equals(listing.getScheduledDate())) {
            LocalDateTime earliestAllowedStart = earliestAllowedStart();
            if (scheduledDate.isBefore(earliestAllowedStart)) {
                throw new BusinessRuleException("The new start time must be at least 3 hours from now.");
            }

            // Re-check scheduling conflict (exclude current listing)
            int durationMinutes = listing.getDurationMinutes() != null ? listing.getDurationMinutes() : 60;
            String conflictMsg = schedulingConflictService.getConflictMessageMinutes(
                    userId, scheduledDate, durationMinutes, listingId);
            if (conflictMsg != null) {
                throw new BusinessRuleException(conflictMsg);
            }

            listing.setScheduledDate(scheduledDate);
        }

        if (location != null) listing.setLocation(location);
        if (skillLevel != null) listing.setSkillLevel(skillLevel);
        if (privacySetting != null) listing.setPrivacySetting(privacySetting);

        logger.info("Game listing {} updated by user {}", listingId, userId);
        return gameListingRepository.save(listing);
    }

    /**
     * Cancels a game listing (C300). The row and its history are preserved.
     */
    @Transactional
    public void cancelListing(Long listingId, Long userId) {
        GameListing listing = getListingById(listingId);

        if (!listing.getCreator().getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("cancel", "game listing");
        }

        if (listing.getListingStatus() == ListingStatus.COMPLETED
                || listing.getListingStatus() == ListingStatus.CANCELLED_BY_CREATOR
                || listing.getListingStatus() == ListingStatus.CANCELLED_INSUFFICIENT_PLAYERS) {
            throw new BusinessRuleException("This listing can no longer be cancelled.");
        }

        // CONFIRMED is an additional state safeguard: a finalised listing is never cancellable.
        if (listing.getListingStatus() == ListingStatus.CONFIRMED) {
            throw new BusinessRuleException(
                    "This listing has been confirmed and can no longer be cancelled.");
        }

        if (!listing.getScheduledDate().isAfter(currentTime())) {
            throw new BusinessRuleException(
                    "This listing can no longer be cancelled because its scheduled start time has been reached.");
        }

        // T-1h is the creator's commitment point. Cancellation is rejected once the scheduled
        // start is one hour or less away, independent of the once-per-minute lifecycle scheduler.
        // This closes the race where a listing is still OPEN at/inside T-1h because the scheduler
        // has not yet finalised it.
        LocalDateTime finalisationBoundary = currentTime()
                .plusHours(ListingLifecycleService.FINALISATION_HOURS);
        if (!listing.getScheduledDate().isAfter(finalisationBoundary)) {
            throw new BusinessRuleException(
                    "This listing can no longer be cancelled because it is within 1 hour of its scheduled start.");
        }

        Set<Long> recipientIds = new LinkedHashSet<>();
        gameJoinerRepository.findParticipants(listingId).stream()
                .map(joiner -> joiner.getUser().getUserId())
                .filter(id -> !id.equals(userId))
                .forEach(recipientIds::add);
        joinRequestRepository.findPendingUserIds(listingId).stream()
                .filter(id -> !id.equals(userId))
                .forEach(recipientIds::add);
        invitationRepository.findByGameListingGameListingIdAndStatus(listingId, InvitationStatus.PENDING).stream()
                .map(invitation -> invitation.getInvitee().getUserId())
                .filter(id -> !id.equals(userId))
                .forEach(recipientIds::add);

        listing.setListingStatus(ListingStatus.CANCELLED_BY_CREATOR);
        joinRequestRepository.expirePendingForListing(listingId);
        invitationRepository.expirePendingForListing(listingId);
        gameListingRepository.save(listing);

        String notifText = listing.getCreator().getUsername() + " cancelled their " +
                listing.getFormat().getSport().getSportName() + " game listing.";
        notificationService.createBulkNotifications(List.copyOf(recipientIds), notifText,
                NotificationType.LISTING_CANCELLED, listing.getCreator(), listing, null, null);
        logger.info("Game listing {} cancelled by creator {}", listingId, userId);
    }

    /** Compatibility entry point used by the current controller. */
    @Transactional
    public void deleteListing(Long listingId, Long userId) {
        cancelListing(listingId, userId);
    }

    /**
     * Whether the creator may currently cancel this listing. Mirrors the guards in
     * {@link #cancelListing(Long, Long)}: not already finished/cancelled, not CONFIRMED,
     * and more than 1 hour (T-1h) before the scheduled start. Used to drive UI visibility.
     */
    @Transactional(readOnly = true)
    public boolean isCreatorCancellable(GameListing listing) {
        ListingStatus status = listing.getListingStatus();
        if (status == ListingStatus.COMPLETED
                || status == ListingStatus.CANCELLED_BY_CREATOR
                || status == ListingStatus.CANCELLED_INSUFFICIENT_PLAYERS
                || status == ListingStatus.CONFIRMED) {
            return false;
        }
        LocalDateTime finalisationBoundary = currentTime()
                .plusHours(ListingLifecycleService.FINALISATION_HOURS);
        return listing.getScheduledDate().isAfter(finalisationBoundary);
    }

    @Transactional(readOnly = true)
    public boolean isEditable(GameListing listing) {
        return listing.getListingStatus() == ListingStatus.OPEN
                && !hasActiveJoinInterest(listing);
    }

    @Transactional(readOnly = true)
    public void validateEditable(GameListing listing) {
        if (listing.getListingStatus() != ListingStatus.OPEN) {
            throw new BusinessRuleException("Only open listings can be edited.");
        }
        if (hasActiveJoinInterest(listing)) {
            throw new BusinessRuleException(
                    "This listing cannot be edited while another user has a pending request or accepted participation.");
        }
    }

    private boolean hasActiveJoinInterest(GameListing listing) {
        Long listingId = listing.getGameListingId();
        boolean activeRequest = joinRequestRepository.existsByGameListingGameListingIdAndStatusIn(
                listingId, List.of(JoinRequestStatus.PENDING, JoinRequestStatus.ACCEPTED));
        return activeRequest || gameJoinerRepository.existsNonCreatorParticipant(
                listingId, listing.getCreator().getUserId());
    }

    private LocalDateTime earliestAllowedStart() {
        // datetime-local submits minute precision, so validate against the same precision.
        return currentTime()
                .truncatedTo(ChronoUnit.MINUTES)
                .plusHours(MIN_CREATION_HOURS_BEFORE_START);
    }

    LocalDateTime currentTime() {
        return LocalDateTime.now();
    }

    /**
     * Marks a listing as completed (after match result recorded).
     */
    @Transactional
    public void markCompleted(Long listingId) {
        GameListing listing = getListingById(listingId);
        listing.setListingStatus(ListingStatus.COMPLETED);
        gameListingRepository.save(listing);
        logger.info("Game listing {} marked as completed", listingId);
    }

    /**
     * Validates that the user has the sport on their profile for accessing a listing.
     * Used for backend enforcement of sport profile restriction.
     */
    @Transactional(readOnly = true)
    public void validateSportProfileAccess(Long userId, GameListing listing) {
        Long sportId = listing.getFormat().getSport().getSportId();
        if (!userSportProfileRepository.existsByIdUserIdAndIdSportId(userId, sportId)) {
            throw new BusinessRuleException(
                    "This sport is not included in your sports profile. " +
                    "Add " + listing.getFormat().getSport().getSportName() + " to your profile first.");
        }
    }
}
