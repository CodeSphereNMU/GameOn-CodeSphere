package com.gameon.controller;

import com.gameon.model.dto.AdminUserView;
import com.gameon.model.dto.WarningView;
import com.gameon.model.entity.ModerationAction;
import com.gameon.model.entity.Report;
import com.gameon.model.entity.User;
import com.gameon.model.enums.AccountStatus;
import com.gameon.model.enums.ContentType;
import com.gameon.model.enums.ModerationActionType;
import com.gameon.model.enums.ReportStatus;
import com.gameon.model.enums.ReportType;
import com.gameon.service.ModerationService;
import com.gameon.service.PostService;
import com.gameon.service.ReportService;
import com.gameon.service.UserService;
import com.gameon.service.WarningService;
import com.gameon.security.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller for B400 (View Reports) and the full moderation workflow - administrator only.
 * BR13: Only an administrator can remove users/posts/comments.
 *
 * <p>Every moderator action is recorded permanently via {@link ModerationService} and, for
 * strikes, {@link WarningService}. No records are ever deleted; content removal and account
 * status changes are soft (audit trail preserved).</p>
 */
@Controller
@RequestMapping("/moderator")
@PreAuthorize("hasRole('ADMIN')")
public class ModeratorController {

    private final ReportService reportService;
    private final UserService userService;
    private final PostService postService;
    private final WarningService warningService;
    private final ModerationService moderationService;

    public ModeratorController(ReportService reportService,
                               UserService userService,
                               PostService postService,
                               WarningService warningService,
                               ModerationService moderationService) {
        this.reportService = reportService;
        this.userService = userService;
        this.postService = postService;
        this.warningService = warningService;
        this.moderationService = moderationService;
    }

