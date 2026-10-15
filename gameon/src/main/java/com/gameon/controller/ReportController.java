package com.gameon.controller;

import com.gameon.model.enums.ReportType;
import com.gameon.security.CustomUserDetails;
import com.gameon.service.ReportService;
import com.gameon.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for D600 (Report User) and D700 (Report Post).
 * Creates reports that go to the moderator queue.
 */
@Controller
@RequestMapping("/report")
public class ReportController {

    private static final String[] USER_OFFENCES = {
            "Harassment or bullying",
            "Inappropriate language",
            "Spam or advertising",
            "Fake account",
            "No-show to games",
            "Unsportsmanlike conduct",
            "Other"
    };

    private static final String[] POST_OFFENCES = {
            "Inappropriate content",
            "Spam or advertising",
            "Harassment",
            "Misinformation",
            "Copyright violation",
            "Other"
    };

    private final ReportService reportService;
    private final UserService userService;

    public ReportController(ReportService reportService, UserService userService) {
        this.reportService = reportService;
        this.userService = userService;
    }

    // ===== D600: Report User =====

    @GetMapping("/user/{userId}")
    public String showReportUser(@PathVariable Long userId, Model model) {
        model.addAttribute("targetUser", userService.getUserById(userId));
        model.addAttribute("offences", USER_OFFENCES);
        model.addAttribute("reportType", "USER");
        return "report/form";
    }

    /**
     * Report &amp; Block (D600): submits a user report AND blocks the reported user in one
     * transaction (see {@link ReportService#reportAndBlockUser}). On success the reporter is
     * sent to the social hub - never back to the now-blocked user's profile, which is no longer
     * accessible to either side. On failure both the report and block are rolled back together.
     */
    @PostMapping("/user/{userId}")
    public String submitUserReport(@PathVariable Long userId,
                                   @RequestParam String reportReason,
                                   @RequestParam(required = false) String content,
                                   @AuthenticationPrincipal CustomUserDetails currentUser,
                                   RedirectAttributes redirectAttributes) {
        try {
            reportService.reportAndBlockUser(currentUser.getUserId(), userId, reportReason, content);
            redirectAttributes.addFlashAttribute("success",
                    "Report submitted and user blocked. You will no longer see each other's content.");
            return "redirect:/social";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            // On failure, return to the report form so the reporter can retry or correct input.
            return "redirect:/report/user/" + userId;
        }
    }

    // ===== D700: Report Post =====

    @GetMapping("/post/{postId}")
    public String showReportPost(@PathVariable Long postId, Model model) {
        model.addAttribute("referenceId", postId);
        model.addAttribute("offences", POST_OFFENCES);
        model.addAttribute("reportType", "POST");
        return "report/form";
    }

    @PostMapping("/post/{postId}")
    public String submitPostReport(@PathVariable Long postId,
                                   @RequestParam String reportReason,
                                   @RequestParam(required = false) String content,
                                   @AuthenticationPrincipal CustomUserDetails currentUser,
                                   RedirectAttributes redirectAttributes) {
        try {
            reportService.createReport(currentUser.getUserId(), postId, ReportType.POST, reportReason, content);
            redirectAttributes.addFlashAttribute("success", "Report submitted. A moderator will review it.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/social";
    }
}
