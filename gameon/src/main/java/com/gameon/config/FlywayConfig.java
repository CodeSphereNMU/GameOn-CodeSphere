package com.gameon.config;

import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flyway configuration providing migration strategy and lifecycle callbacks.
 * Migrations are stored in classpath:db/migration and follow naming convention:
 * V{version}__{description}.sql
 *
 * Versioned migrations build the original schema, align the approved reference
 * data, and then apply the post-meeting workflow redesign and readable views.
 */
@Configuration
public class FlywayConfig {

    private static final Logger logger = LoggerFactory.getLogger(FlywayConfig.class);

    /**
     * Migration strategy that repairs broken migrations before migrating.
     * Useful during development when migration scripts may be modified.
     */
    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            logger.info("Starting Flyway migration for the configured database...");
            flyway.repair();
            flyway.migrate();
            logger.info("Flyway migration completed successfully");
        };
    }

    /**
     * Callback that logs migration lifecycle events for audit and troubleshooting.
     */
    @Bean
    public Callback flywayCallback() {
        return new Callback() {
            @Override
            public boolean supports(Event event, Context context) {
                return event == Event.AFTER_EACH_MIGRATE
                        || event == Event.AFTER_MIGRATE
                        || event == Event.AFTER_EACH_MIGRATE_ERROR;
            }

            @Override
            public boolean canHandleInTransaction(Event event, Context context) {
                return true;
            }

            @Override
            public String getCallbackName() {
                return "GameOnFlywayCallback";
            }

            @Override
            public void handle(Event event, Context context) {
                switch (event) {
                    case AFTER_EACH_MIGRATE -> {
                        if (context.getMigrationInfo() != null) {
                            logger.info("Applied migration: {} ({})",
                                    context.getMigrationInfo().getVersion(),
                                    context.getMigrationInfo().getDescription());
                        }
                    }
                    case AFTER_MIGRATE -> logger.info("All pending migrations applied successfully");
                    case AFTER_EACH_MIGRATE_ERROR -> {
                        if (context.getMigrationInfo() != null) {
                            logger.error("Migration FAILED: {} ({})",
                                    context.getMigrationInfo().getVersion(),
                                    context.getMigrationInfo().getDescription());
                        }
                    }
                    default -> { /* no action for other events */ }
                }
            }
        };
    }
}
