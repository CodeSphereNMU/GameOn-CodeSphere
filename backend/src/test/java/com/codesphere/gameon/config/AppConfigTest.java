package com.codesphere.gameon.config;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AppConfig.
 * Uses the package-private constructor with a controlled value provider
 * so tests are fully isolated from the real .env and system environment.
 */
class AppConfigTest {

    /**
     * A provider that returns null for all keys, simulating no .env and no env vars.
     */
    private static final Function<String, String> EMPTY_PROVIDER = key -> null;

    /**
     * Creates a provider backed by the given map.
     */
    private static Function<String, String> providerFrom(Map<String, String> values) {
        return values::get;
    }

    // ========================================================
    // Default values (provider returns null for everything)
    // ========================================================

    @Test
    void shouldReturnDefaultDbHost() {
        AppConfig config = new AppConfig(EMPTY_PROVIDER);
        assertEquals("localhost", config.getDbHost());
    }

    @Test
    void shouldReturnDefaultDbPort() {
        AppConfig config = new AppConfig(EMPTY_PROVIDER);
        assertEquals(1433, config.getDbPort());
    }

    @Test
    void shouldReturnDefaultDbName() {
        AppConfig config = new AppConfig(EMPTY_PROVIDER);
        assertEquals("GameOnDB", config.getDbName());
    }

    @Test
    void shouldReturnDefaultDbUser() {
        AppConfig config = new AppConfig(EMPTY_PROVIDER);
        assertEquals("", config.getDbUser());
    }

    @Test
    void shouldReturnDefaultDbPassword() {
        AppConfig config = new AppConfig(EMPTY_PROVIDER);
        assertEquals("", config.getDbPassword());
    }

    @Test
    void shouldReturnDefaultAppPort() {
        AppConfig config = new AppConfig(EMPTY_PROVIDER);
        assertEquals(7070, config.getAppPort());
    }

    @Test
    void shouldReturnDefaultAppEnv() {
        AppConfig config = new AppConfig(EMPTY_PROVIDER);
        assertEquals("development", config.getAppEnv());
    }

    @Test
    void shouldNotBeProductionByDefault() {
        AppConfig config = new AppConfig(EMPTY_PROVIDER);
        assertFalse(config.isProduction());
    }

    // ========================================================
    // JDBC URL from defaults
    // ========================================================

    @Test
    void shouldBuildJdbcUrlFromDefaults() {
        AppConfig config = new AppConfig(EMPTY_PROVIDER);
        String url = config.getJdbcUrl();

        assertEquals(
                "jdbc:sqlserver://localhost:1433;databaseName=GameOnDB;encrypt=true;trustServerCertificate=true",
                url
        );
    }

    // ========================================================
    // Supplied values override defaults
    // ========================================================

    @Test
    void shouldUseSuppliedDbHost() {
        AppConfig config = new AppConfig(providerFrom(Map.of("DB_HOST", "myserver")));
        assertEquals("myserver", config.getDbHost());
    }

    @Test
    void shouldUseSuppliedDbPort() {
        AppConfig config = new AppConfig(providerFrom(Map.of("DB_PORT", "5555")));
        assertEquals(5555, config.getDbPort());
    }

    @Test
    void shouldUseSuppliedDbName() {
        AppConfig config = new AppConfig(providerFrom(Map.of("DB_NAME", "TestDB")));
        assertEquals("TestDB", config.getDbName());
    }

    @Test
    void shouldUseSuppliedAppPort() {
        AppConfig config = new AppConfig(providerFrom(Map.of("APP_PORT", "9090")));
        assertEquals(9090, config.getAppPort());
    }

    @Test
    void shouldUseSuppliedAppEnv() {
        AppConfig config = new AppConfig(providerFrom(Map.of("APP_ENV", "staging")));
        assertEquals("staging", config.getAppEnv());
    }

    @Test
    void shouldBuildJdbcUrlFromSuppliedValues() {
        AppConfig config = new AppConfig(providerFrom(Map.of(
                "DB_HOST", "prodserver",
                "DB_PORT", "2433",
                "DB_NAME", "ProdDB"
        )));
        String url = config.getJdbcUrl();

        assertEquals(
                "jdbc:sqlserver://prodserver:2433;databaseName=ProdDB;encrypt=true;trustServerCertificate=true",
                url
        );
    }

    // ========================================================
    // isProduction() behaviour
    // ========================================================

    @Test
    void shouldBeProductionWhenAppEnvIsProduction() {
        AppConfig config = new AppConfig(providerFrom(Map.of("APP_ENV", "production")));
        assertTrue(config.isProduction());
    }

    @Test
    void shouldBeProductionCaseInsensitively() {
        AppConfig config = new AppConfig(providerFrom(Map.of("APP_ENV", "PRODUCTION")));
        assertTrue(config.isProduction());
    }

    @Test
    void shouldNotBeProductionWhenAppEnvIsDevelopment() {
        AppConfig config = new AppConfig(providerFrom(Map.of("APP_ENV", "development")));
        assertFalse(config.isProduction());
    }

    @Test
    void shouldNotBeProductionForArbitraryValue() {
        AppConfig config = new AppConfig(providerFrom(Map.of("APP_ENV", "staging")));
        assertFalse(config.isProduction());
    }

    // ========================================================
    // Blank values fall back to defaults
    // ========================================================

    @Test
    void shouldFallBackToDefaultWhenValueIsBlank() {
        Map<String, String> values = new HashMap<>();
        values.put("DB_HOST", "   ");
        values.put("DB_PORT", "");
        values.put("APP_ENV", "  ");
        AppConfig config = new AppConfig(providerFrom(values));

        assertEquals("localhost", config.getDbHost());
        assertEquals(1433, config.getDbPort());
        assertEquals("development", config.getAppEnv());
    }

    // ========================================================
    // Integer parsing for port values
    // ========================================================

    @Test
    void shouldParseDbPortAsInteger() {
        AppConfig config = new AppConfig(providerFrom(Map.of("DB_PORT", "1434")));
        assertEquals(1434, config.getDbPort());
    }

    @Test
    void shouldParseAppPortAsInteger() {
        AppConfig config = new AppConfig(providerFrom(Map.of("APP_PORT", "8080")));
        assertEquals(8080, config.getAppPort());
    }

    @Test
    void shouldThrowForNonNumericDbPort() {
        AppConfig config = new AppConfig(providerFrom(Map.of("DB_PORT", "notanumber")));
        assertThrows(NumberFormatException.class, config::getDbPort);
    }

    @Test
    void shouldThrowForNonNumericAppPort() {
        AppConfig config = new AppConfig(providerFrom(Map.of("APP_PORT", "abc")));
        assertThrows(NumberFormatException.class, config::getAppPort);
    }
}
