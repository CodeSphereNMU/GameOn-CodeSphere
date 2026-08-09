package com.gameon.service;

import com.gameon.exception.BusinessRuleException;
import com.gameon.exception.ResourceNotFoundException;
import com.gameon.exception.UnauthorizedAccessException;
import com.gameon.model.entity.GameListing;
import com.gameon.model.entity.SportFormat;
import com.gameon.model.entity.User;
import com.gameon.model.enums.NotificationType;
import com.gameon.model.enums.PrivacySetting;
import com.gameon.model.enums.SkillLevel;
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
import java.util.List;

/**
 * Service handling game listing lifecycle.
 * Covers A100 (Create), A200 (Browse), A500 (Hide Expired), C300 (Manage).
 *
 * Business Rules enforced:
 * - BR1: A user can post ONE Game Listing at a time
 * - BR8: User can only create listing if sport is on profile
 */
@Service
public class GameListingService {

    private static final Logger logger = LoggerFactory.getLogger(GameListingService.class);

    private final GameListingRepository gameListingRepository;
    private final UserRepository userRepository;
    private final UserSportProfileRepository userSportProfileRepository;
    private final SportService sportService;
    private final NotificationService notificationService;

    public GameListingService(GameListingRepository gameListingRepository,
                              UserRepository userRepository,
                              UserSportProfileRepository userSportProfileRepository,
                              SportService sportService,
                              NotificationService notificationService) {
        this.gameListingRepository = gameListingRepository;
        this.userRepository = userRepository;
        this.userSportProfileRepository = userSportProfileRepository;
        this.sportService = sportService;
        this.notificationService = notificationService;
    }

    /**
     * Creates a new game listing.
     * BR1: User can only have one active listing at a time.
     * BR8: User can only create listing if sport is on profile.
     */
    @Transactional
    public GameListing createListing(Long creatorId, Long formatId, SkillLevel skillLevel,
                                     LocalDateTime scheduledDate, String location,
                                     PrivacySetting privacySetting, List<Long> invitedFriendIds) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", creatorId));

        // BR1: Check max 1 active listing
        long activeCount = gameListingRepository.countByCreatorUserIdAndIsCompletedFalse(creatorId);
        if (activeCount > 0) {
            throw new BusinessRuleException(
                    "You already have an active game listing. Complete or delete it before creating a new one.", "BR1");
        }

        // Get format and validate
        SportFormat format = sportService.getFormatById(formatId);
        Long sportId = format.getSport().getSportId();

        // BR8: Sport must be on creator's profile
        if (!userSportProfileRepository.existsByIdUserIdAndIdSportId(creatorId, sportId)) {
            throw new BusinessRuleException(
                    "You must add " + format.getSport().getSportName() + " to your profile before creating a listing.", "BR8");
        }

        // Validate scheduled date is in the future
        if (scheduledDate.isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("Scheduled date must be in the future.");
        }

        GameListing listing = new GameListing(creator, format, skillLevel, scheduledDate, location, privacySetting);
        GameListing saved = gameListingRepository.save(listing);

        logger.info("Game listing created: ID={} | Creator={} | Sport={} | Date={}",
                saved.getGameListingId(), creator.getUsername(),
                format.getSport().getSportName(), scheduledDate);

        // Notify invited friends
        if (invitedFriendIds != null && !invitedFriendIds.isEmpty()) {
            String notifText = creator.getUsername() + " invited you to a " +
                    format.getSport().getSportName() + " " + format.getFormatName() + " game!";
            notificationService.createBulkNotifications(invitedFriendIds, notifText, NotificationType.LISTING_INVITE);
        }

        return saved;
    }

    /**
     * Browse available listings (A200).
     * Shows future, non-completed listings that the user hasn't created, matching their sports.
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
        return gameListingRepository.findAvailableListings(formatIds, LocalDateTime.now(), userId, pageable);
    }

    /**
     * Browse with skill level filter.
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
        return gameListingRepository.findAvailableListingsBySkill(formatIds, LocalDateTime.now(), userId, skillLevel, pageable);
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

        if (scheduledDate != null) listing.setScheduledDate(scheduledDate);
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
}
