package com.gameon.model.entity;

import com.gameon.model.enums.ReportStatus;
import com.gameon.model.enums.ReportType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Report entity - User complaints about other users or posts.
 * Maps to 'reports' table in GameOnDb.
 */
@Entity
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    @NotNull(message = "Reference ID is required")
    @Column(name = "reference_id", nullable = false)
    private Long referenceId;

    @NotNull(message = "Report type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 10)
    private ReportType reportType;

    @NotBlank(message = "Report reason is required")
    @Size(max = 50, message = "Report reason must be at most 50 characters")
    @Column(name = "report_reason", nullable = false, length = 50)
    private String reportReason;

    @Size(max = 200, message = "Content must be at most 200 characters")
    @Column(name = "content", length = 200)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReportStatus status = ReportStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // ===== Relationships =====

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    // ===== Constructors =====

    public Report() {
    }

    public Report(User reporter, Long referenceId, ReportType reportType, String reportReason) {
        this.reporter = reporter;
        this.referenceId = referenceId;
        this.reportType = reportType;
        this.reportReason = reportReason;
    }

    // ===== Getters and Setters =====

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Long referenceId) {
        this.referenceId = referenceId;
    }

    public ReportType getReportType() {
        return reportType;
    }

    public void setReportType(ReportType reportType) {
        this.reportType = reportType;
    }

    public String getReportReason() {
        return reportReason;
    }

    public void setReportReason(String reportReason) {
        this.reportReason = reportReason;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public User getReporter() {
        return reporter;
    }

    public void setReporter(User reporter) {
        this.reporter = reporter;
    }
}