    // ===== Moderation Summary Dashboard =====

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalReports", reportService.getTotalReportCount());
        model.addAttribute("pendingReports", reportService.getPendingReportCount());
        model.addAttribute("activeWarnings", warningService.getTotalWarningCount());
        model.addAttribute("suspendedUsers", userService.countByStatus(AccountStatus.SUSPENDED));
        model.addAttribute("bannedUsers", userService.countByStatus(AccountStatus.BANNED));
        model.addAttribute("totalModerationActions", moderationService.getTotalActionCount());
        model.addAttribute("recentActions", moderationService.getRecentActions(10));
        return "moderator/dashboard";
    }

    // ===== B400: View Reports =====

    @GetMapping("/reports")
    public String viewReports(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<Report> reports = reportService.getPendingReports(PageRequest.of(page, 20));
        model.addAttribute("reports", reports);
        model.addAttribute("pendingCount", reportService.getPendingReportCount());
        return "moderator/reports";
    }

    // ===== B400: View Reported Item + report history for the reported user =====

    @GetMapping("/reports/{reportId}")
    public String viewReportDetail(@PathVariable Long reportId, Model model) {
        Report report = reportService.getReportById(reportId);
        model.addAttribute("report", report);

        if (report.getReportType() == ReportType.USER) {
            try {
                User reportedUser = userService.getUserById(report.getReferenceId());
                model.addAttribute("reportedUser", reportedUser);
                model.addAttribute("reportHistory",
                        reportService.getReportsForItem(reportedUser.getUserId(), ReportType.USER));
            } catch (Exception e) {
                model.addAttribute("itemNotFound", true);
            }
        } else if (report.getReportType() == ReportType.POST) {
            try {
                var post = postService.getPostForModeration(report.getReferenceId());
                model.addAttribute("reportedPost", post);
                model.addAttribute("reportHistory",
                        reportService.getReportsForItem(post.getUser().getUserId(), ReportType.USER));
            } catch (Exception e) {
                model.addAttribute("itemNotFound", true);
            }
        }
        return "moderator/report-detail";
    }

    // ===== Moderator action 1: Dismiss Report =====

    @PostMapping("/reports/{reportId}/dismiss")
    public String dismissReport(@PathVariable Long reportId,
                                @AuthenticationPrincipal CustomUserDetails currentUser,
                                RedirectAttributes redirectAttributes) {
        Report report = reportService.getReportById(reportId);
        reportService.dismissReport(reportId, currentUser.getUserId());
        moderationService.logAction(currentUser.getUserId(), targetUserId(report), reportId,
                ModerationActionType.DISMISS_REPORT, "Report dismissed - no action taken",
                contentType(report), report.getReferenceId());
        redirectAttributes.addFlashAttribute("success", "Report dismissed.");
        return "redirect:/moderator/reports";
    }

    // ===== Moderator action 2: Issue Warning (strike only, no content removal) =====

    @PostMapping("/reports/{reportId}/warn")
    public String issueWarning(@PathVariable Long reportId,
                               @RequestParam(required = false) String reason,
                               @AuthenticationPrincipal CustomUserDetails currentUser,
                               RedirectAttributes redirectAttributes) {
        Report report = reportService.getReportById(reportId);
        Long targetUserId = targetUserId(report);
        String effectiveReason = (reason != null && !reason.isBlank()) ? reason : report.getReportReason();

        warningService.issueStrike(targetUserId, currentUser.getUserId(), effectiveReason,
                contentType(report), report.getReferenceId(), reportId);
        reportService.resolveReport(reportId, currentUser.getUserId());
        moderationService.logAction(currentUser.getUserId(), targetUserId, reportId,
                ModerationActionType.ISSUE_WARNING, effectiveReason,
                contentType(report), report.getReferenceId());

        redirectAttributes.addFlashAttribute("success", banAwareMessage(targetUserId, "Warning issued"));
        return "redirect:/moderator/reports";
    }

    // ===== Moderator action 3: Remove Content + Warning =====

    @PostMapping("/reports/{reportId}/remove")
    public String removeContentAndWarn(@PathVariable Long reportId,
                                       @RequestParam(required = false) String reason,
                                       @AuthenticationPrincipal CustomUserDetails currentUser,
                                       RedirectAttributes redirectAttributes) {
        Report report = reportService.getReportById(reportId);
        String effectiveReason = (reason != null && !reason.isBlank()) ? reason : report.getReportReason();

        if (report.getReportType() == ReportType.POST) {
            var post = postService.getPostForModeration(report.getReferenceId());
            Long authorId = post.getUser().getUserId();
            warningService.issueStrike(authorId, currentUser.getUserId(), effectiveReason,
                    ContentType.POST, post.getPostId(), reportId);
            reportService.resolvePostReport(reportId, currentUser.getUserId());
            moderationService.logAction(currentUser.getUserId(), authorId, reportId,
                    ModerationActionType.REMOVE_CONTENT_AND_WARNING, effectiveReason,
                    ContentType.POST, post.getPostId());
            redirectAttributes.addFlashAttribute("success",
                    banAwareMessage(authorId, "Post removed and warning issued"));
        } else {
            // No specific content to remove for a USER report -> equivalent to issuing a warning.
            Long targetUserId = report.getReferenceId();
            warningService.issueStrike(targetUserId, currentUser.getUserId(), effectiveReason,
                    ContentType.USER, targetUserId, reportId);
            reportService.resolveReport(reportId, currentUser.getUserId());
            moderationService.logAction(currentUser.getUserId(), targetUserId, reportId,
                    ModerationActionType.ISSUE_WARNING, effectiveReason, ContentType.USER, targetUserId);
            redirectAttributes.addFlashAttribute("success", banAwareMessage(targetUserId, "Warning issued"));
        }
        return "redirect:/moderator/reports";
    }

    // ===== Moderator action 4: Suspend User (from a report) =====

    @PostMapping("/reports/{reportId}/suspend")
    public String suspendFromReport(@PathVariable Long reportId,
                                    @RequestParam(required = false) String reason,
                                    @AuthenticationPrincipal CustomUserDetails currentUser,
                                    RedirectAttributes redirectAttributes) {
        Report report = reportService.getReportById(reportId);
        Long targetUserId = targetUserId(report);
        String effectiveReason = (reason != null && !reason.isBlank()) ? reason : report.getReportReason();

        User user = userService.suspendUser(targetUserId, effectiveReason);
        reportService.resolveReport(reportId, currentUser.getUserId());
        moderationService.logAction(currentUser.getUserId(), targetUserId, reportId,
                ModerationActionType.SUSPEND, effectiveReason, contentType(report), report.getReferenceId());
        redirectAttributes.addFlashAttribute("success", user.getUsername() + " has been SUSPENDED.");
        return "redirect:/moderator/reports";
    }

    // ===== Moderator action 5: Ban User (from a report) =====

    @PostMapping("/reports/{reportId}/ban")
    public String banFromReport(@PathVariable Long reportId,
                                @RequestParam(required = false) String reason,
                                @AuthenticationPrincipal CustomUserDetails currentUser,
                                RedirectAttributes redirectAttributes) {
        Report report = reportService.getReportById(reportId);
        Long targetUserId = targetUserId(report);
        String effectiveReason = (reason != null && !reason.isBlank()) ? reason : report.getReportReason();

        User user = userService.banUser(targetUserId, currentUser.getUserId(), effectiveReason);
        reportService.resolveReport(reportId, currentUser.getUserId());
        moderationService.logAction(currentUser.getUserId(), targetUserId, reportId,
                ModerationActionType.BAN, effectiveReason, contentType(report), report.getReferenceId());
        redirectAttributes.addFlashAttribute("success", user.getUsername() + " has been BANNED.");
        return "redirect:/moderator/reports";
    }

    // ===== Admin Dashboard: User account status & moderation standing =====

    @GetMapping("/users")
    public String usersDashboard(Model model) {
        List<AdminUserView> users = userService.getAllUsersForModeration().stream()
                .map(AdminUserView::from)
                .toList();
        model.addAttribute("users", users);
        return "moderator/users";
    }

    /** Issue a manual strike against a user (3 strikes => automatic ban). Fully audited. */
    @PostMapping("/users/{userId}/strike")
    public String strikeUser(@PathVariable Long userId,
                             @RequestParam(required = false) String reason,
                             @AuthenticationPrincipal CustomUserDetails currentUser,
                             RedirectAttributes redirectAttributes) {
        warningService.issueStrike(userId, currentUser.getUserId(), reason, ContentType.USER, userId, null);
        moderationService.logAction(currentUser.getUserId(), userId, null,
                ModerationActionType.ISSUE_WARNING, reason, ContentType.USER, userId);
        redirectAttributes.addFlashAttribute("success", banAwareMessage(userId, "Strike issued"));
        return "redirect:/moderator/users";
    }

    /** Suspend a user (temporary restriction; strikes unchanged). */
    @PostMapping("/users/{userId}/suspend")
    public String suspendUser(@PathVariable Long userId,
                              @RequestParam(required = false) String reason,
                              @AuthenticationPrincipal CustomUserDetails currentUser,
                              RedirectAttributes redirectAttributes) {
        var user = userService.suspendUser(userId, reason);
        moderationService.logAction(currentUser.getUserId(), userId, null,
                ModerationActionType.SUSPEND, reason, ContentType.USER, userId);
        redirectAttributes.addFlashAttribute("success", user.getUsername() + " has been SUSPENDED.");
        return "redirect:/moderator/users";
    }

    /** Ban a user directly with a reason (audited). */
    @PostMapping("/users/{userId}/ban")
    public String banUser(@PathVariable Long userId,
                          @RequestParam(required = false) String reason,
                          @AuthenticationPrincipal CustomUserDetails currentUser,
                          RedirectAttributes redirectAttributes) {
        var user = userService.banUser(userId, currentUser.getUserId(), reason);
        moderationService.logAction(currentUser.getUserId(), userId, null,
                ModerationActionType.BAN, reason, ContentType.USER, userId);
        redirectAttributes.addFlashAttribute("success", user.getUsername() + " has been BANNED.");
        return "redirect:/moderator/users";
    }

    // ===== Admin controls: unban / reinstate / reset warnings =====

    /** Reinstate a user: status ACTIVE, strikes reset, ban cleared. */
    @PostMapping("/users/{userId}/reinstate")
    public String reinstateUser(@PathVariable Long userId,
                                @AuthenticationPrincipal CustomUserDetails currentUser,
                                RedirectAttributes redirectAttributes) {
        var user = userService.reinstateUser(userId);
        moderationService.logAction(currentUser.getUserId(), userId, null,
                ModerationActionType.UNBAN, "Reinstated (status ACTIVE, strikes reset)",
                ContentType.USER, userId);
        redirectAttributes.addFlashAttribute("success",
                user.getUsername() + " has been reinstated (status ACTIVE, strikes reset).");
        return "redirect:/moderator/users";
    }

    /** Unban a user without resetting their strike count. */
    @PostMapping("/users/{userId}/unban")
    public String unbanUser(@PathVariable Long userId,
                            @AuthenticationPrincipal CustomUserDetails currentUser,
                            RedirectAttributes redirectAttributes) {
        userService.activateUser(userId);
        User user = userService.getUserById(userId);
        moderationService.logAction(currentUser.getUserId(), userId, null,
                ModerationActionType.UNBAN, "Unbanned (status ACTIVE)", ContentType.USER, userId);
        redirectAttributes.addFlashAttribute("success", user.getUsername() + " has been unbanned (status ACTIVE).");
        return "redirect:/moderator/users";
    }

    /** Reset a user's active warning count to zero (audit records preserved). */
    @PostMapping("/users/{userId}/reset-warnings")
    public String resetWarnings(@PathVariable Long userId,
                                @AuthenticationPrincipal CustomUserDetails currentUser,
                                RedirectAttributes redirectAttributes) {
        var user = userService.resetWarningCount(userId);
        moderationService.logAction(currentUser.getUserId(), userId, null,
                ModerationActionType.RESET_WARNINGS, "Warning count reset to 0", ContentType.USER, userId);
        redirectAttributes.addFlashAttribute("success", user.getUsername() + "'s warning count reset to 0.");
        return "redirect:/moderator/users";
    }

    /** Reverse a specific warning (audit record kept; active count decremented). */
    @PostMapping("/warnings/{warningId}/reverse")
    public String reverseWarning(@PathVariable Long warningId,
                                 @AuthenticationPrincipal CustomUserDetails currentUser,
                                 RedirectAttributes redirectAttributes) {
        var warning = warningService.getWarningById(warningId);
        Long targetUserId = warning.getUser().getUserId();
        User user = warningService.reverseWarning(warningId, currentUser.getUserId());
        moderationService.logAction(currentUser.getUserId(), targetUserId, null,
                ModerationActionType.REVERSE_WARNING, "Reversed warning #" + warningId,
                warning.getContentType(), warning.getContentId());
        redirectAttributes.addFlashAttribute("success",
                "Warning #" + warningId + " reversed. " + user.getUsername()
                        + " now has " + user.getWarningCount() + " active strike(s).");
        return "redirect:/moderator/users/" + targetUserId + "/warnings";
    }

    /** Restore a soft-removed post (audit control). */
    @PostMapping("/posts/{postId}/restore")
    public String restorePost(@PathVariable Long postId,
                              @AuthenticationPrincipal CustomUserDetails currentUser,
                              RedirectAttributes redirectAttributes) {
        var post = postService.getPostForModeration(postId);
        postService.restorePostAsModerator(postId, currentUser.getUserId());
        moderationService.logAction(currentUser.getUserId(), post.getUser().getUserId(), null,
                ModerationActionType.RESTORE_CONTENT, "Restored post #" + postId, ContentType.POST, postId);
        redirectAttributes.addFlashAttribute("success", "Post #" + postId + " restored.");
        return "redirect:/moderator/users";
    }

    // ===== Admin Dashboard: strike / warning audit history =====

    @GetMapping("/warnings")
    public String warningsDashboard(Model model) {
        List<WarningView> warnings = warningService.getAllWarnings().stream()
                .map(WarningView::from)
                .toList();
        model.addAttribute("warnings", warnings);
        model.addAttribute("totalWarnings", warnings.size());
        return "moderator/warnings";
    }

    @GetMapping("/users/{userId}/warnings")
    public String userWarnings(@PathVariable Long userId, Model model) {
        User user = userService.getUserById(userId);
        List<WarningView> warnings = warningService.getWarningsForUser(userId).stream()
                .map(WarningView::from)
                .toList();
        model.addAttribute("user", AdminUserView.from(user));
        model.addAttribute("warnings", warnings);
        model.addAttribute("totalWarnings", warnings.size());
        model.addAttribute("actions", moderationService.getActionsForUser(userId));
        return "moderator/user-warnings";
    }

    // ===== Moderation action history =====

    @GetMapping("/actions")
    public String moderationActions(Model model) {
        List<ModerationAction> actions = moderationService.getAllActions();
        model.addAttribute("actions", actions);
        model.addAttribute("totalActions", actions.size());
        return "moderator/actions";
    }

    // ===== Helpers =====

    private Long targetUserId(Report report) {
        if (report.getReportType() == ReportType.USER) {
            return report.getReferenceId();
        }
        // POST report: the target is the post's author.
        try {
            return postService.getPostForModeration(report.getReferenceId()).getUser().getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    private ContentType contentType(Report report) {
        return report.getReportType() == ReportType.POST ? ContentType.POST : ContentType.USER;
    }

    private String banAwareMessage(Long userId, String prefix) {
        User user = userService.getUserById(userId);
        if (user.isBanned()) {
            return prefix + ". " + user.getUsername() + " reached " + user.getWarningCount()
                    + " strikes and is now BANNED.";
        }
        return prefix + " to " + user.getUsername() + " (" + user.getWarningCount()
                + "/" + User.STRIKES_FOR_BAN + " strikes).";
    }
}
