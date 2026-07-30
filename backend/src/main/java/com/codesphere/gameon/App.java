package com.codesphere.gameon;

import com.codesphere.gameon.config.AppConfig;
import com.codesphere.gameon.config.DatabaseConfig;
import com.codesphere.gameon.config.JavalinConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application entry point for GameOn.
 * Initialises configuration, database connection pool, runs migrations,
 * and starts the Javalin HTTP server.
 */
public class App {

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        logger.info("Starting GameOn application...");

        AppConfig appConfig = new AppConfig();
        DatabaseConfig databaseConfig = new DatabaseConfig(appConfig);

        // Run Flyway migrations
        databaseConfig.runMigrations();

        // Start Javalin server
        var app = JavalinConfig.create(appConfig, databaseConfig);
        int port = appConfig.getAppPort();
        app.start(port);

        logger.info("GameOn is running on http://localhost:{}", port);
    }
}
