package com.codesphere.gameon.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standard wrapper for all API responses.
 * Ensures consistent JSON shape across every endpoint.
 *
 * Success:  { "success": true, "data": {...} }
 * Error:    { "success": false, "error": "message" }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final String error;

    private ApiResponse(boolean success, T data, String error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(true, null, null);
    }

    public static ApiResponse<Void> error(String message) {
        return new ApiResponse<>(false, null, message);
    }

    // --- Getters (Jackson needs these) ---

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public String getError() {
        return error;
    }
}
