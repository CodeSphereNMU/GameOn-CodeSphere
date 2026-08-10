package com.codesphere.gameon.service;

import com.codesphere.gameon.dao.*;
import com.codesphere.gameon.dto.JoinRequestRequest;
import com.codesphere.gameon.dto.JoinRequestResponse;
import com.codesphere.gameon.exception.ApiException;
import com.codesphere.gameon.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Business logic for creating join requests (A200).
 * Performs all validation and executes the transactional insert of:
 * join_request + creator notification.
 */
public class JoinRequestService {

    private static final Logger logger = LoggerFactory.getLogger(JoinRequestService.class);
    private static final Set<String> VALID_TEAMS = Set.of("A", "B");
    static final long LOCK_IN_HOURS = 2;

    private final DataSource dataSource;
    private final GameListingDao gameListingDao;
    private final GameJoinerDao gameJoinerDao;
    private final JoinRequestDao joinRequestDao;
    private final InvitationDao invitationDao;
    private final SportDao sportDao;
    private final SportFormatDao sportFormatDao;
    private final PositionDao positionDao;
    private final NotificationDao notificationDao;
    private final Clock clock;

    /**
     * Production constructor — uses the system default clock.
     */
    public JoinRequestService(DataSource dataSource, GameListingDao gameListingDao,
                              GameJoinerDao gameJoinerDao, JoinRequestDao joinRequestDao,
                              InvitationDao invitationDao, SportDao sportDao,
                              SportFormatDao sportFormatDao, PositionDao positionDao,
                              NotificationDao notificationDao) {
        this(dataSource, gameListingDao, gameJoinerDao, joinRequestDao, invitationDao,
                sportDao, sportFormatDao, positionDao, notificationDao, Clock.systemDefaultZone());
    }

    /**
     * Test constructor — accepts an injectable Clock for deterministic time control.
     */
    public JoinRequestService(DataSource dataSource, GameListingDao gameListingDao,
                              GameJoinerDao gameJoinerDao, JoinRequestDao joinRequestDao,
                              InvitationDao invitationDao, SportDao sportDao,
                              SportFormatDao sportFormatDao, PositionDao positionDao,
                              NotificationDao notificationDao, Clock clock) {
        this.dataSource = dataSource;
        this.gameListingDao = gameListingDao;
        this.gameJoinerDao = gameJoinerDao;
        this.joinRequestDao = joinRequestDao;
        this.invitationDao = invitationDao;
        this.sportDao = sportDao;
        this.sportFormatDao = sportFormatDao;
        this.positionDao = positionDao;
        this.notificationDao = notificationDao;
        this.clock = clock;
    }

