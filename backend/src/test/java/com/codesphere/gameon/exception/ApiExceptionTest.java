package com.codesphere.gameon.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ApiException factory methods.
 */
class ApiExceptionTest {

    @Test
    void badRequestShouldReturn400() {
        ApiException ex = ApiException.badRequest("Invalid input");
        assertEquals(400, ex.getStatus());
        assertEquals("Invalid input", ex.getMessage());
    }

    @Test
    void notFoundShouldReturn404() {
        ApiException ex = ApiException.notFound("User not found");
        assertEquals(404, ex.getStatus());
        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void unauthorizedShouldReturn401() {
        ApiException ex = ApiException.unauthorized("Login required");
        assertEquals(401, ex.getStatus());
    }

    @Test
    void forbiddenShouldReturn403() {
        ApiException ex = ApiException.forbidden("Access denied");
        assertEquals(403, ex.getStatus());
    }

    @Test
    void conflictShouldReturn409() {
        ApiException ex = ApiException.conflict("Username taken");
        assertEquals(409, ex.getStatus());
    }
}
