package com.codesphere.gameon.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke tests for AppConfig.
 * Verifies that configuration loads without errors and returns sensible defaults.
 */
class AppConfigTest {

    @Test
    void shouldLoadWithDefaults() {
        AppConfig config = new AppConfig();

        assertEquals("localhost", config.getDbHost());
        assertEquals(1433, config.getDbPort());
        assertEquals("GameOnDb", config.getDbName());
        assertEquals(7070, config.getAppPort());
        assertEquals("development", config.getAppEnv());
        assertFalse(config.isProduction());
    }

    @Test
    void shouldBuildJdbcUrl() {
        AppConfig config = new AppConfig();
        String url = config.getJdbcUrl();

        assertTrue(url.startsWith("jdbc:sqlserver://"));
        assertTrue(url.contains("localhost"));
        assertTrue(url.contains("1433"));
        assertTrue(url.contains("GameOnDb"));
        assertTrue(url.contains("trustServerCertificate=true"));
    }
}
