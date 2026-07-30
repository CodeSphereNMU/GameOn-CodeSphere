package com.codesphere.gameon.config;

import io.github.cdimascio.dotenv.Dotenv;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads application configuration from environment variables (with .env fallback).
 * All configuration access goes through this class so secrets stay in one place.
 *
 * The .env file lives at the repository root (one level above backend/).
 * When the app runs from backend/ (the normal case with Maven or java -jar),
 * this class looks for ../.env relative to the working directory.
 * If .env is in the current directory instead (e.g. running from repo root), that works too.
 * System environment variables always take final precedence.
 */
public class AppConfig {

    private final Dotenv dotenv;

    public AppConfig() {
        this.dotenv = loadDotenv();
    }

    // --- Database ---

    public String getDbHost() {
        return getOrDefault("DB_HOST", "localhost");
    }

    public int getDbPort() {
        return Integer.parseInt(getOrDefault("DB_PORT", "1433"));
    }

    public String getDbName() {
        return getOrDefault("DB_NAME", "GameOnDb");
    }

    public String getDbUser() {
        return getOrDefault("DB_USER", "");
    }

    public String getDbPassword() {
        return getOrDefault("DB_PASSWORD", "");
    }

    public String getJdbcUrl() {
        return String.format(
                "jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=true;trustServerCertificate=true",
                getDbHost(), getDbPort(), getDbName()
        );
    }

    // --- Application ---

    public int getAppPort() {
        return Integer.parseInt(getOrDefault("APP_PORT", "7070"));
    }

    public String getAppEnv() {
        return getOrDefault("APP_ENV", "development");
    }

    public boolean isProduction() {
        return "production".equalsIgnoreCase(getAppEnv());
    }

    // --- Helpers ---

    private String getOrDefault(String key, String defaultValue) {
        // System env vars take highest precedence
        String sysValue = System.getenv(key);
        if (sysValue != null && !sysValue.isBlank()) {
            return sysValue;
        }
        // Then .env file values
        String dotenvValue = dotenv.get(key);
        if (dotenvValue != null && !dotenvValue.isBlank()) {
            return dotenvValue;
        }
        return defaultValue;
    }

    /**
     * Locates and loads .env from the repository root.
     * Checks ../ first (running from backend/), then ./ (running from repo root).
     * If neither exists, returns a dotenv that yields nulls (system env only).
     */
    private static Dotenv loadDotenv() {
        // When running from backend/ (normal case), .env is one level up
        Path parentEnv = Path.of("../.env");
        if (Files.exists(parentEnv)) {
            return Dotenv.configure()
                    .directory("../")
                    .ignoreIfMissing()
                    .load();
        }

        // When running from repo root directly
        Path currentEnv = Path.of(".env");
        if (Files.exists(currentEnv)) {
            return Dotenv.configure()
                    .ignoreIfMissing()
                    .load();
        }

        // No .env found — rely on system environment variables only
        return Dotenv.configure()
                .ignoreIfMissing()
                .load();
    }
}
