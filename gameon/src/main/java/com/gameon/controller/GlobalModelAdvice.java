package com.gameon.controller;

import com.gameon.model.entity.User;
import com.gameon.security.CustomUserDetails;
import com.gameon.service.NotificationService;
import com.gameon.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Global model attribute advice.
 * Injects common data into all Thymeleaf views:
 * - unreadNotificationCount: for the notification badge in navbar
 * - currentUserProfilePictureUrl: the logged-in user's avatar URL for the navbar (null -> default icon)
 */
@ControllerAdvice
public class GlobalModelAdvice {

    private final NotificationService notificationService;
    private final UserService userService;

    public GlobalModelAdvice(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @ModelAttribute("unreadNotificationCount")
    public Long unreadNotificationCount() {
        CustomUserDetails userDetails = currentUserDetails();
        return userDetails != null ? notificationService.getUnreadCount(userDetails.getUserId()) : 0L;
    }

    /**
     * The logged-in user's profile picture URL for the navbar, read fresh from the database so
     * a just-uploaded picture shows immediately (the security principal holds a login-time
     * snapshot). Returns {@code null} when unauthenticated or no picture is set; the navbar then
     * falls back to the default person icon.
     */
    @ModelAttribute("currentUserProfilePictureUrl")
    public String currentUserProfilePictureUrl() {
        CustomUserDetails userDetails = currentUserDetails();
        if (userDetails == null) {
            return null;
        }
        try {
            User user = userService.getUserById(userDetails.getUserId());
            return user != null ? user.getProfilePictureUrl() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private CustomUserDetails currentUserDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails;
        }
        return null;
    }
}
