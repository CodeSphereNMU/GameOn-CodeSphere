package com.gameon.exception;

/**
 * Exception thrown when a user tries to access or modify a resource they don't own.
 * Examples: editing another user's post, submitting score for another's listing.
 */
public class UnauthorizedAccessException extends RuntimeException {

    public UnauthorizedAccessException(String message) {
        super(message);
    }

    public UnauthorizedAccessException(String action, String resource) {
        super(String.format("You are not authorized to %s this %s", action, resource));
    }
}
