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
 * Scheduled tasks for time-triggered listing lifecycle processing.
 *
 * New lifecycle phases:
 *   T-2h  — Confirmation Deadline: releases unconfirmed players, warns creator.
 *   T-1h  — Finalisation: confirms or cancels the listing, locks participants.
 *
 * Both run every minute to ensure timely processing.
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
     * Runs every minute. Processes listings that have reached the T-2h confirmation deadline
     * but have not yet reached T-1h finalisation.
     */
    @Scheduled(fixedRate = 60000)
    public void processConfirmationDeadlines() {
        List<GameListing> listings = listingLifecycleService.findListingsNeedingConfirmationDeadline();

        if (!listings.isEmpty()) {
            logger.info("Found {} listings needing confirmation deadline processing", listings.size());
            for (GameListing listing : listings) {
                try {
                    listingLifecycleService.processConfirmationDeadline(listing.getGameListingId());
                } catch (Exception e) {
                    logger.error("Failed to process confirmation deadline for listing {}: {}",
                            listing.getGameListingId(), e.getMessage());
                }
            }
        }
    }

    /**
     * Runs every minute. Processes listings that have reached (or passed) the T-1h finalisation point.
     */
    @Scheduled(fixedRate = 60000)
    public void finaliseListings() {
        List<GameListing> listings = listingLifecycleService.findListingsNeedingFinalisation();

        if (!listings.isEmpty()) {
            logger.info("Found {} listings needing finalisation", listings.size());
            for (GameListing listing : listings) {
                try {
                    listingLifecycleService.finaliseListing(listing.getGameListingId());
                } catch (Exception e) {
                    logger.error("Failed to finalise listing {}: {}",
                            listing.getGameListingId(), e.getMessage());
                }
            }
        }
    }
}
