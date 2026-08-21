package com.gameon.service;

import com.gameon.exception.ResourceNotFoundException;
import com.gameon.model.entity.Notification;
import com.gameon.model.entity.User;
import com.gameon.model.entity.GameListing;
import com.gameon.model.entity.JoinRequest;
import com.gameon.model.entity.MatchResult;
import com.gameon.model.enums.NotificationType;
import com.gameon.repository.NotificationRepository;
import com.gameon.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service handling notification creation and management.
 * Used by other services to send notifications on key events:
 * - Follow, Join request received/accepted/rejected
 * - Listing confirmation/cancellation, match results, and invitations
 */
@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    /**
     * Creates and saves a notification for a recipient.
     */
    @Transactional
    public Notification createNotification(Long recipientId, String text, NotificationType type) {
        return createNotification(recipientId, text, type, null, null, null, null);
    }

    @Transactional
    public Notification createNotification(Long recipientId, String text, NotificationType type,
                                           User actor, GameListing listing,
                                           JoinRequest joinRequest, MatchResult matchResult) {
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("User", recipientId));

        Notification notification = new Notification(recipient, text, type);
        notification.setActor(actor);
        notification.setGameListing(listing);
        notification.setJoinRequest(joinRequest);
        notification.setMatchResult(matchResult);
        Notification saved = notificationRepository.save(notification);
        logger.debug("Notification sent to user {}: [{}] {}", recipientId, type, text);
        return saved;
    }

    /**
     * Creates notifications for multiple recipients (bulk).
     */
    @Transactional
    public void createBulkNotifications(List<Long> recipientIds, String text, NotificationType type) {
        createBulkNotifications(recipientIds, text, type, null, null, null, null);
    }

    @Transactional
    public void createBulkNotifications(List<Long> recipientIds, String text, NotificationType type,
                                        User actor, GameListing listing,
                                        JoinRequest joinRequest, MatchResult matchResult) {
        for (Long recipientId : recipientIds) {
            try {
                createNotification(recipientId, text, type, actor, listing, joinRequest, matchResult);
            } catch (ResourceNotFoundException e) {
                logger.warn("Skipping notification for non-existent user: {}", recipientId);
            }
        }
    }

    @Transactional(readOnly = true)
    public Page<Notification> getNotificationsForUser(Long userId, Pageable pageable) {
        return notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public List<Notification> getAllNotificationsForUser(Long userId) {
        return notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByRecipientUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.markAsRead(notificationId);
    }

    @Transactional
    public int markAllAsRead(Long userId) {
        int count = notificationRepository.markAllAsReadForUser(userId);
        logger.debug("Marked {} notifications as read for user {}", count, userId);
        return count;
    }
}
