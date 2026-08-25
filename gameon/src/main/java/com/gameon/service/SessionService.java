package com.gameon.service;

import com.gameon.exception.BusinessRuleException;
import com.gameon.exception.ResourceNotFoundException;
import com.gameon.model.entity.GameListing;
import com.gameon.model.entity.GameJoiner;
import com.gameon.model.entity.Session;
import com.gameon.model.entity.Sport;
import com.gameon.model.entity.User;
import com.gameon.model.entity.UserSportProfile;
import com.gameon.model.enums.JoinerStatus;
import com.gameon.model.enums.NotificationType;
import com.gameon.model.enums.SkillLevel;
import com.gameon.repository.GameJoinerRepository;
import com.gameon.repository.GameListingRepository;
import com.gameon.repository.SessionRepository;
import com.gameon.repository.SportRepository;
import com.gameon.repository.UserRepository;
import com.gameon.repository.UserSportProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service handling session confirmation (A700) and game reminders (A600).
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
    private final UserSportProfileRepository userSportProfileRepository;
    private final UserRepository userRepository;
    private final SportRepository sportRepository;
    private final NotificationService notificationService;

    public SessionService(SessionRepository sessionRepository,
                          GameListingRepository gameListingRepository,
                          GameJoinerRepository gameJoinerRepository,
                          UserSportProfileRepository userSportProfileRepository,
                          UserRepository userRepository,
                          SportRepository sportRepository,
                          NotificationService notificationService) {
        this.sessionRepository = sessionRepository;
        this.gameListingRepository = gameListingRepository;
        this.gameJoinerRepository = gameJoinerRepository;
        this.userSportProfileRepository = userSportProfileRepository;
        this.userRepository = userRepository;
        this.sportRepository = sportRepository;
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

        // Auto-create UserSportProfile for all locked participants who don't have one
        Long sportId = listing.getFormat().getSport().getSportId();
        SkillLevel listingSkillLevel = listing.getSkillLevel();
        List<GameJoiner> lockedParticipants = gameJoinerRepository.findByIdGameListingIdAndStatus(listingId, JoinerStatus.LOCKED);
        for (GameJoiner participant : lockedParticipants) {
            ensureSportProfileExists(participant.getUser().getUserId(), sportId, listingSkillLevel);
        }
        // Also ensure creator has a sport profile
        ensureSportProfileExists(listing.getCreator().getUserId(), sportId, listingSkillLevel);

        // Send game reminders (A600)
        List<Long> participantIds = gameJoinerRepository.findParticipants(listingId).stream()
                .map(gj -> gj.getUser().getUserId())
                .toList();

        String reminderText = "Reminder: Your " + listing.getFormat().getSport().getSportName() +
                " game at " + listing.getLocation() + " starts in 2 hours!";
        notificationService.createBulkNotifications(participantIds, reminderText, NotificationType.GAME_REMINDER);

        // Also notify the creator
        notificationService.createNotification(
                listing.getCreator().getUserId(), reminderText, NotificationType.GAME_REMINDER);

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

    /**
     * Ensures a UserSportProfile exists for the given user and sport.
     * If no profile exists, auto-creates one with:
     *   - wins = 0, losses = 0, winPercentage = 0
     *   - skillLevel = listing skill level (if provided) or BEGINNER default
     *
     * Called when participants are LOCKED to ensure stats can be tracked.
     */
    private void ensureSportProfileExists(Long userId, Long sportId, SkillLevel defaultSkillLevel) {
        if (userSportProfileRepository.existsByIdUserIdAndIdSportId(userId, sportId)) {
            return; // Profile already exists
        }

        logger.info("Auto-creating UserSportProfile for user {} and sport {} during session confirmation", userId, sportId);
        User user = userRepository.findById(userId).orElse(null);
        Sport sport = sportRepository.findById(sportId).orElse(null);

        if (user == null || sport == null) {
            logger.warn("Cannot auto-create sport profile: user or sport not found. userId={}, sportId={}", userId, sportId);
            return;
        }

        SkillLevel skillLevel = (defaultSkillLevel != null) ? defaultSkillLevel : SkillLevel.BEGINNER;
        UserSportProfile newProfile = new UserSportProfile(user, sport, skillLevel);
        userSportProfileRepository.save(newProfile);
    }
}
