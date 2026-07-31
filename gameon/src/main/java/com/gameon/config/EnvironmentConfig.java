package com.gameon.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Environment configuration validator.
 * Logs active profiles and validates critical configuration at startup.
 * Fails fast if production is missing required environment variables.
 */
@Configuration
public class EnvironmentConfig {

    private static final Logger logger = LoggerFactory.getLogger(EnvironmentConfig.class);

    private final Environment environment;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:}")
    private String datasourceUsername;

    @Value("${server.port:8080}")
    private int serverPort;

    public EnvironmentConfig(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void validateEnvironment() {
        String[] activeProfiles = environment.getActiveProfiles();
        String profileDisplay = activeProfiles.length > 0
                ? String.join(", ", activeProfiles)
                : "default";

        logger.info("============================================================");
        logger.info("  GameOn Application Starting");
        logger.info("  Active Profile(s): {}", profileDisplay);
        logger.info("  Server Port: {}", serverPort);
        logger.info("  Database URL: {}", maskConnectionString(datasourceUrl));
        logger.info("  Database User: {}", datasourceUsername);
        logger.info("============================================================");

        // In production, validate required environment variables are set
        if (isProfileActive("prod")) {
            validateRequired("DB_URL", datasourceUrl);
            validateRequired("DB_USERNAME", datasourceUsername);
            validateRequired("DB_PASSWORD", environment.getProperty("spring.datasource.password"));
            logger.info("Production environment validation passed");
        }
    }

    private boolean isProfileActive(String profile) {
        for (String active : environment.getActiveProfiles()) {
            if (active.equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }

    private void validateRequired(String variableName, String value) {
        if (value == null || value.isBlank()) {
            String message = String.format(
                    "Required environment variable '%s' is not set. "
                    + "Application cannot start in production without this value.", variableName);
            logger.error(message);
            throw new IllegalStateException(message);
        }
    }

    /**
     * Masks the connection string for safe logging (hides server details after ://).
     */
    private String maskConnectionString(String url) {
        if (url == null || url.isBlank()) {
            return "[NOT SET]";
        }
        int dbNameIndex = url.indexOf("databaseName=");
        if (dbNameIndex > 0) {
            int endIndex = url.indexOf(';', dbNameIndex);
            String dbName = endIndex > 0
                    ? url.substring(dbNameIndex, endIndex)
                    : url.substring(dbNameIndex);
            return "jdbc:sqlserver://***;" + dbName + ";...";
        }
        return url.substring(0, Math.min(url.length(), 30)) + "...";
    }
}
