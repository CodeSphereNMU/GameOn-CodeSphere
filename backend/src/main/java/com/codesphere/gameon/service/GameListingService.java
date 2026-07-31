package com.codesphere.gameon.service;

import com.codesphere.gameon.dao.*;
import com.codesphere.gameon.dto.CreateListingRequest;
import com.codesphere.gameon.dto.CreateListingResponse;
import com.codesphere.gameon.exception.ApiException;
import com.codesphere.gameon.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Business logic for creating game listings (A100).
 * Performs all validation and executes the transactional insert of:
 * game_listing + creator game_joiner + invitation notifications.
 */
public class GameListingService {

    private static final Logger logger = LoggerFactory.getLogger(GameListingService.class);
    private static final Set<String> VALID_SKILL_LEVELS = Set.of("Beginner", "Intermediate", "Advanced");

    /**
     * Provisional: minimum hours before a listing's start time that it must be created.
     * Awaiting group confirmation — the value may change.
     */
    static final long MINIMUM_LISTING_LEAD_TIME_HOURS = 3;

    private final DataSource dataSource;
    private final SportDao sportDao;
    private final SportFormatDao sportFormatDao;
    private final PositionDao positionDao;
    private final GameListingDao gameListingDao;
    private final GameJoinerDao gameJoinerDao;
    private final FollowDao followDao;
    private final NotificationDao notificationDao;
    private final Clock clock;

    /**
     * Production constructor — uses the system default clock.
     */
    public GameListingService(DataSource dataSource, SportDao sportDao, SportFormatDao sportFormatDao,
                              PositionDao positionDao, GameListingDao gameListingDao,
                              GameJoinerDao gameJoinerDao, FollowDao followDao,
                              NotificationDao notificationDao) {
        this(dataSource, sportDao, sportFormatDao, positionDao, gameListingDao,
                gameJoinerDao, followDao, notificationDao, Clock.systemDefaultZone());
    }

    /**
     * Test constructor — accepts an injectable Clock for deterministic time control.
     */
    public GameListingService(DataSource dataSource, SportDao sportDao, SportFormatDao sportFormatDao,
                              PositionDao positionDao, GameListingDao gameListingDao,
                              GameJoinerDao gameJoinerDao, FollowDao followDao,
                              NotificationDao notificationDao, Clock clock) {
        this.dataSource = dataSource;
        this.sportDao = sportDao;
        this.sportFormatDao = sportFormatDao;
        this.positionDao = positionDao;
        this.gameListingDao = gameListingDao;
        this.gameJoinerDao = gameJoinerDao;
        this.followDao = followDao;
        this.notificationDao = notificationDao;
        this.clock = clock;
    }

