package com.codesphere.gameon.exception;

/**
 * Base exception for controlled API errors.
 * Thrown from services or controllers when a request cannot be fulfilled
 * for a known, expected reason (e.g. validation failure, resource not found).
 *
 * The central error handler in JavalinConfig converts these into clean JSON responses.
 */
public class ApiException extends RuntimeException {

    private final int status;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    // --- Convenient factory methods ---

    public static ApiException badRequest(String message) {
        return new ApiException(400, message);
    }

    public static ApiException unauthorized(String message) {
        return new ApiException(401, message);
    }

    public static ApiException forbidden(String message) {
        return new ApiException(403, message);
    }

    public static ApiException notFound(String message) {
        return new ApiException(404, message);
    }

    public static ApiException conflict(String message) {
        return new ApiException(409, message);
    }
}
