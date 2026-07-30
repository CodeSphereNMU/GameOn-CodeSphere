package com.codesphere.gameon.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the standard ApiResponse wrapper.
 */
class ApiResponseTest {

    @Test
    void successResponseShouldHaveData() {
        ApiResponse<String> response = ApiResponse.success("hello");

        assertTrue(response.isSuccess());
        assertEquals("hello", response.getData());
        assertNull(response.getError());
    }

    @Test
    void successResponseWithoutDataShouldWork() {
        ApiResponse<Void> response = ApiResponse.success();

        assertTrue(response.isSuccess());
        assertNull(response.getData());
        assertNull(response.getError());
    }

    @Test
    void errorResponseShouldHaveMessage() {
        ApiResponse<Void> response = ApiResponse.error("Something went wrong");

        assertFalse(response.isSuccess());
        assertNull(response.getData());
        assertEquals("Something went wrong", response.getError());
    }
}
