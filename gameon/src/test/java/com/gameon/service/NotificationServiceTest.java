package com.gameon.service;

import com.gameon.model.enums.NotificationType;
import com.gameon.model.entity.User;
import com.gameon.repository.NotificationRepository;
import com.gameon.repository.PostRepository;
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
    @Mock PostRepository postRepository;
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

    @Test
    void openingOwnedAcceptedNotificationMarksItReadAndUsesJoinedLobby() {
        com.gameon.model.entity.GameListing listing = new com.gameon.model.entity.GameListing();
        listing.setGameListingId(99L);
        com.gameon.model.entity.Notification notification = new com.gameon.model.entity.Notification();
        notification.setNotificationType(NotificationType.JOIN_ACCEPTED);
        notification.setGameListing(listing);
        when(notificationRepository.findOwnedForOpening(45L, 12L)).thenReturn(java.util.Optional.of(notification));

        org.assertj.core.api.Assertions.assertThat(service.openNotification(45L, 12L))
                .contains("/lobby/joined");
        org.assertj.core.api.Assertions.assertThat(notification.getIsRead()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    void nonClickableNotificationDoesNotChangeReadState() {
        com.gameon.model.entity.Notification notification = new com.gameon.model.entity.Notification();
        notification.setNotificationType(NotificationType.STRIKE_ISSUED);
        when(notificationRepository.findOwnedForOpening(45L, 12L)).thenReturn(java.util.Optional.of(notification));

        org.assertj.core.api.Assertions.assertThat(service.openNotification(45L, 12L)).isEmpty();
        org.assertj.core.api.Assertions.assertThat(notification.getIsRead()).isFalse();
        verify(notificationRepository, never()).save(notification);
    }

    @Test
    void clickableTypesResolveToTheirApprovedDestinations() {
        java.util.Map<NotificationType, String> destinations = new java.util.LinkedHashMap<>();
        destinations.put(NotificationType.FOLLOW_NEW, "/profile/77");
        destinations.put(NotificationType.POST_COMMENTED, "/social/post/88");
        destinations.put(NotificationType.JOIN_REQUEST_RECEIVED, "/lobby/requests/99");
        destinations.put(NotificationType.JOIN_ACCEPTED, "/lobby/joined");
        destinations.put(NotificationType.JOIN_WITHDRAWN, "/lobby/requests/99");
        destinations.put(NotificationType.LISTING_INVITE, "/listings/99");
        destinations.put(NotificationType.ATTENDANCE_REMINDER, "/lobby/joined");
        destinations.put(NotificationType.ATTENDANCE_CONFIRMED, "/lobby/requests/99");
        destinations.put(NotificationType.ATTENDANCE_CONFIRMATION_DEADLINE, "/lobby/joined");
        destinations.put(NotificationType.CREATOR_CONFIRMATION_URGENT, "/lobby/created");
        destinations.put(NotificationType.LAST_CALL_OFFER, "/lobby/joined");
        destinations.put(NotificationType.LAST_CALL_CLAIMED, "/lobby/joined");
        destinations.put(NotificationType.LAST_CALL_PLACE_FILLED, "/lobby/requests/99");
        destinations.put(NotificationType.LISTING_CONFIRMED, "/lobby/joined");
        destinations.put(NotificationType.MATCH_RESULT_POSTED, "/match-result/view/99");
        destinations.put(NotificationType.MATCH_RESULT_UPDATED, "/match-result/view/99");
        destinations.put(NotificationType.MATCH_RESULT_REMINDER, "/match-result/submit/99");
        destinations.put(NotificationType.LISTING_NEEDS_PLAYERS, "/lobby/requests/99");

        User creator = new User("Creator", "password", com.gameon.model.enums.UserRole.USER);
        creator.setUserId(1L);
        com.gameon.model.entity.GameListing listing = new com.gameon.model.entity.GameListing();
        listing.setGameListingId(99L);
        listing.setCreator(creator);
        com.gameon.model.entity.Post post = new com.gameon.model.entity.Post();
        post.setPostId(88L);
        User actor = new User("Follower", "password", com.gameon.model.enums.UserRole.USER);
        actor.setUserId(77L);

        long id = 100L;
        for (var entry : destinations.entrySet()) {
            com.gameon.model.entity.Notification notification = new com.gameon.model.entity.Notification();
            notification.setNotificationType(entry.getKey());
            if (entry.getKey() == NotificationType.FOLLOW_NEW) notification.setActor(actor);
            if (entry.getKey() == NotificationType.POST_COMMENTED) {
                notification.setPost(post);
                when(postRepository.existsVisibleToUser(88L, 12L)).thenReturn(true);
            }
            if (entry.getKey() != NotificationType.FOLLOW_NEW
                    && entry.getKey() != NotificationType.POST_COMMENTED) notification.setGameListing(listing);
            if (entry.getKey() == NotificationType.MATCH_RESULT_POSTED
                    || entry.getKey() == NotificationType.MATCH_RESULT_UPDATED) {
                notification.setMatchResult(new com.gameon.model.entity.MatchResult());
            }
            when(notificationRepository.findOwnedForOpening(id, 12L))
                    .thenReturn(java.util.Optional.of(notification));

            org.assertj.core.api.Assertions.assertThat(service.openNotification(id, 12L))
                    .as("destination for %s", entry.getKey()).contains(entry.getValue());
            id++;
        }

        // Creator-specific variants open the created lobby.
        listing.setCreator(new User("Recipient", "password", com.gameon.model.enums.UserRole.USER));
        listing.getCreator().setUserId(12L);
        com.gameon.model.entity.Notification creatorReminder = new com.gameon.model.entity.Notification();
        creatorReminder.setNotificationType(NotificationType.ATTENDANCE_REMINDER);
        creatorReminder.setGameListing(listing);
        when(notificationRepository.findOwnedForOpening(id, 12L))
                .thenReturn(java.util.Optional.of(creatorReminder));
        org.assertj.core.api.Assertions.assertThat(service.openNotification(id, 12L))
                .contains("/lobby/created");
    }
}
