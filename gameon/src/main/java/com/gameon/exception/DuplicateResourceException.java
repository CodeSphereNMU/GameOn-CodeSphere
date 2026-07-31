package com.gameon.exception;

/**
 * Exception thrown when attempting to create a resource that already exists.
 * Examples: duplicate username, sport already on profile.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
