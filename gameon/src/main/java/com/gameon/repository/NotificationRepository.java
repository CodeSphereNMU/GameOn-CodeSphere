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

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientUserIdOrderByCreatedAtDesc(Long userId);

    Page<Notification> findByRecipientUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByRecipientUserIdAndIsReadFalse(Long userId);

    List<Notification> findByRecipientUserIdAndNotificationType(Long userId, NotificationType type);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP " +
           "WHERE n.recipient.userId = :userId AND n.isRead = false")
    int markAllAsReadForUser(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP " +
           "WHERE n.notificationId = :notificationId")
    int markAsRead(@Param("notificationId") Long notificationId);
}
