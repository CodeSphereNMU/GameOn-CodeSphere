package com.gameon.service;

import com.gameon.exception.BusinessRuleException;
import com.gameon.exception.ResourceNotFoundException;
import com.gameon.exception.UnauthorizedAccessException;
import com.gameon.model.entity.*;
import com.gameon.model.enums.*;
import com.gameon.repository.GameJoinerRepository;
import com.gameon.repository.GameListingRepository;
import com.gameon.repository.ListingInvitationRepository;
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
import java.util.Optional;

/**
 * Service handling game listing lifecycle.
 * Covers A100 (Create), A200 (Browse), A500 (Hide Expired), C300 (Manage).
 *
 * Business Rules enforced:
 * - User can create MULTIPLE listings (old BR1 one-listing limit removed)
 * - Scheduling conflict: new listing must not conflict with user's existing sessions (60-min buffer)
 * - Listing must be created at least 3 hours before start time
 * - BR8: User can only create listing if sport is on profile
 * - Position validation: if sport has positions, at least one must be selected
 * - Creator is automatically a participant
 * - Privacy: public listings appear in browse, private do not
 */
@Service
public class GameListingService {

    private static final Logger logger = LoggerFactory.getLogger(GameListingService.class);
    private static final int MIN_CREATION_HOURS_BEFORE_START = 3;

    private final GameListingRepository gameListingRepository;
    private final GameJoinerRepository gameJoinerRepository;
    private final UserRepository userRepository;
    private final UserSportProfileRepository userSportProfileRepository;
    private final ListingInvitationRepository listingInvitationRepository;
    private final SportService sportService;
    private final NotificationService notificationService;
    private final SchedulingConflictService schedulingConflictService;

    public GameListingService(GameListingRepository gameListingRepository,
                              GameJoinerRepository gameJoinerRepository,
                              UserRepository userRepository,
                              UserSportProfileRepository userSportProfileRepository,
                              ListingInvitationRepository listingInvitationRepository,
                              SportService sportService,
                              NotificationService notificationService,
                              SchedulingConflictService schedulingConflictService) {
        this.gameListingRepository = gameListingRepository;
        this.gameJoinerRepository = gameJoinerRepository;
        this.userRepository = userRepository;
        this.userSportProfileRepository = userSportProfileRepository;
        this.listingInvitationRepository = listingInvitationRepository;
        this.sportService = sportService;
        this.notificationService = notificationService;
        this.schedulingConflictService = schedulingConflictService;
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
                                     List<Long> positionIds, List<Long> invitedFriendIds,
                                     String venueName, String address, Double latitude, Double longitude) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", creatorId));

        // Get format and validate
        SportFormat format = sportService.getFormatById(formatId);
        Long sportId = format.getSport().getSportId();

        // BR8: Sport must be on creator's profile
        if (!userSportProfileRepository.existsByIdUserIdAndIdSportId(creatorId, sportId)) {
            throw new BusinessRuleException(
                    "This sport is not included in your sports profile. " +
                    "You must add " + format.getSport().getSportName() + " to your profile before creating a listing.", "BR8");
        }

        // Position validation: if format has positions, at least one must be selected
        if (format.getHasPositions()) {
            validatePositionSelection(positionIds);
        }

        // Validate scheduled date is in the future
        if (scheduledDate.isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("Scheduled date must be in the future.");
        }

        // Validate listing is created at least 3 hours before start time
        LocalDateTime earliestAllowedStart = LocalDateTime.now().plusHours(MIN_CREATION_HOURS_BEFORE_START);
        if (scheduledDate.isBefore(earliestAllowedStart)) {
            throw new BusinessRuleException(
                    "This listing must be created at least 3 hours before the start time. " +
                    "The earliest allowed start time is " +
                    earliestAllowedStart.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")) + ".");
        }

        // Validate session duration
        if (sessionDuration == null || sessionDuration < 1 || sessionDuration > 8) {
            throw new BusinessRuleException("Session duration must be between 1 and 8 hours.");
        }

        // Scheduling conflict check with 60-minute travel buffer
        String conflictMsg = schedulingConflictService.getConflictMessage(creatorId, scheduledDate, sessionDuration, null);
        if (conflictMsg != null) {
            throw new BusinessRuleException(conflictMsg);
        }

        GameListing listing = new GameListing(creator, format, skillLevel, scheduledDate,
                location, privacySetting, sessionDuration);
        // Set map location fields
        listing.setVenueName(venueName);
        listing.setAddress(address);
        listing.setLatitude(latitude);
        listing.setLongitude(longitude);
        // Ensure location is populated from address/venueName if it's somehow blank
        if (listing.getLocation() == null || listing.getLocation().isBlank()) {
            if (address != null && !address.isBlank()) {
                listing.setLocation(address.length() > 200 ? address.substring(0, 200) : address);
            } else if (venueName != null && !venueName.isBlank()) {
                listing.setLocation(venueName);
            }
        }
        // Truncate location to max 200 chars if needed
        if (listing.getLocation() != null && listing.getLocation().length() > 200) {
            listing.setLocation(listing.getLocation().substring(0, 200));
        }

        // Debug: Log final values before persist
        logger.info("[Create Listing Save] Final entity values - location={}, venueName={}, lat={}, lng={}",
                listing.getLocation(), listing.getVenueName(), listing.getLatitude(), listing.getLongitude());

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

        logger.info("Game listing created: ID={} | Creator={} | Sport={} | Date={} | Duration={}h",
                saved.getGameListingId(), creator.getUsername(),
                format.getSport().getSportName(), scheduledDate, sessionDuration);

        // Notify invited friends (invitation is courtesy only - does NOT auto-accept)
        if (invitedFriendIds != null && !invitedFriendIds.isEmpty()) {
            String notifText = creator.getUsername() + " invited you to a " +
                    format.getSport().getSportName() + " " + format.getFormatName() + " game on " +
                    scheduledDate.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM HH:mm")) +
                    ". Submit a join request to participate.";
            notificationService.createBulkNotifications(invitedFriendIds, notifText, NotificationType.LISTING_INVITE);

            // Record invitation history
            for (Long friendId : invitedFriendIds) {
                User invitedUser = userRepository.findById(friendId).orElse(null);
                if (invitedUser != null) {
                    ListingInvitation invitation = new ListingInvitation(saved, invitedUser, creator);
                    listingInvitationRepository.save(invitation);
                }
            }
        }

        return saved;
    }

