package com.gameon.service;

import com.gameon.model.entity.GameListing;
import com.gameon.model.enums.JoinerStatus;
import com.gameon.repository.GameJoinerRepository;
import com.gameon.repository.GameListingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Central service for scheduling conflict validation.
 * Implements the 60-minute travel buffer rule for all listing participation.
 *
 * Conflict algorithm:
 * - For each listing, the "blocked interval" is: [sessionStart, sessionEnd + 60 minutes]
 * - Two listings conflict if either listing's time window overlaps the other's blocked interval.
 */
@Service
public class SchedulingConflictService {

    private static final Logger logger = LoggerFactory.getLogger(SchedulingConflictService.class);
    private static final int TRAVEL_BUFFER_MINUTES = 60;
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private final GameListingRepository gameListingRepository;
    private final GameJoinerRepository gameJoinerRepository;

    public SchedulingConflictService(GameListingRepository gameListingRepository,
                                     GameJoinerRepository gameJoinerRepository) {
        this.gameListingRepository = gameListingRepository;
        this.gameJoinerRepository = gameJoinerRepository;
    }

    /**
     * Checks if a user has a scheduling conflict with a proposed session.
     *
     * @param userId         the user to check
     * @param newStart       proposed session start time
     * @param newDurationHrs proposed session duration in hours
     * @return Optional containing conflicting listing if conflict exists, empty otherwise
     */
    @Transactional(readOnly = true)
    public Optional<GameListing> findSchedulingConflict(Long userId, LocalDateTime newStart, int newDurationHrs) {
        return findSchedulingConflictMinutes(userId, newStart, newDurationHrs * 60, null);
    }

    /**
     * Checks if a user has a scheduling conflict, optionally excluding a specific listing
     * (useful when editing a listing to avoid self-conflict).
     *
     * @param userId            the user to check
     * @param newStart          proposed session start time
     * @param newDurationHrs    proposed session duration in hours
     * @param excludeListingId  listing ID to exclude from conflict check (may be null)
     * @return Optional containing conflicting listing if conflict exists, empty otherwise
     */
    @Transactional(readOnly = true)
    public Optional<GameListing> findSchedulingConflict(Long userId, LocalDateTime newStart,
                                                         int newDurationHrs, Long excludeListingId) {
        return findSchedulingConflictMinutes(userId, newStart, newDurationHrs * 60, excludeListingId);
    }

    @Transactional(readOnly = true)
    public Optional<GameListing> findSchedulingConflictMinutes(Long userId, LocalDateTime newStart,
                                                                int newDurationMinutes,
                                                                Long excludeListingId) {
        LocalDateTime newEnd = newStart.plusMinutes(newDurationMinutes);
        LocalDateTime newBlockedUntil = newEnd.plusMinutes(TRAVEL_BUFFER_MINUTES);

        // Get all upcoming listings where user is creator or accepted participant
        List<GameListing> userListings = gameListingRepository.findUpcomingListingsForUserAfter(userId, LocalDateTime.now());

        for (GameListing existing : userListings) {
            // Skip the listing being edited
            if (excludeListingId != null && excludeListingId.equals(existing.getGameListingId())) {
                continue;
            }

            LocalDateTime existingStart = existing.getScheduledDate();
            int existingDuration = existing.getDurationMinutes() != null ? existing.getDurationMinutes() : 60;
            LocalDateTime existingEnd = existingStart.plusMinutes(existingDuration);
            LocalDateTime existingBlockedUntil = existingEnd.plusMinutes(TRAVEL_BUFFER_MINUTES);

            // Check conflict in both directions:
            // 1. New session's window [newStart, newEnd] overlaps existing's blocked interval [existingStart, existingBlockedUntil]
            // 2. Existing session's window [existingStart, existingEnd] overlaps new's blocked interval [newStart, newBlockedUntil]
            boolean newOverlapsExistingBlocked = newStart.isBefore(existingBlockedUntil) && newEnd.isAfter(existingStart);
            boolean existingOverlapsNewBlocked = existingStart.isBefore(newBlockedUntil) && existingEnd.isAfter(newStart);

            if (newOverlapsExistingBlocked || existingOverlapsNewBlocked) {
                logger.debug("Scheduling conflict found for user {}: new [{} - {}] conflicts with listing {} [{} - {}]",
                        userId, newStart, newEnd, existing.getGameListingId(), existingStart, existingEnd);
                return Optional.of(existing);
            }
        }

        return Optional.empty();
    }

    /**
     * Checks for scheduling conflict and returns a user-friendly error message.
     *
     * @return error message if conflict exists, null otherwise
     */
    @Transactional(readOnly = true)
    public String getConflictMessage(Long userId, LocalDateTime newStart, int newDurationHrs, Long excludeListingId) {
        return getConflictMessageMinutes(userId, newStart, newDurationHrs * 60, excludeListingId);
    }

    @Transactional(readOnly = true)
    public String getConflictMessageMinutes(Long userId, LocalDateTime newStart,
                                            int newDurationMinutes, Long excludeListingId) {
        Optional<GameListing> conflict = findSchedulingConflictMinutes(
                userId, newStart, newDurationMinutes, excludeListingId);
        if (conflict.isPresent()) {
            GameListing conflicting = conflict.get();
            String existingTime = conflicting.getScheduledDate().format(DISPLAY_FORMAT);
            int duration = conflicting.getDurationMinutes() != null ? conflicting.getDurationMinutes() : 60;
            String endTime = conflicting.getScheduledDate().plusMinutes(duration).format(DISPLAY_FORMAT);
            return "Your new listing conflicts with another upcoming session (" +
                    conflicting.getFormat().getSport().getSportName() + " on " +
                    existingTime + " - " + endTime + "). " +
                    "Sessions require a 60-minute travel buffer between them.";
        }
        return null;
    }

    /**
     * Returns true if a conflict exists.
     */
    @Transactional(readOnly = true)
    public boolean hasSchedulingConflict(Long userId, LocalDateTime newStart, int newDurationHrs) {
        return findSchedulingConflict(userId, newStart, newDurationHrs).isPresent();
    }
}
