package com.gameon.model.entity;

import com.gameon.model.enums.ContentType;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * UserWarning entity - a permanent, immutable audit record of a single moderation strike
 * issued against a user. Maps to the {@code user_warnings} table.
 *
 * <p>Every strike/warning a moderator issues MUST create one of these records. A strike must
 * never exist only as a notification, a UI message, or the {@code users.warning_count} field.
 * These records are never deleted, even when a user is banned - the moderation history must
 * remain available for audit and demonstration.</p>
 */
@Entity
@Table(name = "user_warnings")
public class UserWarning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "warning_id")
    private Long warningId;

    /** The user who received the strike. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** The report that led to this strike, if any (nullable for manual strikes). */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "report_id")
    private Report report;

    /** The moderator/administrator who issued the strike. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "moderator_id", nullable = false)
    private User moderator;

    @Column(name = "warning_reason", nullable = false, length = 300)
    private String warningReason;

    /** The type of content the strike relates to (POST, COMMENT, USER, OTHER). */
    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 20)
    private ContentType contentType = ContentType.OTHER;

    /** The id of the offending content (post id, comment id, etc.), if applicable. */
    @Column(name = "content_id")
    private Long contentId;

    /** Which strike this was for the user at the time it was issued (1, 2, 3, ...). */
    @Column(name = "strike_number", nullable = false)
    private int strikeNumber;

    /** The date the warning was issued (day granularity, as requested). */
    @Column(name = "warning_date", nullable = false)
    private LocalDate warningDate = LocalDate.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // ===== Constructors =====

    public UserWarning() {
    }

    public UserWarning(User user, User moderator, String warningReason,
                       ContentType contentType, Long contentId, int strikeNumber, Report report) {
        this.user = user;
        this.moderator = moderator;
        this.warningReason = warningReason;
        this.contentType = contentType != null ? contentType : ContentType.OTHER;
        this.contentId = contentId;
        this.strikeNumber = strikeNumber;
        this.report = report;
        this.warningDate = LocalDate.now();
        this.createdAt = LocalDateTime.now();
    }

    // ===== Getters and Setters =====

    public Long getWarningId() {
        return warningId;
    }

    public void setWarningId(Long warningId) {
        this.warningId = warningId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Report getReport() {
        return report;
    }

    public void setReport(Report report) {
        this.report = report;
    }

    public User getModerator() {
        return moderator;
    }

    public void setModerator(User moderator) {
        this.moderator = moderator;
    }

    public String getWarningReason() {
        return warningReason;
    }

    public void setWarningReason(String warningReason) {
        this.warningReason = warningReason;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public void setContentType(ContentType contentType) {
        this.contentType = contentType;
    }

    public Long getContentId() {
        return contentId;
    }

    public void setContentId(Long contentId) {
        this.contentId = contentId;
    }

    public int getStrikeNumber() {
        return strikeNumber;
    }

    public void setStrikeNumber(int strikeNumber) {
        this.strikeNumber = strikeNumber;
    }

    public LocalDate getWarningDate() {
        return warningDate;
    }

    public void setWarningDate(LocalDate warningDate) {
        this.warningDate = warningDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
