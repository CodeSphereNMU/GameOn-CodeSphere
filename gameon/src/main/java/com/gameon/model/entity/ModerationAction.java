package com.gameon.model.entity;

import com.gameon.model.enums.ContentType;
import com.gameon.model.enums.ModerationActionType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * ModerationAction entity - a permanent, immutable audit record of a single moderator action
 * (dismiss report, issue warning, remove content, suspend, ban, reverse warning, restore
 * content, unban, reset warnings). Maps to the {@code moderation_actions} table.
 *
 * <p>These records are never deleted. Together with {@link UserWarning} they form the complete
 * moderation audit trail required for admin dashboards and lecturer demonstrations.</p>
 */
@Entity
@Table(name = "moderation_actions")
public class ModerationAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "moderation_action_id")
    private Long moderationActionId;

    /** The moderator/administrator who performed the action. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "moderator_id", nullable = false)
    private User moderator;

    /** The user the action targeted, if any (nullable for content-only actions). */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "target_user_id")
    private User targetUser;

    /** The report that prompted the action, if any. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "report_id")
    private Report report;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 40)
    private ModerationActionType actionType;

    @Column(name = "reason", length = 300)
    private String reason;

    /** The type of content involved, if any (POST, COMMENT, USER, OTHER). */
    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", length = 20)
    private ContentType contentType;

    /** The id of the involved content, if any. */
    @Column(name = "content_id")
    private Long contentId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // ===== Constructors =====

    public ModerationAction() {
    }

    public ModerationAction(User moderator, User targetUser, Report report,
                            ModerationActionType actionType, String reason,
                            ContentType contentType, Long contentId) {
        this.moderator = moderator;
        this.targetUser = targetUser;
        this.report = report;
        this.actionType = actionType;
        this.reason = reason;
        this.contentType = contentType;
        this.contentId = contentId;
        this.createdAt = LocalDateTime.now();
    }

    // ===== Getters and Setters =====

    public Long getModerationActionId() {
        return moderationActionId;
    }

    public void setModerationActionId(Long moderationActionId) {
        this.moderationActionId = moderationActionId;
    }

    public User getModerator() {
        return moderator;
    }

    public void setModerator(User moderator) {
        this.moderator = moderator;
    }

    public User getTargetUser() {
        return targetUser;
    }

    public void setTargetUser(User targetUser) {
        this.targetUser = targetUser;
    }

    public Report getReport() {
        return report;
    }

    public void setReport(Report report) {
        this.report = report;
    }

    public ModerationActionType getActionType() {
        return actionType;
    }

    public void setActionType(ModerationActionType actionType) {
        this.actionType = actionType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