    /**
     * Validates position selection rules.
     * - At least one position must be selected if sport has positions
     * - "Any Position" cannot be combined with specific positions
     */
    private void validatePositionSelection(List<Long> positionIds) {
        if (positionIds == null || positionIds.isEmpty()) {
            throw new BusinessRuleException("Please select at least one position for this sport format.");
        }

        // Look up the "Any Position" ID from the database
        Long anyPositionId = getAnyPositionId();

        if (anyPositionId != null) {
            boolean hasAnyPosition = positionIds.contains(anyPositionId);
            boolean hasSpecificPositions = positionIds.stream().anyMatch(id -> !id.equals(anyPositionId));

            if (hasAnyPosition && hasSpecificPositions) {
                throw new BusinessRuleException(
                        "'Any Position' cannot be combined with specific positions. " +
                        "Select either 'Any Position' alone or one or more specific positions.");
            }
        }
    }

    /**
     * Gets the ID of the "Any Position" position, or null if not found.
     */
    private Long getAnyPositionId() {
        return sportService.getAnyPositionId();
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
        return gameListingRepository.findAvailablePublicListings(formatIds, LocalDateTime.now(), userId, pageable);
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
        return gameListingRepository.findAvailablePublicListingsBySkill(formatIds, LocalDateTime.now(), userId, skillLevel, pageable);
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
                                     String location, SkillLevel skillLevel, PrivacySetting privacySetting,
                                     String venueName, String address, Double latitude, Double longitude) {
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

        // Update map location fields
        if (venueName != null) listing.setVenueName(venueName);
        if (address != null) listing.setAddress(address);
        if (latitude != null) listing.setLatitude(latitude);
        if (longitude != null) listing.setLongitude(longitude);

        // Keep location in sync with map address
        if (address != null && !address.isBlank()) {
            listing.setLocation(address.length() > 200 ? address.substring(0, 200) : address);
        } else if (venueName != null && !venueName.isBlank() && (listing.getLocation() == null || listing.getLocation().isBlank())) {
            listing.setLocation(venueName);
        }

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

    /**
     * Gets all active public listings that have location coordinates.
     * Used for Map View feature.
     */
    @Transactional(readOnly = true)
    public List<GameListing> getActiveListingsWithCoordinates() {
        return gameListingRepository.findActivePublicListingsWithCoordinates(LocalDateTime.now());
    }

    /**
     * Gets active public listings within a given radius (in km) of a point.
     * Uses Haversine formula for distance calculation.
     *
     * @param userLat  user's latitude
     * @param userLng  user's longitude
     * @param radiusKm radius in kilometres
     * @return list of listings with their calculated distances, sorted by distance
     */
    @Transactional(readOnly = true)
    public List<NearbyListingResult> getNearbyListings(double userLat, double userLng, double radiusKm) {
        List<GameListing> allWithCoords = gameListingRepository
                .findActivePublicListingsWithCoordinates(LocalDateTime.now());

        return allWithCoords.stream()
                .map(listing -> {
                    double dist = haversineDistance(userLat, userLng,
                            listing.getLatitude(), listing.getLongitude());
                    return new NearbyListingResult(listing, dist);
                })
                .filter(result -> result.getDistanceKm() <= radiusKm)
                .sorted((a, b) -> Double.compare(a.getDistanceKm(), b.getDistanceKm()))
                .toList();
    }

    /**
     * Haversine formula to calculate distance between two lat/lng points in km.
     */
    private double haversineDistance(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371.0; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * DTO holding a listing and its distance from the user.
     */
    public static class NearbyListingResult {
        private final GameListing listing;
        private final double distanceKm;

        public NearbyListingResult(GameListing listing, double distanceKm) {
            this.listing = listing;
            this.distanceKm = distanceKm;
        }

        public GameListing getListing() { return listing; }
        public double getDistanceKm() { return distanceKm; }
    }
}
