package com.codesphere.gameon.config;

import com.codesphere.gameon.controller.*;
import com.codesphere.gameon.dao.*;
import com.codesphere.gameon.exception.ApiException;
import com.codesphere.gameon.service.AuthService;
import com.codesphere.gameon.service.BrowseListingService;
import com.codesphere.gameon.service.GameListingService;
import com.codesphere.gameon.service.JoinRequestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.json.JavalinJackson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Configures and creates the Javalin application instance.
 * Handles JSON setup, static files, CORS, error handling, and route registration.
 */
public class JavalinConfig {

    private static final Logger logger = LoggerFactory.getLogger(JavalinConfig.class);

    private JavalinConfig() {
        // Static factory only
    }

    /**
     * Creates and configures the Javalin instance with all routes and middleware.
     */
    public static Javalin create(AppConfig appConfig, DatabaseConfig databaseConfig) {
        ObjectMapper objectMapper = createObjectMapper();

        Javalin app = Javalin.create(config -> {
            // JSON serialisation
            config.jsonMapper(new JavalinJackson(objectMapper, false));

            // Serve static frontend files (copied from frontend/ at build time)
            config.staticFiles.add("/public");

            // CORS for local development
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(rule -> {
                    rule.allowHost("http://localhost:7070", "http://127.0.0.1:7070");
                });
            });

            // Request logging in development
            if (!appConfig.isProduction()) {
                config.requestLogger.http((ctx, ms) ->
                        logger.debug("{} {} - {}ms", ctx.method(), ctx.path(), String.format("%.1f", ms))
                );
            }
        });

        // Central error handling
        registerErrorHandlers(app);

        // Register routes
        registerRoutes(app, databaseConfig);

        // Graceful shutdown
        app.events(event -> event.serverStopping(() -> {
            logger.info("Shutting down GameOn...");
            databaseConfig.close();
        }));

        return app;
    }

    /**
     * Registers all API route groups.
     * Add new controller registrations here as features are built.
     */
    private static void registerRoutes(Javalin app, DatabaseConfig databaseConfig) {
        // Health / status endpoint
        new HealthController(databaseConfig).register(app);

        // Authentication
        UserDao userDao = new UserDao(databaseConfig.getDataSource());
        AuthService authService = new AuthService(userDao);
        new AuthController(authService, userDao).register(app);

        // User sports (for Create Listing dropdown)
        SportDao sportDao = new SportDao(databaseConfig.getDataSource());
        new UserSportController(sportDao).register(app);

        // Sport formats and positions lookups
        SportFormatDao sportFormatDao = new SportFormatDao(databaseConfig.getDataSource());
        PositionDao positionDao = new PositionDao(databaseConfig.getDataSource());
        new SportController(sportFormatDao, positionDao).register(app);

        // Friends (mutual followers)
        FollowDao followDao = new FollowDao(databaseConfig.getDataSource());
        new FriendController(followDao).register(app);

        // Game Listings
        GameListingDao gameListingDao = new GameListingDao(databaseConfig.getDataSource());
        GameJoinerDao gameJoinerDao = new GameJoinerDao(databaseConfig.getDataSource());
        NotificationDao notificationDao = new NotificationDao(databaseConfig.getDataSource());
        InvitationDao invitationDao = new InvitationDao(databaseConfig.getDataSource());
        JoinRequestDao joinRequestDao = new JoinRequestDao(databaseConfig.getDataSource());
        GameListingService gameListingService = new GameListingService(
                databaseConfig.getDataSource(), sportDao, sportFormatDao, positionDao,
                gameListingDao, gameJoinerDao, followDao, notificationDao, invitationDao);
        BrowseListingService browseListingService = new BrowseListingService(
                sportDao, gameListingDao, gameJoinerDao, invitationDao, sportFormatDao, userDao, joinRequestDao);
        new GameListingController(gameListingService, browseListingService).register(app);

        // Join Requests
        JoinRequestService joinRequestService = new JoinRequestService(
                databaseConfig.getDataSource(), gameListingDao, gameJoinerDao, joinRequestDao,
                invitationDao, sportDao, sportFormatDao, positionDao, notificationDao);
        new JoinRequestController(joinRequestService).register(app);
    }

    /**
     * Central error handlers so no stack traces or internal details leak to the client.
     */
    private static void registerErrorHandlers(Javalin app) {
        // Handle our custom ApiException
        app.exception(ApiException.class, (e, ctx) -> {
            logger.warn("API error: {} - {}", e.getStatus(), e.getMessage());
            ctx.status(e.getStatus());
            ctx.json(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        });

        // Handle unexpected exceptions
        app.exception(Exception.class, (e, ctx) -> {
            logger.error("Unhandled exception on {} {}", ctx.method(), ctx.path(), e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(Map.of(
                    "success", false,
                    "error", "An unexpected error occurred. Please try again later."
            ));
        });

        // 404 handler
        app.error(HttpStatus.NOT_FOUND, ctx -> {
            ctx.json(Map.of(
                    "success", false,
                    "error", "The requested resource was not found."
            ));
        });
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
