package com.gameon.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility class for accessing security context information.
 * Provides static helper methods for getting the current authenticated user.
 */
public final class SecurityUtils {

    private SecurityUtils() {
        // Prevent instantiation
    }

    /**
     * Gets the current authenticated user's ID.
     */
    public static Long getCurrentUserId() {
        CustomUserDetails userDetails = getCurrentUserDetails();
        return userDetails != null ? userDetails.getUserId() : null;
    }

    /**
     * Gets the current authenticated user's username.
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        return null;
    }

    /**
     * Gets the CustomUserDetails for the current authenticated user.
     */
    public static CustomUserDetails getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            return (CustomUserDetails) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * Checks if the given userId matches the current authenticated user.
     */
    public static boolean isCurrentUser(Long userId) {
        Long currentUserId = getCurrentUserId();
        return currentUserId != null && currentUserId.equals(userId);
    }

    /**
     * Checks if the current user has the MODERATOR role.
     */
    public static boolean isModerator() {
        CustomUserDetails userDetails = getCurrentUserDetails();
        return userDetails != null && userDetails.isModerator();
    }

    /**
     * Checks if the current user has the ADMIN role.
     */
    public static boolean isAdmin() {
        CustomUserDetails userDetails = getCurrentUserDetails();
        return userDetails != null && userDetails.isAdmin();
    }

    /**
     * Checks if a user is currently authenticated (not anonymous).
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());
    }
}