    /**
     * Creates a game listing with full validation and transactional persistence.
     *
     * @param userId  the authenticated user's ID (from session)
     * @param request the creation request payload
     * @return the response with the created listing details
     */
    public CreateListingResponse createListing(long userId, CreateListingRequest request) {
        // --- Input validation ---
        validateRequired(request);

        // Validate skill level
        if (!VALID_SKILL_LEVELS.contains(request.getSkillLevel())) {
            throw ApiException.badRequest("Invalid skill level. Must be one of: Beginner, Intermediate, Advanced");
        }

        // Validate location
        if (request.getLocation() == null || request.getLocation().isBlank()) {
            throw ApiException.badRequest("Location is required");
        }

        // Parse and validate date/time
        LocalDateTime dateTime = parseDateTime(request.getDate(), request.getTime());
        LocalDateTime now = LocalDateTime.now(clock);

        if (!dateTime.isAfter(now)) {
            throw ApiException.badRequest("Date and time must be in the future");
        }

        // Minimum advance-booking rule (provisional — awaiting group confirmation)
        long secondsUntilStart = Duration.between(now, dateTime).getSeconds();
        long minimumLeadTimeSeconds = MINIMUM_LISTING_LEAD_TIME_HOURS * 3600;
        if (secondsUntilStart < minimumLeadTimeSeconds) {
            throw ApiException.badRequest("A game listing must be created at least "
                    + MINIMUM_LISTING_LEAD_TIME_HOURS + " hours before its start time.");
        }

        // Validate sport belongs to user's profile
        if (!sportDao.userHasSport(userId, request.getSportId())) {
            throw ApiException.badRequest("Selected sport is not on your profile");
        }

        // Validate format belongs to sport
        SportFormat format = sportFormatDao.findById(request.getFormatId())
                .orElseThrow(() -> ApiException.badRequest("Selected format does not exist"));
        if (format.getSportId() != request.getSportId()) {
            throw ApiException.badRequest("Selected format does not belong to the selected sport");
        }

        // Validate positions
        validatePositions(request, format);

        // Validate invitations
        int maxInvitations = format.getNoPlayers() - 1; // creator occupies one slot
        List<Long> invitedFriendIds = validateInvitations(request, userId, maxInvitations);

        // --- Transactional creation ---
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Check scheduling conflict within transaction
                boolean hasConflict = gameListingDao.hasSchedulingConflict(conn, userId, dateTime);
                if (hasConflict) {
                    throw ApiException.badRequest("Scheduling conflict: you have an incomplete listing within 2 hours of this time");
                }

                // Insert game_listing
                GameListing listing = new GameListing();
                listing.setDate(dateTime);
                listing.setCompleted(false);
                listing.setPrivate(request.getIsPrivate() != null && request.getIsPrivate());
                listing.setLocation(request.getLocation().trim());
                listing.setSkillLevel(request.getSkillLevel());
                listing.setCreatorId(userId);
                listing.setFormatId(request.getFormatId());

                long listingId = gameListingDao.insert(conn, listing);

                // Insert creator as accepted game_joiner
                GameJoiner creatorJoiner = new GameJoiner();
                creatorJoiner.setGameListingId(listingId);
                creatorJoiner.setUserId(userId);
                creatorJoiner.setTeam("A");
                creatorJoiner.setStatus("accepted");
                creatorJoiner.setFormatId(format.getFormatId());

                if (request.getPositionId() != null) {
                    creatorJoiner.setPositionId(request.getPositionId());
                }
                if (request.getAlternatePositionId() != null) {
                    creatorJoiner.setAlternateFormatPosition(String.valueOf(request.getAlternatePositionId()));
                }

                gameJoinerDao.insertCreator(conn, creatorJoiner);

                // Insert invitation notifications
                if (!invitedFriendIds.isEmpty()) {
                    // Derive listing title
                    List<Sport> userSports = sportDao.findSportsByUserId(userId);
                    String sportName = userSports.stream()
                            .filter(s -> s.getSportId() == request.getSportId())
                            .map(Sport::getSportName)
                            .findFirst()
                            .orElse("Unknown");
                    String listingTitle = sportName + " " + format.getFormatName();

                    List<Notification> notifications = new ArrayList<>();
                    for (Long friendId : invitedFriendIds) {
                        Notification n = new Notification();
                        n.setRead(false);
                        n.setText("You've been invited to " + listingTitle + " at " + listing.getLocation());
                        n.setTypeOfNotification("game_invitation");
                        n.setRecipientId(friendId);
                        notifications.add(n);
                    }
                    notificationDao.insertBatch(conn, notifications);
                }

                conn.commit();

                // Build response
                List<Sport> userSports = sportDao.findSportsByUserId(userId);
                String sportName = userSports.stream()
                        .filter(s -> s.getSportId() == request.getSportId())
                        .map(Sport::getSportName)
                        .findFirst()
                        .orElse("Unknown");

                CreateListingResponse response = new CreateListingResponse();
                response.setGameListingId(listingId);
                response.setSportName(sportName);
                response.setFormatName(format.getFormatName());
                response.setSkillLevel(request.getSkillLevel());
                response.setDate(dateTime.toString());
                response.setLocation(listing.getLocation());
                response.setPrivate(listing.isPrivate());
                response.setCapacity(format.getNoPlayers());
                response.setInvitedCount(invitedFriendIds.size());

                logger.info("Game listing created: id={}, creator={}, sport={} {}",
                        listingId, userId, sportName, format.getFormatName());
                return response;

            } catch (ApiException e) {
                conn.rollback();
                throw e;
            } catch (Exception e) {
                conn.rollback();
                logger.error("Transaction failed during listing creation for user: {}", userId, e);
                throw new RuntimeException("Failed to create listing", e);
            }
        } catch (ApiException e) {
            throw e;
        } catch (SQLException e) {
            logger.error("Database connection error during listing creation for user: {}", userId, e);
            throw new RuntimeException("Database error", e);
        }
    }

    private void validateRequired(CreateListingRequest request) {
        if (request.getSportId() == null) {
            throw ApiException.badRequest("Sport is required");
        }
        if (request.getFormatId() == null) {
            throw ApiException.badRequest("Format is required");
        }
        if (request.getSkillLevel() == null || request.getSkillLevel().isBlank()) {
            throw ApiException.badRequest("Skill level is required");
        }
        if (request.getDate() == null || request.getDate().isBlank()) {
            throw ApiException.badRequest("Date is required");
        }
        if (request.getTime() == null || request.getTime().isBlank()) {
            throw ApiException.badRequest("Time is required");
        }
        if (request.getLocation() == null || request.getLocation().isBlank()) {
            throw ApiException.badRequest("Location is required");
        }
    }

    private LocalDateTime parseDateTime(String dateStr, String timeStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr);
            LocalTime time = LocalTime.parse(timeStr);
            return LocalDateTime.of(date, time);
        } catch (DateTimeParseException e) {
            throw ApiException.badRequest("Invalid date or time format. Use ISO format: date=YYYY-MM-DD, time=HH:MM");
        }
    }

    private void validatePositions(CreateListingRequest request, SportFormat format) {
        Long posId = request.getPositionId();
        Long altPosId = request.getAlternatePositionId();
        boolean anyPos = Boolean.TRUE.equals(request.getAnyPosition());

        if (!format.isHasPositions()) {
            // Format doesn't use positions — silently ignore any submitted values
            request.setPositionId(null);
            request.setAlternatePositionId(null);
            request.setAnyPosition(null);
            return;
        }

        // Positional format: user must explicitly choose "Any Position" OR at least one real position
        if (anyPos) {
            // "Any Position" must be mutually exclusive with real positions
            if (posId != null || altPosId != null) {
                throw ApiException.badRequest("Cannot select both 'Any Position' and a specific position");
            }
            // Valid: explicit "Any Position" preference → position_id stays NULL in game_joiner
            return;
        }

        // Not "Any Position" — a real position is required
        if (posId == null) {
            throw ApiException.badRequest("A position selection is required for this format");
        }

        // Primary position must belong to format
        if (!positionDao.positionBelongsToFormat(posId, format.getFormatId())) {
            throw ApiException.badRequest("Selected position does not belong to the chosen format");
        }

        // If alternate position is provided
        if (altPosId != null) {
            if (altPosId.equals(posId)) {
                throw ApiException.badRequest("First and second position preferences must be different");
            }
            if (!positionDao.positionBelongsToFormat(altPosId, format.getFormatId())) {
                throw ApiException.badRequest("Selected alternate position does not belong to the chosen format");
            }
        }
    }

    private List<Long> validateInvitations(CreateListingRequest request, long userId, int maxInvitations) {
        List<Long> invitedFriendIds = request.getInvitedFriendIds();
        if (invitedFriendIds == null || invitedFriendIds.isEmpty()) {
            return List.of();
        }

        // Check for duplicates
        Set<Long> uniqueIds = new HashSet<>(invitedFriendIds);
        if (uniqueIds.size() != invitedFriendIds.size()) {
            throw ApiException.badRequest("Duplicate friend IDs in invitation list");
        }

        // Cannot invite self
        if (uniqueIds.contains(userId)) {
            throw ApiException.badRequest("Cannot invite yourself to your own listing");
        }

        // Check capacity
        if (invitedFriendIds.size() > maxInvitations) {
            throw ApiException.badRequest("Too many invitations. Maximum is " + maxInvitations);
        }

        // All invited IDs must be mutual friends
        Set<Long> mutualFriendIds = followDao.findMutualFollowerIds(userId);
        for (Long friendId : invitedFriendIds) {
            if (!mutualFriendIds.contains(friendId)) {
                throw ApiException.badRequest("User " + friendId + " is not a mutual friend");
            }
        }

        return invitedFriendIds;
    }
}
