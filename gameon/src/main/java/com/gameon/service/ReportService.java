package com.gameon.service;

import com.gameon.exception.ResourceNotFoundException;
import com.gameon.model.entity.Report;
import com.gameon.model.entity.User;
import com.gameon.model.enums.ReportStatus;
import com.gameon.model.enums.ReportType;
import com.gameon.repository.ReportRepository;
import com.gameon.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    public ReportService(ReportRepository reportRepository,
                         UserRepository userRepository) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
    }

    /**
     * Creates a new report (D600/D700).
     * BR12: No limit on number of reports a user can submit.
     */
    @Transactional
    public Report createReport(Long reporterId, Long referenceId, ReportType reportType, String reportReason) {
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new ResourceNotFoundException("User", reporterId));

        Report report = new Report(reporter, referenceId, reportType, reportReason);
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
    public Report dismissReport(Long reportId) {
        Report report = getReportById(reportId);
        report.setStatus(ReportStatus.DISMISSED);
        Report saved = reportRepository.save(report);
        logger.info("Report {} dismissed", reportId);
        return saved;
    }

    /**
     * Actions a report (B400 - content/user removed).
     * BR13: Only moderator should call this (enforced at controller level).
     */
    @Transactional
    public Report actionReport(Long reportId) {
        Report report = getReportById(reportId);
        report.setStatus(ReportStatus.ACTIONED);
        Report saved = reportRepository.save(report);
        logger.info("Report {} actioned (content removed)", reportId);
        return saved;
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
        return reportRepository.findByReferenceIdAndReportType(referenceId, type);
    }
}
