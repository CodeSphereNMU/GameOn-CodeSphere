package com.gameon.controller;

import com.gameon.model.entity.Notification;
import com.gameon.security.CustomUserDetails;
import com.gameon.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for D500 - View Notifications.
 * Shows paginated notifications with read/unread status.
 */
@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public String index(@AuthenticationPrincipal CustomUserDetails currentUser,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        Page<Notification> notifications = notificationService.getNotificationsForUser(
                currentUser.getUserId(), PageRequest.of(page, 20));

        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(currentUser.getUserId()));
        return "notifications/index";
    }

    @PostMapping("/mark-read/{id}")
    public String markAsRead(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        notificationService.markAsRead(id);
        return "redirect:/notifications";
    }

    @PostMapping("/mark-all-read")
    public String markAllRead(@AuthenticationPrincipal CustomUserDetails currentUser,
                              RedirectAttributes redirectAttributes) {
        int count = notificationService.markAllAsRead(currentUser.getUserId());
        redirectAttributes.addFlashAttribute("success", count + " notifications marked as read");
        return "redirect:/notifications";
    }
}
