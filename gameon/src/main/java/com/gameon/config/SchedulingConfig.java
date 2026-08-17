package com.gameon.config;

import com.gameon.model.entity.GameListing;
import com.gameon.service.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

/**
 * Scheduled tasks for time-triggered features.
 * A500: Game reminders sent when session is confirmed.
 * A600: Listings are hidden from Browse at the two-hour lock-in point.
 * A700: Session confirmation 2 hours before scheduled time.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {

    private static final Logger logger = LoggerFactory.getLogger(SchedulingConfig.class);

    private final SessionService sessionService;

    public SchedulingConfig(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    /**
     * Runs every 15 minutes to check for listings needing session confirmation.
     * A700: If a listing is full and within 2 hours of start, confirm and lock users.
     * A500: Sends game reminder notifications to all participants.
     */
    @Scheduled(fixedRate = 900000) // 15 minutes
    public void confirmUpcomingSessions() {
        List<GameListing> listings = sessionService.findListingsNeedingConfirmation();

        if (!listings.isEmpty()) {
            logger.info("Found {} listings needing session confirmation", listings.size());
            for (GameListing listing : listings) {
                try {
                    sessionService.confirmSession(listing.getGameListingId());
                } catch (Exception e) {
                    logger.error("Failed to confirm session for listing {}: {}",
                            listing.getGameListingId(), e.getMessage());
                }
            }
        }
    }
}
