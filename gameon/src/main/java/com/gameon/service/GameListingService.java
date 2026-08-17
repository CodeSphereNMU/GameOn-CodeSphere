package com.gameon.service;

import com.gameon.exception.BusinessRuleException;
import com.gameon.exception.ResourceNotFoundException;
import com.gameon.exception.UnauthorizedAccessException;
import com.gameon.model.entity.*;
import com.gameon.model.enums.*;
import com.gameon.repository.GameJoinerRepository;
import com.gameon.repository.GameListingRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.HashSet;
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
    public static final int LOCK_IN_HOURS_BEFORE_START = 2;

    private final GameListingRepository gameListingRepository;
    private final GameJoinerRepository gameJoinerRepository;
    private final UserRepository userRepository;
    private final UserSportProfileRepository userSportProfileRepository;
    private final SportService sportService;
    private final NotificationService notificationService;
    private final SchedulingConflictService schedulingConflictService;
    private final InvitationService invitationService;

    public GameListingService(GameListingRepository gameListingRepository,
                              GameJoinerRepository gameJoinerRepository,
                              UserRepository userRepository,
                              UserSportProfileRepository userSportProfileRepository,
                              SportService sportService,
                              NotificationService notificationService,
                              SchedulingConflictService schedulingConflictService,
                              InvitationService invitationService) {
        this.gameListingRepository = gameListingRepository;
        this.gameJoinerRepository = gameJoinerRepository;
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
                                     PrivacySetting privacySetting, Integer sessionDuration,
                                     List<Long> positionIds, List<Long> invitedFriendIds) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", creatorId));
        SportFormat format = validateListingDetails(
                creatorId, formatId, skillLevel, scheduledDate, location, privacySetting, sessionDuration);
        validatePositionSelection(format, positionIds);

        GameListing listing = new GameListing(creator, format, skillLevel, scheduledDate,
                location, privacySetting, sessionDuration);
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

        logger.info("Game listing created: ID={} | Creator={} | Sport={} | Date={} | Duration={}h",
                saved.getGameListingId(), creator.getUsername(),
                format.getSport().getSportName(), scheduledDate, sessionDuration);

        // Notify invited friends (invitation is courtesy only - does NOT auto-accept)
        if (invitedFriendIds != null && !invitedFriendIds.isEmpty()) {
            String notifText = creator.getUsername() + " invited you to a " +
                    format.getSport().getSportName() + " " + format.getFormatName() + " game on " +
                    scheduledDate.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM HH:mm")) +
                    " (listing #" + saved.getGameListingId() + "). Submit a join request to participate.";
            notificationService.createBulkNotifications(invitedFriendIds, notifText, NotificationType.LISTING_INVITE);
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
                                              PrivacySetting privacySetting, Integer sessionDuration) {
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
        LocalDateTime earliestAllowedStart = LocalDateTime.now().plusHours(MIN_CREATION_HOURS_BEFORE_START);
        if (scheduledDate.isBefore(earliestAllowedStart)) {
            throw new BusinessRuleException(
                    "This listing must be created at least 3 hours before the start time. The earliest allowed start time is " +
                    earliestAllowedStart.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")) + ".");
        }
        if (sessionDuration == null || sessionDuration < 1 || sessionDuration > 8) {
            throw new BusinessRuleException("Session duration must be between 1 and 8 hours.");
        }
        if (format.getDurationMinutes() != null) {
            int formatHours = (format.getDurationMinutes() + 59) / 60;
            if (sessionDuration != formatHours) {
                throw new BusinessRuleException("Session duration must match the selected sport format.");
            }
        }
        String conflictMsg = schedulingConflictService.getConflictMessage(
                creatorId, scheduledDate, sessionDuration, null);
        if (conflictMsg != null) {
            throw new BusinessRuleException(conflictMsg);
        }
        return format;
    }

    /**
     * Browse available PUBLIC listings (A200).
     * Shows future, non-completed PUBLIC listings that the user hasn't created, matching their sports.
     * Private listings are excluded from browse.
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
        LocalDateTime browseCutoff = LocalDateTime.now().plusHours(LOCK_IN_HOURS_BEFORE_START);
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
        LocalDateTime browseCutoff = LocalDateTime.now().plusHours(LOCK_IN_HOURS_BEFORE_START);
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
                formatIds, LocalDateTime.now().plusHours(LOCK_IN_HOURS_BEFORE_START), userId,
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
        return gameListingRepository.findCreatedByUser(userId);
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

        if (listing.getIsCompleted()) {
            throw new BusinessRuleException("Cannot update a completed listing.");
        }

        // If date is changing, validate the new date
        if (scheduledDate != null && !scheduledDate.equals(listing.getScheduledDate())) {
            if (scheduledDate.isBefore(LocalDateTime.now())) {
                throw new BusinessRuleException("Scheduled date must be in the future.");
            }

            // Re-check scheduling conflict (exclude current listing)
            int duration = listing.getSessionDuration() != null ? listing.getSessionDuration() : 1;
            String conflictMsg = schedulingConflictService.getConflictMessage(
                    userId, scheduledDate, duration, listingId);
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
     * Deletes a game listing (C300). Only the creator can delete.
     * Notifies all joiners that the listing was cancelled.
     */
    @Transactional
    public void deleteListing(Long listingId, Long userId) {
        GameListing listing = getListingById(listingId);

        if (!listing.getCreator().getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("delete", "game listing");
        }

        // Notify joiners before deletion
        String notifText = listing.getCreator().getUsername() + " cancelled their " +
                listing.getFormat().getSport().getSportName() + " game listing.";
        List<Long> joinerIds = listing.getJoiners().stream()
                .map(j -> j.getUser().getUserId())
                .filter(id -> !id.equals(userId)) // Don't notify the creator
                .toList();
        notificationService.createBulkNotifications(joinerIds, notifText, NotificationType.LISTING_CANCELLED);

        gameListingRepository.delete(listing);
        logger.info("Game listing {} deleted by creator {}", listingId, userId);
    }

    /**
     * Marks a listing as completed (after match result recorded).
     */
    @Transactional
    public void markCompleted(Long listingId) {
        GameListing listing = getListingById(listingId);
        listing.setIsCompleted(true);
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
