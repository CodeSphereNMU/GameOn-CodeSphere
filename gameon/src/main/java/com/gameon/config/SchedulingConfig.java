package com.gameon.config;

import com.gameon.model.entity.GameListing;
import com.gameon.service.SessionService;
import com.gameon.service.WeatherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

/**
 * Scheduled tasks for time-triggered features.
 * A500: Expired listings are hidden via query filter (scheduledDate > now).
 * A600: Game reminders sent when session is confirmed.
 * A700: Session confirmation 2 hours before scheduled time.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {

    private static final Logger logger = LoggerFactory.getLogger(SchedulingConfig.class);

    private final SessionService sessionService;
    private final WeatherService weatherService;

    public SchedulingConfig(SessionService sessionService, WeatherService weatherService) {
        this.sessionService = sessionService;
        this.weatherService = weatherService;
    }

    /**
     * Runs every 15 minutes to check for listings needing session confirmation.
     * A700: If a listing is full and within 2 hours of start, confirm and lock users.
     * A600: Sends game reminder notifications to all participants.
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

    /**
     * Runs every 6 hours to refresh weather forecasts for upcoming listings.
     * Updates forecasts for listings occurring within the next 7 days.
     * Does not modify completed or cancelled listings.
     */
    @Scheduled(fixedRate = 21600000) // 6 hours in milliseconds
    public void refreshWeatherForecasts() {
        logger.info("Starting scheduled weather forecast refresh...");
        try {
            int updated = weatherService.refreshUpcomingForecasts();
            logger.info("Weather forecast refresh completed: {} listings updated", updated);
        } catch (Exception e) {
            logger.error("Weather forecast refresh failed: {}", e.getMessage());
        }
    }
}
