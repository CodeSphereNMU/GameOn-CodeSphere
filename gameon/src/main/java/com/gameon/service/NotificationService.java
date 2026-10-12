package com.gameon.service;

import com.gameon.exception.ResourceNotFoundException;
import com.gameon.model.entity.Notification;
import com.gameon.model.entity.User;
import com.gameon.model.entity.GameListing;
import com.gameon.model.entity.JoinRequest;
import com.gameon.model.entity.MatchResult;
import com.gameon.model.entity.Post;
import com.gameon.model.enums.NotificationType;
import com.gameon.repository.NotificationRepository;
import com.gameon.repository.PostRepository;
import com.gameon.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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
    private final PostRepository postRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository,
                               PostRepository postRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
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
        return createNotification(recipientId, text, type, actor, listing, joinRequest, matchResult, null);
    }

    @Transactional
    public Notification createNotification(Long recipientId, String text, NotificationType type,
                                           User actor, GameListing listing,
                                           JoinRequest joinRequest, MatchResult matchResult, Post post) {
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("User", recipientId));

        Notification notification = new Notification(recipient, text, type);
        notification.setActor(actor);
        notification.setGameListing(listing);
        notification.setJoinRequest(joinRequest);
        notification.setMatchResult(matchResult);
        notification.setPost(post);
        Notification saved = notificationRepository.save(notification);
        logger.debug("Notification sent to user {}: [{}] {}", recipientId, type, text);
        return saved;
    }

    /** Opens a notification only for its recipient, marks it read, and returns an allowed local route. */
    @Transactional
    public Optional<String> openNotification(Long notificationId, Long userId) {
        return notificationRepository.findOwnedForOpening(notificationId, userId)
                .flatMap(notification -> resolveDestination(notification, userId)
                        .map(destination -> {
                            notification.setIsRead(true);
                            notificationRepository.save(notification);
                            return destination;
                        }));
    }

    @Transactional(readOnly = true)
    public java.util.Set<Long> getClickableNotificationIds(List<Long> notificationIds, Long userId) {
        if (notificationIds == null || notificationIds.isEmpty()) {
            return java.util.Set.of();
        }
        return notificationRepository.findOwnedForOpening(notificationIds, userId).stream()
                .filter(notification -> resolveDestination(notification, userId).isPresent())
                .map(Notification::getNotificationId)
                .collect(java.util.stream.Collectors.toSet());
    }

    private Optional<String> resolveDestination(Notification notification, Long userId) {
        Long listingId = notification.getGameListing() == null
                ? null : notification.getGameListing().getGameListingId();
        boolean creator = listingId != null && notification.getGameListing().getCreator() != null
                && userId.equals(notification.getGameListing().getCreator().getUserId());
        return switch (notification.getNotificationType()) {
            case FOLLOW_NEW -> notification.getActor() == null ? Optional.empty()
                    : Optional.of("/profile/" + notification.getActor().getUserId());
            case POST_COMMENTED -> {
                Post post = notification.getPost();
                yield post != null && post.getPostId() != null
                        && postRepository.existsVisibleToUser(post.getPostId(), userId)
                        ? Optional.of("/social/post/" + post.getPostId()) : Optional.empty();
            }
            case JOIN_REQUEST_RECEIVED, JOIN_WITHDRAWN, ATTENDANCE_CONFIRMED,
                    LAST_CALL_PLACE_FILLED, LISTING_NEEDS_PLAYERS -> listingRoute("/lobby/requests/", listingId);
            case JOIN_ACCEPTED, LAST_CALL_OFFER, LAST_CALL_CLAIMED ->
                    listingId == null ? Optional.empty() : Optional.of("/lobby/joined");
            case LISTING_INVITE -> listingRoute("/listings/", listingId);
            case ATTENDANCE_REMINDER, ATTENDANCE_CONFIRMATION_DEADLINE, LISTING_CONFIRMED ->
                    listingId == null ? Optional.empty()
                            : Optional.of(creator ? "/lobby/created" : "/lobby/joined");
            case CREATOR_CONFIRMATION_URGENT ->
                    listingId == null ? Optional.empty() : Optional.of("/lobby/created");
            case MATCH_RESULT_POSTED, MATCH_RESULT_UPDATED -> notification.getMatchResult() == null
                    ? Optional.empty() : listingRoute("/match-result/view/", listingId);
            case MATCH_RESULT_REMINDER -> listingRoute("/match-result/submit/", listingId);
            default -> Optional.empty();
        };
    }

    private Optional<String> listingRoute(String prefix, Long listingId) {
        return listingId == null ? Optional.empty() : Optional.of(prefix + listingId);
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
    public int markAsRead(Long notificationId, Long userId) {
        return notificationRepository.markAsReadForUser(notificationId, userId);
    }

    @Transactional(readOnly = true)
    public boolean hasNotification(Long recipientId, Long listingId, NotificationType type) {
        return notificationRepository.existsByRecipient_UserIdAndGameListing_GameListingIdAndNotificationType(
                recipientId, listingId, type);
    }

    @Transactional
    public int removeForJoinRequest(Long joinRequestId) {
        return notificationRepository.deleteForJoinRequest(joinRequestId);
    }

    @Transactional
    public int markAllAsRead(Long userId) {
        int count = notificationRepository.markAllAsReadForUser(userId);
        logger.debug("Marked {} notifications as read for user {}", count, userId);
        return count;
    }
}