    /**
     * Creates a join request with full validation and transactional persistence.
     *
     * @param userId    the authenticated user's ID (from session)
     * @param listingId the game listing to join
     * @param request   the join request payload
     * @return the response with the created join request details
     */
    public JoinRequestResponse createJoinRequest(long userId, long listingId, JoinRequestRequest request) {
        // --- Pre-transaction validation (fail fast, no DB connection held) ---

        // 1. Team validation
        if (request.getTeam() == null || request.getTeam().isBlank()) {
            throw ApiException.badRequest("Team selection is required (A or B)");
        }
        String team = request.getTeam().trim().toUpperCase();
        if (!VALID_TEAMS.contains(team)) {
            throw ApiException.badRequest("Team selection is required (A or B)");
        }

        // 2. Fetch listing (404 if not found)
        GameListing listing = gameListingDao.findById(listingId)
                .orElseThrow(() -> ApiException.notFound("Listing not found"));

        // 3. Creator check (cannot join own listing)
        if (listing.getCreatorId() == userId) {
            throw ApiException.badRequest("Cannot join your own listing");
        }

        // 4. Status check (must be OPEN)
        if (!"OPEN".equals(listing.getStatus())) {
            throw ApiException.badRequest("Listing is not open for join requests");
        }

        // 5. Lock-in check (must be MORE than 2 hours before start; exactly 2h = rejected)
        LocalDateTime now = LocalDateTime.now(clock);
        if (Duration.between(now, listing.getDate()).getSeconds() <= LOCK_IN_HOURS * 3600) {
            throw ApiException.badRequest("Listing has passed lock-in and is no longer accepting requests");
        }

        // 6. Fetch format (for position rules)
        SportFormat format = sportFormatDao.findById(listing.getFormatId())
                .orElseThrow(() -> ApiException.badRequest("Format not found"));

        // 7. Position validation (format-dependent)
        Long positionId = request.getPositionId();
        Long alternatePositionId = request.getAlternatePositionId();
        boolean anyPosition = request.isAnyPosition();

        if (!format.isHasPositions()) {
            // Non-positional format: ignore any submitted positions, store as NULL
            positionId = null;
            alternatePositionId = null;
        } else if (anyPosition) {
            // Any Position selected: both positions must be null
            if (positionId != null || alternatePositionId != null) {
                throw ApiException.badRequest("Position IDs must be null when Any Position is selected");
            }
            positionId = null;
            alternatePositionId = null;
        } else {
            // Positional format with specific position: position_id is required
            if (alternatePositionId != null && positionId == null) {
                throw ApiException.badRequest("Alternate position requires a primary position selection");
            }
            if (positionId == null) {
                throw ApiException.badRequest("A position selection is required for this format");
            }
            if (!positionDao.positionBelongsToFormat(positionId, format.getFormatId())) {
                throw ApiException.badRequest("Selected position does not belong to the chosen format");
            }
            if (alternatePositionId != null) {
                if (alternatePositionId.equals(positionId)) {
                    throw ApiException.badRequest("First and second position preferences must be different");
                }
                if (!positionDao.positionBelongsToFormat(alternatePositionId, format.getFormatId())) {
                    throw ApiException.badRequest("Selected alternate position does not belong to the chosen format");
                }
            }
        }

        // Final position values for the transactional section
        final Long finalPositionId = positionId;
        final Long finalAlternatePositionId = alternatePositionId;

        // --- Transactional section ---
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 8a. Invitation lookup (using txn connection)
                Long invitationId = invitationDao.findPendingInvitationId(conn, listingId, userId);

                // 8b. Sport on profile check (bypassed if invitation found)
                if (invitationId == null) {
                    if (!sportDao.userHasSport(userId, format.getSportId())) {
                        throw ApiException.badRequest("Selected sport is not on your profile");
                    }
                }

                // 8c. Scheduling conflict check
                boolean hasConflict = gameListingDao.hasSchedulingConflict(conn, userId, listing.getDate(), listing.getEndTime());
                if (hasConflict) {
                    throw ApiException.badRequest("Scheduling conflict: the proposed session overlaps with an existing session and its travel buffer");
                }

                // 8d. Already-accepted check (game_joiner with ACCEPTED)
                if (gameJoinerDao.isAcceptedJoiner(conn, listingId, userId)) {
                    throw ApiException.badRequest("You are already a participant in this listing");
                }

                // 8e. Already-pending check (join_request with PENDING)
                if (joinRequestDao.hasPendingRequest(conn, listingId, userId)) {
                    throw ApiException.badRequest("You already have a pending request for this listing");
                }

                // 8f. Insert join_request (status=PENDING, invitation_id from step a)
                JoinRequest joinRequest = new JoinRequest();
                joinRequest.setGameListingId(listingId);
                joinRequest.setUserId(userId);
                joinRequest.setFormatId(format.getFormatId());
                joinRequest.setTeam(team);
                joinRequest.setPositionId(finalPositionId);
                joinRequest.setAlternatePositionId(finalAlternatePositionId);
                joinRequest.setInvitationId(invitationId);
                joinRequest.setStatus("PENDING");

                long joinRequestId = joinRequestDao.insert(conn, joinRequest);

                // 8g. Insert notification for listing creator (type="join_request")
                Notification notification = new Notification();
                notification.setRead(false);
                notification.setText("A player has requested to join your listing");
                notification.setTypeOfNotification("join_request");
                notification.setRecipientId(listing.getCreatorId());
                notification.setGameListingId(listingId);
                notificationDao.insertBatch(conn, List.of(notification));

                conn.commit();

                // Build response
                JoinRequestResponse response = new JoinRequestResponse();
                response.setJoinRequestId(joinRequestId);
                response.setGameListingId(listingId);
                response.setTeam(team);
                response.setPositionId(finalPositionId);
                response.setAlternatePositionId(finalAlternatePositionId);
                response.setStatus("PENDING");
                response.setInvitationLinked(invitationId != null);

                logger.info("Join request created: id={}, listing={}, user={}, team={}, invitationLinked={}",
                        joinRequestId, listingId, userId, team, invitationId != null);
                return response;

            } catch (ApiException e) {
                conn.rollback();
                throw e;
            } catch (Exception e) {
                conn.rollback();
                logger.error("Transaction failed during join request creation for user: {} on listing: {}", userId, listingId, e);
                throw new RuntimeException("Failed to create join request", e);
            }
        } catch (ApiException e) {
            throw e;
        } catch (SQLException e) {
            logger.error("Database connection error during join request creation for user: {} on listing: {}", userId, listingId, e);
            throw new RuntimeException("Database error", e);
        }
    }
}
