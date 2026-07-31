package com.gameon.controller;

import com.gameon.security.CustomUserDetails;
import com.gameon.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Global model attribute advice.
 * Injects common data into all Thymeleaf views:
 * - unreadNotificationCount: for the notification badge in navbar
 */
@ControllerAdvice
public class GlobalModelAdvice {

    private final NotificationService notificationService;

    public GlobalModelAdvice(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @ModelAttribute("unreadNotificationCount")
    public Long unreadNotificationCount() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            return notificationService.getUnreadCount(userDetails.getUserId());
        }
        return 0L;
    }
}
