package com.gameon.repository;

import com.gameon.model.entity.Notification;
import com.gameon.model.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientUserIdOrderByCreatedAtDesc(Long userId);

    Page<Notification> findByRecipientUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByRecipientUserIdAndIsReadFalse(Long userId);

    @Query("SELECT n FROM Notification n LEFT JOIN FETCH n.gameListing gl LEFT JOIN FETCH gl.creator " +
           "LEFT JOIN FETCH n.actor LEFT JOIN FETCH n.post LEFT JOIN FETCH n.matchResult " +
           "WHERE n.notificationId = :notificationId AND n.recipient.userId = :userId")
    Optional<Notification> findOwnedForOpening(@Param("notificationId") Long notificationId,
                                               @Param("userId") Long userId);

    @Query("SELECT n FROM Notification n LEFT JOIN FETCH n.gameListing gl LEFT JOIN FETCH gl.creator " +
           "LEFT JOIN FETCH n.actor LEFT JOIN FETCH n.post LEFT JOIN FETCH n.matchResult " +
           "WHERE n.notificationId IN :ids AND n.recipient.userId = :userId")
    List<Notification> findOwnedForOpening(@Param("ids") List<Long> ids, @Param("userId") Long userId);

    List<Notification> findByRecipientUserIdAndNotificationType(Long userId, NotificationType type);

    boolean existsByRecipient_UserIdAndGameListing_GameListingIdAndNotificationType(
            Long recipientId, Long listingId, NotificationType type);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP " +
           "WHERE n.recipient.userId = :userId AND n.isRead = false")
    int markAllAsReadForUser(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP " +
           "WHERE n.notificationId = :notificationId AND n.recipient.userId = :userId AND n.isRead = false")
    int markAsReadForUser(@Param("notificationId") Long notificationId, @Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.joinRequest.joinRequestId = :joinRequestId")
    int deleteForJoinRequest(@Param("joinRequestId") Long joinRequestId);

    /**
     * Mutual blocking: remove pending (unread) notifications between two users, where one is the
     * recipient and the other is the actor (in either direction). Read notifications are left
     * intact as history; only pending ones are cleared so no stale cross-notifications remain.
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.isRead = false AND n.actor IS NOT NULL AND (" +
           "(n.recipient.userId = :userId1 AND n.actor.userId = :userId2) OR " +
           "(n.recipient.userId = :userId2 AND n.actor.userId = :userId1))")
    int deleteBetweenUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
}
