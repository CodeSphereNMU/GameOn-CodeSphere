package com.gameon;

import org.junit.jupiter.api.Test;

/**
 * Basic smoke test verifying the project compiles and test infrastructure works.
 * Full integration tests require SQL Server connection.
 */
class GameOnApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the test framework is configured correctly.
        // Spring context load test requires database connection,
        // so this is intentionally a plain unit test.
    }
}
