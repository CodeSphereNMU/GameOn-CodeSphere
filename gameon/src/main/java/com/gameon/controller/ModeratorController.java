package com.gameon.controller;

import com.gameon.model.entity.Report;
import com.gameon.model.enums.ReportType;
import com.gameon.service.PostService;
import com.gameon.service.ReportService;
import com.gameon.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for B400 (View Reports) - administrator only.
 * BR13: Only an administrator can remove users/posts/comments.
 */
@Controller
@RequestMapping("/moderator")
@PreAuthorize("hasRole('ADMIN')")
public class ModeratorController {

    private final ReportService reportService;
    private final UserService userService;
    private final PostService postService;

    public ModeratorController(ReportService reportService,
                               UserService userService,
                               PostService postService) {
        this.reportService = reportService;
        this.userService = userService;
        this.postService = postService;
    }

    // ===== B400: View Reports =====

    @GetMapping("/reports")
    public String viewReports(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<Report> reports = reportService.getPendingReports(PageRequest.of(page, 20));
        model.addAttribute("reports", reports);
        model.addAttribute("pendingCount", reportService.getPendingReportCount());
        return "moderator/reports";
    }

    // ===== B400: View Reported Item =====

    @GetMapping("/reports/{reportId}")
    public String viewReportDetail(@PathVariable Long reportId, Model model) {
        Report report = reportService.getReportById(reportId);
        model.addAttribute("report", report);

        // Load the referenced item
        if (report.getReportType() == ReportType.USER) {
            try {
                model.addAttribute("reportedUser", userService.getUserById(report.getReferenceId()));
            } catch (Exception e) {
                model.addAttribute("itemNotFound", true);
            }
        } else if (report.getReportType() == ReportType.POST) {
            try {
                model.addAttribute("reportedPost", postService.getPostWithUser(report.getReferenceId()));
            } catch (Exception e) {
                model.addAttribute("itemNotFound", true);
            }
        }
        return "moderator/report-detail";
    }

    // ===== B400: Dismiss Report =====

    @PostMapping("/reports/{reportId}/dismiss")
    public String dismissReport(@PathVariable Long reportId, RedirectAttributes redirectAttributes) {
        reportService.dismissReport(reportId);
        redirectAttributes.addFlashAttribute("success", "Report dismissed.");
        return "redirect:/moderator/reports";
    }

    // ===== B400: Action Report (Remove content) =====

    @PostMapping("/reports/{reportId}/action")
    public String actionReport(@PathVariable Long reportId, RedirectAttributes redirectAttributes) {
        Report report = reportService.getReportById(reportId);

        // Remove the referenced item
        if (report.getReportType() == ReportType.USER) {
            userService.deactivateUser(report.getReferenceId());
        } else if (report.getReportType() == ReportType.POST) {
            postService.deletePostAsModerator(report.getReferenceId());
        }

        reportService.actionReport(reportId);
        redirectAttributes.addFlashAttribute("success", "Report actioned. Content has been removed.");
        return "redirect:/moderator/reports";
    }
}
