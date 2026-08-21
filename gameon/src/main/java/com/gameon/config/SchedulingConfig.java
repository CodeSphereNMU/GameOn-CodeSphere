package com.gameon.config;

import com.gameon.model.entity.GameListing;
import com.gameon.service.ListingLifecycleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

/**
 * Scheduled tasks for time-triggered features.
 * A500: Confirmation notifications sent at lock-in.
 * A600: Listings are hidden from Browse at the two-hour lock-in point.
 * A700: Listing confirmation/cancellation 2 hours before scheduled time.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {

    private static final Logger logger = LoggerFactory.getLogger(SchedulingConfig.class);

    private final ListingLifecycleService listingLifecycleService;

    public SchedulingConfig(ListingLifecycleService listingLifecycleService) {
        this.listingLifecycleService = listingLifecycleService;
    }

    /**
     * Runs every minute to process listings at the two-hour lock-in point.
     * Full listings are confirmed and locked; incomplete listings are cancelled.
     */
    @Scheduled(fixedRate = 60000) // 1 minute
    public void confirmUpcomingSessions() {
        List<GameListing> listings = listingLifecycleService.findListingsNeedingLockIn();

        if (!listings.isEmpty()) {
            logger.info("Found {} listings needing lock-in processing", listings.size());
            for (GameListing listing : listings) {
                try {
                    listingLifecycleService.lockInListing(listing.getGameListingId());
                } catch (Exception e) {
                    logger.error("Failed to process lock-in for listing {}: {}",
                            listing.getGameListingId(), e.getMessage());
                }
            }
        }
    }
}
