package com.gameon.model.entity;

import com.gameon.model.enums.NotificationType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Notification entity - System-generated messages delivered to users.
 * Maps to 'notifications' table in GameOnDb.
 */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @NotBlank(message = "Notification text is required")
    @Column(name = "text", nullable = false, length = 300)
    private String text;

    @NotNull(message = "Notification type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "read_at")
    private LocalDateTime readAt;

    // ===== Relationships =====

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "actor_user_id")
    private User actor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_listing_id")
    private GameListing gameListing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "join_request_id")
    private JoinRequest joinRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_result_id")
    private MatchResult matchResult;

    // ===== Constructors =====

    public Notification() {
    }

    public Notification(User recipient, String text, NotificationType notificationType) {
        this.recipient = recipient;
        this.text = text;
        this.notificationType = notificationType;
    }

    // ===== Getters and Setters =====

    public Long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Long notificationId) {
        this.notificationId = notificationId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
        this.readAt = Boolean.TRUE.equals(isRead) ? LocalDateTime.now() : null;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }

    public User getRecipient() {
        return recipient;
    }

    public void setRecipient(User recipient) {
        this.recipient = recipient;
    }

    public User getActor() { return actor; }
    public void setActor(User actor) { this.actor = actor; }
    public GameListing getGameListing() { return gameListing; }
    public void setGameListing(GameListing gameListing) { this.gameListing = gameListing; }
    public JoinRequest getJoinRequest() { return joinRequest; }
    public void setJoinRequest(JoinRequest joinRequest) { this.joinRequest = joinRequest; }
    public MatchResult getMatchResult() { return matchResult; }
    public void setMatchResult(MatchResult matchResult) { this.matchResult = matchResult; }
}
