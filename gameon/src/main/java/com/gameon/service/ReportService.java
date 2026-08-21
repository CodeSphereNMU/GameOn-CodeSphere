package com.gameon.service;

import com.gameon.exception.ResourceNotFoundException;
import com.gameon.exception.BusinessRuleException;
import com.gameon.model.entity.Post;
import com.gameon.model.entity.Report;
import com.gameon.model.entity.User;
import com.gameon.model.enums.ReportStatus;
import com.gameon.model.enums.ReportType;
import com.gameon.repository.ReportRepository;
import com.gameon.repository.PostRepository;
import com.gameon.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDateTime;

/**
 * Service handling report creation and moderator actions.
 * Covers D600 (Report User), D700 (Report Post), B400 (View Reports - Moderator).
 * BR12: A user can report many users, posts, comments.
 * BR13: Only moderator can remove users/posts/comments.
 */
@Service
public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PostService postService;

    public ReportService(ReportRepository reportRepository,
                         UserRepository userRepository,
                         PostRepository postRepository,
                         PostService postService) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.postService = postService;
    }

    /**
     * Creates a new report (D600/D700).
     * BR12: No limit on number of reports a user can submit.
     */
    @Transactional
    public Report createReport(Long reporterId, Long referenceId, ReportType reportType, String reportReason) {
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new ResourceNotFoundException("User", reporterId));

        if (reportType == null) {
            throw new BusinessRuleException("Report type is required.");
        }
        Report report = new Report(reporter, reportReason);
        if (reportType == ReportType.USER) {
            if (reporterId.equals(referenceId)) {
                throw new BusinessRuleException("You cannot report your own account.");
            }
            User reportedUser = userRepository.findById(referenceId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", referenceId));
            report.setReportedUser(reportedUser);
        } else {
            Post reportedPost = postRepository.findByPostIdAndRemovedAtIsNull(referenceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Post", referenceId));
            report.setReportedPost(reportedPost);
        }
        Report saved = reportRepository.save(report);

        logger.info("Report created: ID={} | Reporter={} | Type={} | Reference={} | Reason={}",
                saved.getReportId(), reporter.getUsername(), reportType, referenceId, reportReason);
        return saved;
    }

    /**
     * Creates a report with additional content/description.
     */
    @Transactional
    public Report createReport(Long reporterId, Long referenceId, ReportType reportType,
                               String reportReason, String content) {
        Report report = createReport(reporterId, referenceId, reportType, reportReason);
        report.setContent(content);
        return reportRepository.save(report);
    }

    /**
     * Gets all pending reports (B400 - Moderator view).
     */
    @Transactional(readOnly = true)
    public Page<Report> getPendingReports(Pageable pageable) {
        return reportRepository.findByStatusOrderByCreatedAtDesc(ReportStatus.PENDING, pageable);
    }

    /**
     * Gets pending reports with reporter info pre-loaded.
     */
    @Transactional(readOnly = true)
    public List<Report> getPendingReportsWithReporter() {
        return reportRepository.findPendingWithReporter(ReportStatus.PENDING);
    }

    /**
     * Gets a report by ID.
     */
    @Transactional(readOnly = true)
    public Report getReportById(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", reportId));
    }

    /**
     * Dismisses a report (B400 - no action taken).
     * BR13: Only moderator should call this (enforced at controller level).
     */
    @Transactional
    public Report dismissReport(Long reportId, Long reviewerId) {
        Report report = getReportById(reportId);
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", reviewerId));
        report.setStatus(ReportStatus.DISMISSED);
        report.setReviewedBy(reviewer);
        report.setReviewedAt(LocalDateTime.now());
        Report saved = reportRepository.save(report);
        logger.info("Report {} dismissed", reportId);
        return saved;
    }

    /**
     * Actions a report (B400 - content/user removed).
     * BR13: Only moderator should call this (enforced at controller level).
     */
    @Transactional
    public Report resolveReport(Long reportId, Long reviewerId) {
        Report report = getReportById(reportId);
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", reviewerId));
        report.setStatus(ReportStatus.RESOLVED);
        report.setReviewedBy(reviewer);
        report.setReviewedAt(LocalDateTime.now());
        Report saved = reportRepository.save(report);
        logger.info("Report {} resolved by user {}", reportId, reviewerId);
        return saved;
    }

    /** Soft-removes the reported post and resolves the report in one transaction. */
    @Transactional
    public Report resolvePostReport(Long reportId, Long reviewerId) {
        Report report = getReportById(reportId);
        if (report.getReportedPost() == null) {
            throw new BusinessRuleException("This report does not reference a post.");
        }
        postService.deletePostAsModerator(report.getReportedPost().getPostId(), reviewerId);
        return resolveReport(reportId, reviewerId);
    }

    /**
     * Gets the count of pending reports (for moderator badge).
     */
    @Transactional(readOnly = true)
    public long getPendingReportCount() {
        return reportRepository.countByStatus(ReportStatus.PENDING);
    }

    /**
     * Gets all reports submitted by a user.
     */
    @Transactional(readOnly = true)
    public List<Report> getReportsByUser(Long userId) {
        return reportRepository.findByReporterUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Gets reports for a specific referenced item (user or post).
     */
    @Transactional(readOnly = true)
    public List<Report> getReportsForItem(Long referenceId, ReportType type) {
        return type == ReportType.USER
                ? reportRepository.findByReportedUserUserId(referenceId)
                : reportRepository.findByReportedPostPostId(referenceId);
    }
}
