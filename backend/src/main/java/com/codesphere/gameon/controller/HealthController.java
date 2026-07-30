package com.codesphere.gameon.controller;

import com.codesphere.gameon.config.DatabaseConfig;
import com.codesphere.gameon.dto.ApiResponse;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.Map;

/**
 * Provides health and status endpoints.
 * Useful for verifying the server is running and the database is reachable.
 */
public class HealthController {

    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);

    private final DatabaseConfig databaseConfig;

    public HealthController(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    public void register(Javalin app) {
        app.get("/api/health", ctx -> {
            boolean dbHealthy = checkDatabase();
            Map<String, Object> healthData = Map.of(
                    "status", dbHealthy ? "healthy" : "degraded",
                    "database", dbHealthy ? "connected" : "unreachable",
                    "application", "GameOn",
                    "version", "1.0-SNAPSHOT"
            );
            ctx.json(ApiResponse.success(healthData));
        });
    }

    private boolean checkDatabase() {
        try (Connection conn = databaseConfig.getDataSource().getConnection()) {
            return conn.isValid(3);
        } catch (Exception e) {
            logger.warn("Database health check failed: {}", e.getMessage());
            return false;
        }
    }
}
