package com.gameon.controller;

import com.gameon.model.entity.Notification;
import com.gameon.exception.BusinessRuleException;
import com.gameon.security.CustomUserDetails;
import com.gameon.service.NotificationService;
import com.gameon.service.InvitationService;
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
    private final InvitationService invitationService;

    public NotificationController(NotificationService notificationService,
                                  InvitationService invitationService) {
        this.notificationService = notificationService;
        this.invitationService = invitationService;
    }

    @GetMapping
    public String index(@AuthenticationPrincipal CustomUserDetails currentUser,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        Page<Notification> notifications = notificationService.getNotificationsForUser(
                currentUser.getUserId(), PageRequest.of(page, 20));

        model.addAttribute("notifications", notifications);
        model.addAttribute("clickableNotificationIds", notificationService
                .getClickableNotificationIds(notifications.getContent().stream()
                        .map(Notification::getNotificationId).toList(), currentUser.getUserId()));
        model.addAttribute("unreadCount", notificationService.getUnreadCount(currentUser.getUserId()));
        model.addAttribute("invitations", invitationService.getActiveInvitations(currentUser.getUserId()));
        return "notifications/index";
    }

    @PostMapping("/mark-read/{id}")
    public String markAsRead(@PathVariable Long id,
                             @AuthenticationPrincipal CustomUserDetails currentUser) {
        notificationService.markAsRead(id, currentUser.getUserId());
        return "redirect:/notifications";
    }

    @PostMapping("/mark-all-read")
    public String markAllRead(@AuthenticationPrincipal CustomUserDetails currentUser,
                              RedirectAttributes redirectAttributes) {
        int count = notificationService.markAllAsRead(currentUser.getUserId());
        redirectAttributes.addFlashAttribute("success", count + " notifications marked as read");
        return "redirect:/notifications";
    }

    @PostMapping("/open/{id}")
    public String open(@PathVariable Long id,
                       @AuthenticationPrincipal CustomUserDetails currentUser,
                       RedirectAttributes redirectAttributes) {
        return notificationService.openNotification(id, currentUser.getUserId())
                .map(destination -> "redirect:" + destination)
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error",
                            "This notification no longer has an available destination.");
                    return "redirect:/notifications";
                });
    }

    @PostMapping("/invitations/{id}/decline")
    public String declineInvitation(@PathVariable Long id,
                                    @AuthenticationPrincipal CustomUserDetails currentUser,
                                    RedirectAttributes redirectAttributes) {
        try {
            invitationService.declineInvitation(id, currentUser.getUserId());
            redirectAttributes.addFlashAttribute("success", "Invitation declined.");
        } catch (BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/notifications";
    }
}
