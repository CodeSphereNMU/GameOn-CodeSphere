package com.gameon.service;

import com.gameon.exception.BusinessRuleException;
import com.gameon.exception.ResourceNotFoundException;
import com.gameon.model.entity.GameListing;
import com.gameon.model.entity.Session;
import com.gameon.model.enums.JoinerStatus;
import com.gameon.model.enums.NotificationType;
import com.gameon.repository.GameJoinerRepository;
import com.gameon.repository.GameListingRepository;
import com.gameon.repository.SessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service handling session confirmation (A700) and game reminders (A500).
 * BR3: Only one session can be scheduled from a Game Listing.
 * BR6: Users 2 hours before scheduled time are locked in.
 */
@Service
public class SessionService {

    private static final Logger logger = LoggerFactory.getLogger(SessionService.class);

    private static final int CONFIRMATION_HOURS_BEFORE = 2;

    private final SessionRepository sessionRepository;
    private final GameListingRepository gameListingRepository;
    private final GameJoinerRepository gameJoinerRepository;
    private final NotificationService notificationService;

    public SessionService(SessionRepository sessionRepository,
                          GameListingRepository gameListingRepository,
                          GameJoinerRepository gameJoinerRepository,
                          NotificationService notificationService) {
        this.sessionRepository = sessionRepository;
        this.gameListingRepository = gameListingRepository;
        this.gameJoinerRepository = gameJoinerRepository;
        this.notificationService = notificationService;
    }

    /**
     * Confirms a session for a listing (A700).
     * BR3: Only one session per listing.
     * BR6: Locks all accepted joiners.
     */
    @Transactional
    public Session confirmSession(Long listingId) {
        GameListing listing = gameListingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Game Listing", listingId));

        // BR3: Check if session already exists
        if (sessionRepository.existsByGameListingGameListingId(listingId)) {
            throw new BusinessRuleException("A session has already been confirmed for this listing.", "BR3");
        }

        if (listing.getIsCompleted()) {
            throw new BusinessRuleException("Cannot confirm session for a completed listing.");
        }

        // Create session record
        Session session = new Session(listing);
        Session saved = sessionRepository.save(session);

        // BR6: Lock all accepted joiners
        int lockedCount = gameJoinerRepository.lockAllAcceptedJoiners(listingId);

        // Send game reminders (A500)
        List<Long> participantIds = gameJoinerRepository.findParticipants(listingId).stream()
                .map(gj -> gj.getUser().getUserId())
                .toList();

        String reminderText = "Reminder: Your " + listing.getFormat().getSport().getSportName() +
                " game at " + listing.getLocation() + " starts in 2 hours!";
        notificationService.createBulkNotifications(participantIds, reminderText, NotificationType.GAME_REMINDER);

        logger.info("Session confirmed for listing {}: {} players locked", listingId, lockedCount);
        return saved;
    }

    /**
     * Finds listings that need session confirmation (scheduled within 2 hours).
     * Called by a scheduled task or manually triggered.
     */
    @Transactional(readOnly = true)
    public List<GameListing> findListingsNeedingConfirmation() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.plusHours(CONFIRMATION_HOURS_BEFORE);
        return gameListingRepository.findFullListingsNeedingConfirmation(now, threshold);
    }

    /**
     * Gets the session for a listing, if it exists.
     */
    @Transactional(readOnly = true)
    public Session getSessionForListing(Long listingId) {
        return sessionRepository.findByGameListingGameListingId(listingId)
                .orElse(null);
    }

    /**
     * Checks if a listing has a confirmed session.
     */
    @Transactional(readOnly = true)
    public boolean isSessionConfirmed(Long listingId) {
        return sessionRepository.existsByGameListingGameListingId(listingId);
    }

    /**
     * Gets upcoming sessions within a time range (for dashboard display).
     */
    @Transactional(readOnly = true)
    public List<Session> getUpcomingSessions(LocalDateTime start, LocalDateTime end) {
        return sessionRepository.findUpcomingSessions(start, end);
    }
}
