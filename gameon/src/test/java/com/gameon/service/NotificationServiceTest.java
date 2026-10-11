package com.gameon.service;

import com.gameon.model.enums.NotificationType;
import com.gameon.repository.NotificationRepository;
import com.gameon.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationRepository notificationRepository;
    @Mock UserRepository userRepository;
    @InjectMocks NotificationService service;

    @Test
    void markAsReadScopesTheUpdateToTheCurrentUser() {
        service.markAsRead(45L, 12L);

        verify(notificationRepository).markAsReadForUser(45L, 12L);
    }

    @Test
    void notificationExistenceIsScopedToRecipientListingAndType() {
        when(notificationRepository.existsByRecipient_UserIdAndGameListing_GameListingIdAndNotificationType(
                12L, 99L, NotificationType.ATTENDANCE_REMINDER)).thenReturn(true);

        org.assertj.core.api.Assertions.assertThat(service.hasNotification(
                12L, 99L, NotificationType.ATTENDANCE_REMINDER)).isTrue();
    }
}
